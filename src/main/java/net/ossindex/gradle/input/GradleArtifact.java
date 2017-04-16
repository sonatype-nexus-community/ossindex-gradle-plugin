package net.ossindex.gradle.input;

import org.gradle.api.artifacts.ResolvedDependency;

import java.util.HashSet;
import java.util.Set;

public class GradleArtifact {
    private final GradleArtifact parent;
    private final Set<GradleArtifact> children;
    private final String group;
    private final String name;

    public String getGroup() {
        return group;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public Set<GradleArtifact> getAllArtifacts() {
        Set<GradleArtifact> allGradleArtifacts = new HashSet<>();
        allGradleArtifacts.add(this);
        allGradleArtifacts.addAll(getAllChildren());
        return allGradleArtifacts;
    }

    public Set<GradleArtifact> getAllChildren() {
        Set<GradleArtifact> allChildren = new HashSet<>();
        for (GradleArtifact child : children) {
            allChildren.add(child);
            allChildren.addAll(child.children);
        }
        return allChildren;
    }

    private final String version;

    public GradleArtifact(GradleArtifact parent, ResolvedDependency gradleArtifact) {
        this.parent = parent;
        children = new HashSet<>();
        name = gradleArtifact.getModule().getId().getName();
        group = gradleArtifact.getModule().getId().getGroup();
        version = gradleArtifact.getModule().getId().getVersion();
        addChildren(gradleArtifact);
    }

    public GradleArtifact getTopMostParent() {
        if (parent == null) return this;
        GradleArtifact currentParent = parent;
        while (currentParent.parent != null) {
            currentParent = parent.parent;
        }
        return currentParent;
    }

    public boolean containsPackage(String fullQualifiedName) {
        return getAllArtifacts().stream().anyMatch(c -> c.getFullDescription().equals(fullQualifiedName));
    }

    public String getFullDescription() {
        return String.format("%s:%s:%s", group, name, version);
    }

    private void addChildren(ResolvedDependency gradleArtifact) {
        gradleArtifact.getChildren().forEach(c -> {
                    GradleArtifact child = new GradleArtifact(this, c);
                    children.add(child);
                }
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GradleArtifact gradleArtifact = (GradleArtifact) o;

        return getFullDescription() != null ? getFullDescription().equals(gradleArtifact.getFullDescription()) : gradleArtifact.getFullDescription() == null;
    }

    @Override
    public int hashCode() {
        return getFullDescription() != null ? getFullDescription().hashCode() : 0;
    }

    public Set<GradleArtifact> getChildren() {
        return children;
    }
}

