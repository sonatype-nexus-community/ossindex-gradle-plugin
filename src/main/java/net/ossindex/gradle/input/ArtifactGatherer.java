package net.ossindex.gradle.input;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;

import java.util.Set;
import java.util.stream.Collectors;

public class ArtifactGatherer {
    public Set<GradleArtifact> gatherResolvedArtifacts(Project project) {
        return project
                .getConfigurations()
                .stream()
                .filter(Configuration::isCanBeResolved)
                .flatMap(c -> c.getResolvedConfiguration().getFirstLevelModuleDependencies().stream())
                .map(it -> new GradleArtifact(null, it))
                .collect(Collectors.toSet());
    }
}
