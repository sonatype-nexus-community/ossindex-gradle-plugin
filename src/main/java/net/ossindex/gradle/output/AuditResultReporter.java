package net.ossindex.gradle.output;

import net.ossindex.gradle.AuditExtensions;
import net.ossindex.gradle.OssIndexPlugin;
import net.ossindex.gradle.audit.MavenPackageDescriptor;
import net.ossindex.gradle.input.GradleArtifact;
import org.gradle.api.GradleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

public class AuditResultReporter {
    private static final Logger logger = LoggerFactory.getLogger(AuditResultReporter.class);
    private final Set<GradleArtifact> resolvedTopLevelArtifacts;
    private final AuditExtensions settings;
    private Set<GradleArtifact> allGradleArtifacts;
    private Element currentTestSuite = null;
    private String currentVulnerableArtifact = null;
    ArrayList<String> currentVulnerabilityList = new ArrayList<>();
    private String currentVulnerabilityTotals = null;
    private String thisTask = null;

    public JunitXmlReportWriter getJunitXmlReportWriter() {
        return junitXmlReportWriter;
    }

    private JunitXmlReportWriter junitXmlReportWriter;

    public String getJunitReport() {
        return junitReport;
    }

    private String junitReport;

    public AuditResultReporter(Set<GradleArtifact> resolvedTopLevelArtifacts, AuditExtensions settings, OssIndexPlugin ossIndexPlugin) {
        this.resolvedTopLevelArtifacts = resolvedTopLevelArtifacts;
        this.settings = ossIndexPlugin.getSettings();
        this.junitReport = ossIndexPlugin.getJunitReport();
        this.junitXmlReportWriter = ossIndexPlugin.getJunitXmlReportWriter();
        this.thisTask = ossIndexPlugin.thisTask;
    }

    public void reportResult(Collection<MavenPackageDescriptor> results) {
        int vulnerabilities = getSumOfVulnerabilities(results);
        if (vulnerabilities == 0) return;

        int unignoredVulnerabilities = getUnignoredVulnerabilities(results);

        currentTestSuite = addChildElementToReport("testsuite", "");
        junitXmlReportWriter.addElementAttribute(currentTestSuite, "name", thisTask);

        allGradleArtifacts = getAllDependencies();

        for (MavenPackageDescriptor descriptor : results) {
            if (descriptor.getVulnerabilities() == null) {
                logger.info("No vulnerabilities in " + descriptor.getMavenVersionId());
                continue;
            }
            if (settings.isIgnored(descriptor)) {
                logger.info(descriptor.getMavenVersionId() + " is ignored due to settings");
                continue;
            }
            GradleArtifact importingGradleArtifact = findImportingArtifactFor(descriptor);

            // These calls report on the vulnerable artifact and then each vulnerability in it
            reportVulnerableArtifact(importingGradleArtifact, descriptor);
            reportIntroducedVulnerabilities(descriptor);

            // Update the JUnit plugin XML report object
            updateJunitReport();
        }

        // Now reporting on the totals for this vulnerable package
        currentVulnerabilityTotals = String.format("%s unignored (of %s total) vulnerabilities found", unignoredVulnerabilities, vulnerabilities);
        junitXmlReportWriter.addChildElement(currentTestSuite, "totals", currentVulnerabilityTotals);
        logger.error(currentVulnerabilityTotals);



        if (unignoredVulnerabilities > 0) {
            throw new GradleException("Too many vulnerabilities (" + vulnerabilities + ") found.");
        }
    }

    private void updateJunitReport() {
        junitXmlReportWriter.addChildElement(currentTestSuite, "testcase", currentVulnerableArtifact);
        currentVulnerabilityList.forEach(v -> junitXmlReportWriter.addChildElement(currentTestSuite, "failure", v));
    }

    private void reportVulnerableArtifact(GradleArtifact importingArtifact, MavenPackageDescriptor descriptor) {
        currentVulnerableArtifact = String.format("%s introduces %s which has %s vulnerabilities",
                importingArtifact.getFullDescription(), descriptor.getMavenVersionId(), descriptor.getVulnerabilityMatches());
        logger.error(currentVulnerableArtifact);
    }

    private void reportIntroducedVulnerabilities(MavenPackageDescriptor descriptor) {
        currentVulnerabilityList.clear();
        descriptor.getVulnerabilities().forEach(v -> reportVulnerability(String.format("=> %s (see %s)", v.getTitle(), v.getUriString())));
    }

    private void reportVulnerability(String line) {
        logger.error(line);
        currentVulnerabilityList.add(line);
    }

    private Element addChildElementToReport(String name, String data) {
        return junitXmlReportWriter.addChildElement(junitXmlReportWriter.getRootElement(), name, data);
    }

    private GradleArtifact findImportingArtifactFor(MavenPackageDescriptor mavenPackageDescriptor) {
        return allGradleArtifacts
                .stream()
                .filter(a -> a.getFullDescription().equals(mavenPackageDescriptor.getMavenVersionId()))
                .map(GradleArtifact::getTopMostParent)
                .findAny()
                .orElseThrow(() -> new GradleException("Couldn't find importing artifact for " + mavenPackageDescriptor.getMavenVersionId()));
    }

    private Set<GradleArtifact> getAllDependencies() {
        return resolvedTopLevelArtifacts.stream().flatMap(a -> a.getAllArtifacts().stream()).collect(Collectors.toSet());
    }

    private int getSumOfVulnerabilities(Collection<MavenPackageDescriptor> results) {
        return results.stream().mapToInt(MavenPackageDescriptor::getVulnerabilityMatches).sum();
    }

    private int getUnignoredVulnerabilities(Collection<MavenPackageDescriptor> results) {
        return results.stream().filter(d -> !settings.isIgnored(d)).mapToInt(MavenPackageDescriptor::getVulnerabilityMatches).sum();
    }
}
