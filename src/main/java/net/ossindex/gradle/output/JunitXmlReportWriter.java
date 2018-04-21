package net.ossindex.gradle.output;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Set;

public class JunitXmlReportWriter {

    private DocumentBuilderFactory docFactory;
    private DocumentBuilder docBuilder;
    private Document doc;
    private Element rootElement;
    public Element getRootElement() {
        return rootElement;
    }

    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss");

    public JunitXmlReportWriter() {

        docFactory = DocumentBuilderFactory.newInstance();
        try {
            docBuilder = docFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
            System.exit(1);
        }

        doc = docBuilder.newDocument();
        rootElement = doc.createElement("testsuites");
        doc.appendChild(rootElement);
        addElementAttribute(rootElement, "disabled", "false");
        addElementAttribute(rootElement, "errors", "0");
        addElementAttribute(rootElement, "failures", "0");
        addElementAttribute(rootElement, "name", "");
        addElementAttribute(rootElement, "tests", "0");
        addElementAttribute(rootElement, "timestamp", formattedTimestamp());
    }

    public void writeXmlReport(String pathToReport) throws Exception {
        if(parentDirIsWritable(pathToReport)) {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(new File(pathToReport));
            transformer.transform(source, result);
        } else {
            throw new java.io.IOException("Report (" + pathToReport + ") failed permissions check.");
        }
    }

    private String formattedTimestamp() {
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        return sdf.format(timestamp);
    }

    public void addElementAttribute(Element parent, String name, String value) {
        Attr attr = doc.createAttribute(name);
        attr.setValue(value);
        parent.setAttributeNode(attr);
    }

    public Element addChildElement(Element parent, String name, String data) {
        Element elem = doc.createElement(name);
        elem.appendChild(doc.createTextNode(data));
        parent.appendChild(elem);
        return elem;
    }

    public Integer getTotalOfElementsByName(String name) {
        return doc.getElementsByTagName(name).getLength();
    }

    public void modifyElementAttribute(String tagName, Integer index, String attrName, String value) {
        Node target = doc.getElementsByTagName(tagName).item(index);
        NamedNodeMap attr = target.getAttributes();
        Node nodeAttr = attr.getNamedItem(attrName);
        nodeAttr.setTextContent(value);
    }

    private Boolean parentDirIsWritable(String pathToReport) throws java.io.IOException {
        File dir = new File(pathToReport);
        String parentDir = dir.getParent();
        if (parentDir != null) {
            Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(Paths.get(parentDir), LinkOption.NOFOLLOW_LINKS);
            return (permissions.contains(PosixFilePermission.OTHERS_WRITE) ||
                    permissions.contains(PosixFilePermission.GROUP_WRITE) ||
                    permissions.contains(PosixFilePermission.OWNER_WRITE)
                    );
        }
        return false;
    }
}

