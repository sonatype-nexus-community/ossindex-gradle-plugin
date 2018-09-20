package net.ossindex.gradle.output;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import net.ossindex.common.OssiVulnerability;
import net.ossindex.gradle.AuditExtensions;
import net.ossindex.gradle.audit.MavenPackageDescriptor;
import net.ossindex.gradle.input.GradleArtifact;
import org.gradle.api.GradleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuditResultReporter
{
    private static final Logger logger = LoggerFactory.getLogger(AuditResultReporter.class);

    private static final String OSSI_VULN_PREFIX = "https://ossindex.sonatype.org/vuln/";

    private final Set<GradleArtifact> resolvedTopLevelArtifacts;

    private final AuditExtensions settings;

    private Set<GradleArtifact> allGradleArtifacts;

    private String currentVulnerableArtifact = null;

    ArrayList<String> currentVulnerabilityList = new ArrayList<>();

    private String currentVulnerabilityTotals = null;

    private String thisTask;

    private JunitXmlReportWriter junitXmlReportWriter;

    public AuditResultReporter(Set<GradleArtifact> resolvedTopLevelArtifacts,
                                                         AuditExtensions settings,
                                                         JunitXmlReportWriter junitXmlReportWriter,
                                                         String thisTask)
    {
        this.resolvedTopLevelArtifacts = resolvedTopLevelArtifacts;
        this.settings = settings;
        this.junitXmlReportWriter = junitXmlReportWriter;
        this.thisTask = thisTask;
    }

    public void reportResult(Collection<MavenPackageDescriptor> results) {
        int vulnerabilities = getSumOfVulnerabilities(results);
        if (vulnerabilities == 0) {
            return;
        }

        int unignoredVulnerabilities = getUnignoredVulnerabilities(results);

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

            // We already calculated unignored vulnerabilities. We need to include unexcluded vulnerabilities since they
            // are handled by the audit library.
            int actualVulnerabilities = descriptor.getVulnerabilities().size();
            int expectedVulnerabilities = descriptor.getVulnerabilityMatches();
            int unExcludedVulnerabilities = expectedVulnerabilities - actualVulnerabilities;
            unignoredVulnerabilities -= unExcludedVulnerabilities;

            // Now bail if exclusions cause all issues in this package to be ignored
            if (actualVulnerabilities == 0) {
                logger.info("Vulnerabilities in " + descriptor.getMavenVersionId() + " are excluded due to settings");
                continue;
            }

            GradleArtifact importingGradleArtifact = null;
            try {
                importingGradleArtifact = findImportingArtifactFor(descriptor);
            }
            catch (GradleException ignore) {
                // This seems to mean that this is a top level artifact
            }
            reportVulnerableArtifact(importingGradleArtifact, descriptor);
            reportIntroducedVulnerabilities(descriptor);
        }

        currentVulnerabilityTotals = String.format("%s unignored (of %s total) vulnerabilities found",
                unignoredVulnerabilities,
                vulnerabilities);
        logger.error(currentVulnerabilityTotals);

        // Update the JUnit plugin XML report object
        junitXmlReportWriter.updateJunitReport(currentVulnerabilityTotals,
                thisTask,
                currentVulnerableArtifact,
                currentVulnerabilityList);

        if (unignoredVulnerabilities > 0) {
            throw new GradleException("Too many vulnerabilities (" + vulnerabilities + ") found.");
        }
    }

    private void reportVulnerableArtifact(GradleArtifact importingArtifact, MavenPackageDescriptor descriptor) {
        currentVulnerableArtifact = importingArtifact == null ?
            String.format("%s introduces %s which has %s vulnerabilities",
                descriptor.getMavenVersionId(),
                descriptor.getMavenVersionId(),
                descriptor.getVulnerabilityMatches()) :
            String.format("%s introduces %s which has %s vulnerabilities",
                importingArtifact.getFullDescription(),
                descriptor.getMavenVersionId(),
                descriptor.getVulnerabilityMatches());
        logger.error(currentVulnerableArtifact);
    }

    private int reportIntroducedVulnerabilities(MavenPackageDescriptor descriptor) {
        currentVulnerabilityList.clear();
        List<OssiVulnerability> vulns = descriptor.getVulnerabilities();
        vulns.forEach(v -> reportVulnerability(String.format("=> %s (see %s)", v.getTitle(), getUriString(v))));
        return vulns.size();
    }

    private String getUriString(final OssiVulnerability v) {
        return OSSI_VULN_PREFIX + v.getId();
    }

    private void reportVulnerability(String line) {
        logger.error(line);
        currentVulnerabilityList.add(line);
    }

    private GradleArtifact findImportingArtifactFor(MavenPackageDescriptor mavenPackageDescriptor) {
        return allGradleArtifacts
                .stream()
                .filter(a -> a.getFullDescription().equals(mavenPackageDescriptor.getMavenVersionId()))
                .map(GradleArtifact::getTopMostParent)
                .findAny()
                .orElseThrow(() -> new GradleException(
                        "Couldn't find importing artifact for " + mavenPackageDescriptor.getMavenVersionId()));
    }

    private Set<GradleArtifact> getAllDependencies() {
        return resolvedTopLevelArtifacts.stream().flatMap(a -> a.getAllArtifacts().stream()).collect(Collectors.toSet());
    }

    private int getSumOfVulnerabilities(Collection<MavenPackageDescriptor> results) {
        return results.stream().mapToInt(MavenPackageDescriptor::getVulnerabilityMatches).sum();
    }

    private int getUnignoredVulnerabilities(Collection<MavenPackageDescriptor> results) {
        return results.stream().filter(d -> !settings.isIgnored(d))
                .mapToInt(MavenPackageDescriptor::getVulnerabilityMatches).sum();
    }
}
