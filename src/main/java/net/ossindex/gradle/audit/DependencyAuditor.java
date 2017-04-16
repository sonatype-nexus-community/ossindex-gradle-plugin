package net.ossindex.gradle.audit;

import net.ossindex.common.IPackageRequest;
import net.ossindex.common.OssIndexApi;
import net.ossindex.common.PackageDescriptor;
import net.ossindex.gradle.input.GradleArtifact;

import java.io.IOException;
import java.util.*;

public class DependencyAuditor {
    private Map<PackageDescriptor, PackageDescriptor> parents = new HashMap<>();
    private IPackageRequest request = OssIndexApi.createPackageRequest();

    public DependencyAuditor(Set<GradleArtifact> gradleArtifacts) {
        addArtifactsToAudit(gradleArtifacts);
    }

    public Collection<MavenPackageDescriptor> runAudit() throws IOException {
        List<MavenPackageDescriptor> results = new LinkedList<>();
        Collection<PackageDescriptor> packages = request.run();
        for (PackageDescriptor pkg : packages) {
            MavenPackageDescriptor mvnPkg = new MavenPackageDescriptor(pkg);
            if (parents.containsKey(pkg)) {
                PackageDescriptor parent = parents.get(pkg);
                if (parent != null) {
                    mvnPkg.setParent(new MavenIdWrapper(parent));
                }
            }
            results.add(mvnPkg);
        }
        return results;
    }

    private void addArtifactsToAudit(Set<GradleArtifact> gradleArtifacts) {
        gradleArtifacts.forEach(this::addArtifact);
    }

    private void addPackageDependencies(PackageDescriptor parent, GradleArtifact gradleArtifact) {
        PackageDescriptor pkgDep = new PackageDescriptor("maven", gradleArtifact.getGroup(), gradleArtifact.getName(), gradleArtifact.getVersion());
        if (!parents.containsKey(pkgDep)) {
            pkgDep = request.add("maven", gradleArtifact.getGroup(), gradleArtifact.getName(), gradleArtifact.getVersion());
            parents.put(pkgDep, parent);
        }
    }

    private void addArtifact(GradleArtifact gradleArtifact) {
        PackageDescriptor parent = request.add("maven", gradleArtifact.getGroup(), gradleArtifact.getName(), gradleArtifact.getVersion());
        parents.put(parent, null);
        gradleArtifact.getAllChildren().forEach(c -> addPackageDependencies(parent, c));
    }

}
