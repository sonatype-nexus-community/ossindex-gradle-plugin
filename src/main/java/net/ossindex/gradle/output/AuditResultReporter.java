package net.ossindex.gradle.output;

import net.ossindex.gradle.audit.MavenPackageDescriptor;
import net.ossindex.gradle.input.GradleArtifact;
import org.gradle.api.GradleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class AuditResultReporter {
    private static final Logger logger = LoggerFactory.getLogger(AuditResultReporter.class);
    private final Set<GradleArtifact> resolvedTopLevelArtifacts;
    private Set<GradleArtifact> allGradleArtifacts;

    public AuditResultReporter(Set<GradleArtifact> resolvedTopLevelArtifacts) {
        this.resolvedTopLevelArtifacts = resolvedTopLevelArtifacts;
    }

    public void reportResult(Collection<MavenPackageDescriptor> results) {
        int vulnerabilities = getSumOfVulnerabilities(results);
        if (vulnerabilities == 0) return;

        allGradleArtifacts = getAllDependencies();

        for (MavenPackageDescriptor descriptor : results) {
            if (descriptor.getVulnerabilities() == null) {
                logger.info("No vulnerabilities in " + descriptor.getMavenVersionId());
                continue;
            }
            GradleArtifact importingGradleArtifact = findImportingArtifactFor(descriptor).orElseThrow(() -> new GradleException("Couldn't find importing artifact for " + descriptor.getMavenVersionId()));
            logger.error(importingGradleArtifact.getFullDescription() + " introduces " + descriptor.getMavenVersionId() + " which has " + descriptor.getVulnerabilityTotal() + " vulnerabilities");
            descriptor.getVulnerabilities().forEach(v -> {
                logger.error("=> " + v.getTitle() + " (see " + v.getUriString() + ")");
            });
        }
        logger.error(vulnerabilities + " vulnerabilities found!");
        throw new GradleException("Too many vulnerabilities (" + vulnerabilities + ") found.");
    }

    private Optional<GradleArtifact> findImportingArtifactFor(MavenPackageDescriptor mavenPackageDescriptor) {
        return allGradleArtifacts
                .stream()
                .filter(a -> a.getFullDescription().equals(mavenPackageDescriptor.getMavenVersionId()))
                .map(GradleArtifact::getTopMostParent)
                .findAny();
    }

    private Set<GradleArtifact> getAllDependencies() {
        return resolvedTopLevelArtifacts.stream().flatMap(a -> a.getAllArtifacts().stream()).collect(Collectors.toSet());
    }

    private int getSumOfVulnerabilities(Collection<MavenPackageDescriptor> results) {
        return results.stream().mapToInt(MavenPackageDescriptor::getVulnerabilityMatches).sum();
    }
}
