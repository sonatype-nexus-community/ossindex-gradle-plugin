package net.ossindex.gradle.output;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

public class JunitXmlReportWriter
{
  private static final Logger logger = LoggerFactory.getLogger(JunitXmlReportWriter.class);

  public static String pathToReport;

  private boolean skip = false;

  private Element testSuite;

  private Long startSeconds = null;

  /**
   * The DocResource class does a file lock preventing concurrent access from other processes, but we still need
   * to prevent concurrent access of the document in *THIS* class.
   */
  private static Lock lock = new ReentrantLock();

  public void init(String junitReport) {

    if (junitReport == null) {
      skip = true;
      return;
    }

    this.pathToReport = junitReport;
    // Metrics
    setStartTime();
  }

  public void setStartTime() {
    startSeconds = java.time.Instant.now().getEpochSecond();
  }

  public void updateJunitReport(String totals,
                                String task,
                                String artifact,
                                ArrayList<String> currentVulnerabilityList)
  {

    if (skip) {
      return;
    }

    lock.lock();
    try (DocResource docResource = new DocResource(pathToReport)) {
      Document doc = docResource.getDocument();

      // Get a new testcase ID
      Integer testCaseId = getTotalOfElementsByName(doc, "testcase");
      testCaseId++;

      // Change to empty string for text, add name tag set to
      Element testCase = addChildElement(doc, testSuite, "testcase", "");
      addElementAttribute(doc, testCase, "name", task + " - " + totals);
      addElementAttribute(doc, testCase, "id", testCaseId.toString());
      Long elapsedTime = java.time.Instant.now().getEpochSecond() - startSeconds;
      addElementAttribute(doc, testCase, "time", elapsedTime.toString());

      if (artifact != null) {
        if (!artifact.substring(0, 1).equals("0")) {
          Element failure = addChildElement(doc, testCase, "failure", buildFailureString(currentVulnerabilityList));
          addElementAttribute(doc, failure, "message", artifact);
        }
      }
    }
    catch (IOException e) {
      logger.error("Exception writing log: " + e);
    }
    finally {
      lock.unlock();
    }
  }

  public void writeXmlReport(String pathToReport) throws Exception {
    if (skip) {
      return;
    }

    lock.lock();
    try (DocResource docResource = new DocResource(pathToReport)) {
      Document doc = docResource.getDocument();
      String testCount = getTotalOfElementsByName(doc, "testcase").toString();
      modifyElementAttribute(doc, "testsuites", 0, "tests", testCount);
      String failureCount = getTotalOfElementsByName(doc, "failure").toString();
      modifyElementAttribute(doc, "testsuites", 0, "failures", failureCount);
    }
    finally {
      lock.unlock();
    }
  }

  private Element addChildElement(Document doc, Element parent, String name, String data) {
    Element elem = doc.createElement(name);
    elem.appendChild(doc.createTextNode(data));
    parent.appendChild(elem);
    return elem;
  }

  private void addElementAttribute(Document doc, Element parent, String name, String value) {
    Attr attr = doc.createAttribute(name);
    attr.setValue(value);
    parent.setAttributeNode(attr);
  }

  private String buildFailureString(ArrayList<String> currentVulnerabilityList) {
    String failureString = "";
    for (String tmp : currentVulnerabilityList) {
      failureString = failureString + tmp + "\n";
    }
    return failureString.trim();
  }

  private Integer getTotalOfElementsByName(Document doc, String name) {
    return doc.getElementsByTagName(name).getLength();
  }

  private void modifyElementAttribute(Document doc, String tagName, Integer index, String attrName, String value) {
    Node target = doc.getElementsByTagName(tagName).item(index);
    NamedNodeMap attr = target.getAttributes();
    Node nodeAttr = attr.getNamedItem(attrName);
    nodeAttr.setTextContent(value);
  }

  /**
   * Manage the lock and writing of the report document.
   *
   * Every document edit is performed by:
   *
   * 1. Locking the file
   * 2. Creating/Loading the document
   * 3. Performing the document changes
   * 4. Writing the file
   * 5. Unlocking the file
   *
   * This is done to ensure that parallel builds can all work on the same report file.
   */
  class DocResource
      implements AutoCloseable
  {

    private String path;

    private Document doc;

    private FileLock fileLock;

    public DocResource(final String path) {
      File f = new File(path);
      this.path = f.getAbsolutePath();

      try {
        RandomAccessFile randomAccessFile = new RandomAccessFile(path, "rw");
        FileChannel fc = randomAccessFile.getChannel();

        fileLock = fc.lock();
        if (fc.size() == 0) {
          // If this is a new file, then initialize it.
          doc = createDocument();
        }
        else {
          doc = loadDocument();
        }
      }
      catch (IOException e) {
        logger.error("Exception writing log", e);
      }

    }

    public Document getDocument() {
      return doc;
    }

    @Override
    public void close() throws IOException {
      writeDocument(doc);
      fileLock.close();
    }

    /**
     * Called only if the file does not exist. Creates the basic document structure and writes to the file.
     */
    private Document createDocument() throws IOException {
      File f = new File(pathToReport);
      Document doc = getDocBuilder().newDocument();
      Element rootElement = doc.createElement("testsuites");
      doc.appendChild(rootElement);
      addElementAttribute(doc, rootElement, "id", "1");
      addElementAttribute(doc, rootElement, "failures", "0");
      addElementAttribute(doc, rootElement, "tests", "0");
      // Top level test suite
      testSuite = addChildElement(doc, rootElement, "testsuite", "");
      addElementAttribute(doc, testSuite, "id", "1");
      addElementAttribute(doc, testSuite, "name", "OSSIndex");

      writeDocument(doc);
      return doc;
    }

    /**
     * Load the existing report file.
     */
    private Document loadDocument() {
      Document doc = null;
      try {
        doc = getDocBuilder().parse(pathToReport);
      }
      catch (SAXException | IOException e) {
        e.printStackTrace();
        System.exit(1);
      }
      testSuite = (Element) doc.getElementsByTagName("testsuite").item(0);
      return doc;
    }

    private DocumentBuilder getDocBuilder() {
      DocumentBuilder docBuilder = null;
      DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
      try {
        docBuilder = docFactory.newDocumentBuilder();
      }
      catch (ParserConfigurationException e) {
        e.printStackTrace();
        System.exit(1);
      }
      return docBuilder;
    }

    /**
     * Write the report file
     */
    private void writeDocument(Document doc) throws IOException {
      if (!parentDirIsWritable(new File(path))) {
        throw new IOException(("Report directory is not writable: " + path));
      }
      try {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(pathToReport);

        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

        transformer.transform(source, result);
      }
      catch (TransformerConfigurationException e) {
        throw new IOException(e);
      }
      catch (TransformerException e) {
        throw new IOException(e);
      }
    }
  }

  private Boolean parentDirIsWritable(File path) throws IOException {
    File parentDir = path.getParentFile();
    if (!parentDir.exists()) {
      parentDir.mkdirs();
    }
    if (parentDir.exists()) {
      Set<PosixFilePermission> permissions = Files
          .getPosixFilePermissions(Paths.get(parentDir.getAbsolutePath()), LinkOption.NOFOLLOW_LINKS);
      return (permissions.contains(PosixFilePermission.OTHERS_WRITE) ||
          permissions.contains(PosixFilePermission.GROUP_WRITE) ||
          permissions.contains(PosixFilePermission.OWNER_WRITE)
      );
    }
    return false;
  }
}

