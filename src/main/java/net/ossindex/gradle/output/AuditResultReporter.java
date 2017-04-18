package net.ossindex.gradle.output;

import net.ossindex.gradle.AuditExtensions;
import net.ossindex.gradle.audit.MavenPackageDescriptor;
import net.ossindex.gradle.input.GradleArtifact;
import org.gradle.api.GradleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

public class AuditResultReporter {
    private static final Logger logger = LoggerFactory.getLogger(AuditResultReporter.class);
    private final Set<GradleArtifact> resolvedTopLevelArtifacts;
    private final AuditExtensions settings;
    private Set<GradleArtifact> allGradleArtifacts;

    public AuditResultReporter(Set<GradleArtifact> resolvedTopLevelArtifacts, AuditExtensions settings) {
        this.resolvedTopLevelArtifacts = resolvedTopLevelArtifacts;
        this.settings = settings;
    }

    public void reportResult(Collection<MavenPackageDescriptor> results) {
        int vulnerabilities = getSumOfVulnerabilities(results);
        if (vulnerabilities == 0) return;

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
            GradleArtifact importingGradleArtifact = findImportingArtifactFor(descriptor);
            reportVulnerableArtifact(importingGradleArtifact, descriptor);
            reportIntroducedVulnerabilities(descriptor);
        }
        logger.error(String.format("%s unignored (of %s total) vulnerabilities found", unignoredVulnerabilities, vulnerabilities));

        if (unignoredVulnerabilities == vulnerabilities) {
            throw new GradleException("Too many vulnerabilities (" + vulnerabilities + ") found.");
        }
    }

    private void reportVulnerableArtifact(GradleArtifact importingArtifact, MavenPackageDescriptor descriptor) {
        logger.error(String.format("%s introduces %s which has %s vulnerabilities",
                importingArtifact.getFullDescription(), descriptor.getMavenVersionId(), descriptor.getVulnerabilityMatches()));
    }

    private void reportIntroducedVulnerabilities(MavenPackageDescriptor descriptor) {
        descriptor.getVulnerabilities().forEach(v -> logger.error(String.format("=> %s (see %s)", v.getTitle(), v.getUriString())));
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
