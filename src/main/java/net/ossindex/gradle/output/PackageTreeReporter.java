package net.ossindex.gradle.output;

import net.ossindex.gradle.audit.MavenPackageDescriptor;
import net.ossindex.gradle.input.GradleArtifact;
import org.gradle.api.invocation.Gradle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class PackageTreeReporter {

    private static final Logger logger = LoggerFactory.getLogger(PackageTreeReporter.class);

    public void reportDependencyTree(Set<GradleArtifact> topLevelArtifacts, Collection<MavenPackageDescriptor> descriptorsWithVulnerabilities) {
        if (!logger.isInfoEnabled()) return;

        Map<GradleArtifact, List<MavenPackageDescriptor>> artifactsWithVulnerabilities = getArtifactsContainingVulnerabilities(topLevelArtifacts, descriptorsWithVulnerabilities);

        artifactsWithVulnerabilities.forEach(this::printDependencyTree);
    }

    private Map<GradleArtifact, List<MavenPackageDescriptor>> getArtifactsContainingVulnerabilities(Set<GradleArtifact> artifacts, Collection<MavenPackageDescriptor> descriptors) {
        Map<GradleArtifact, List<MavenPackageDescriptor>> map = new HashMap<>();
        for (GradleArtifact artifact : artifacts) {
            for (MavenPackageDescriptor descriptor : descriptors) {
                if (artifact.containsPackage(descriptor.getMavenVersionId())) {
                    if (!map.containsKey(artifact)) {
                        map.put(artifact, new ArrayList<>());
                    }
                    map.get(artifact).add(descriptor);
                }
            }
        }

        return map;
    }

    private void printDependencyTree(GradleArtifact artifact, List<MavenPackageDescriptor> descriptors) {
        StringBuilder builder = new StringBuilder();
        builder.append(artifact.getFullDescription());
        if (hasVulnerabilities(artifact, descriptors)) {
            builder.append("\033[31m <-- vulnerability\033[0m");
        }
        builder.append("\n");

        for (GradleArtifact child : artifact.getChildren()) {
            printChild(child, descriptors, builder, "-");
        }

        logger.info(builder.toString());
    }

    private void printChild(GradleArtifact artifact, List<MavenPackageDescriptor> descriptors, StringBuilder builder, String prefix) {
        builder.append("|");
        builder.append(prefix);
        builder.append(artifact.getFullDescription());
        if (hasVulnerabilities(artifact, descriptors)) {
            builder.append("\033[31m <-- vulnerability\033[0m");
        }
        builder.append("\n");

        for (GradleArtifact child : artifact.getChildren()) {
            printChild(child, descriptors, builder, prefix + "-");
        }
    }

    private boolean hasVulnerabilities(GradleArtifact artifact, List<MavenPackageDescriptor> descriptors) {
        return descriptors.stream().anyMatch(d -> d.getMavenVersionId().equals(artifact.getFullDescription()));
    }
}
