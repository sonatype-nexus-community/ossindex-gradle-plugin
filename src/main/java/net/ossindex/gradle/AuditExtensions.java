package net.ossindex.gradle;

import net.ossindex.gradle.audit.MavenPackageDescriptor;

import java.util.ArrayList;
import java.util.List;

public class AuditExtensions {
    public boolean failOnError = true;
    public List<String> ignore = new ArrayList<>();

    public boolean isIgnored(MavenPackageDescriptor descriptor) {
        return isWholeArtifactIgnored(descriptor) || isSpecificVersionIgnored(descriptor);
    }

    private boolean isSpecificVersionIgnored(MavenPackageDescriptor descriptor) {
        return ignore.stream().anyMatch(ignored -> ignored.equals(descriptor.getMavenVersionId()));
    }

    private boolean isWholeArtifactIgnored(MavenPackageDescriptor descriptor) {
        return ignore.stream().anyMatch(ignored -> ignored.equals(descriptor.getMavenPackageId()));
    }
}
