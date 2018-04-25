package net.ossindex.gradle.output;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.Set;

public class JunitXmlReportWriter {

    private Document doc;
    private DocumentBuilder docBuilder;
    private DocumentBuilderFactory docFactory;
    private Element testSuite;

    public JunitXmlReportWriter() {

        docFactory = DocumentBuilderFactory.newInstance();
        try {
            docBuilder = docFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
            System.exit(1);
        }

        doc = docBuilder.newDocument();
        Element rootElement = doc.createElement("testsuites");
        doc.appendChild(rootElement);
        addElementAttribute(rootElement, "id", "1");
        addElementAttribute(rootElement, "failures", "0");
        addElementAttribute(rootElement, "tests", "0");

        // Top level test suite
        testSuite = addChildElement(rootElement,"testsuite", "");
        addElementAttribute(testSuite, "id", "1");
        addElementAttribute(testSuite, "name", "OSSIndex");
    }

    public void updateJunitReport(String totals, String task, Integer instanceId, String artifact, ArrayList<String> currentVulnerabilityList) {

        // Change to empty string for text, add name tag set to
        Element testCase = addChildElement(testSuite, "testcase", "");
        addElementAttribute(testCase, "name", task + " - " + totals);
        addElementAttribute(testCase, "id", instanceId.toString());

        if (artifact != null) {
            Element failure = addChildElement(testCase, "failure", buildFailureString(currentVulnerabilityList));
            addElementAttribute(failure, "message", artifact);
        }
    }

    public void writeXmlReport(String pathToReport) throws Exception {

        String testCount = getTotalOfElementsByName("testcase").toString();
        modifyElementAttribute("testsuites", 0, "tests", testCount);
        String failureCount = getTotalOfElementsByName("failure").toString();
        modifyElementAttribute("testsuites", 0, "failures", failureCount);

        if(parentDirIsWritable(pathToReport)) {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(new File(pathToReport));

            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

            transformer.transform(source, result);
        } else {
            throw new java.io.IOException("Report (" + pathToReport + ") failed permissions check.");
        }
    }

    private Element addChildElement(Element parent, String name, String data) {
        Element elem = doc.createElement(name);
        elem.appendChild(doc.createTextNode(data));
        parent.appendChild(elem);
        return elem;
    }

    private void addElementAttribute(Element parent, String name, String value) {
        Attr attr = doc.createAttribute(name);
        attr.setValue(value);
        parent.setAttributeNode(attr);
    }

    private  String buildFailureString(ArrayList<String> currentVulnerabilityList) {
        String failureString = "";
        for (String tmp: currentVulnerabilityList){
            failureString = failureString + tmp + "\n";
        }
        return failureString.trim();
    }

    private Integer getTotalOfElementsByName(String name) {
        return doc.getElementsByTagName(name).getLength();
    }

    private void modifyElementAttribute(String tagName, Integer index, String attrName, String value) {
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

