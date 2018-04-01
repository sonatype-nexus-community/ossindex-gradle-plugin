package net.ossindex.gradle.audit;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.ossindex.common.IPackageRequest;
import net.ossindex.common.OssIndexApi;
import net.ossindex.common.PackageDescriptor;
import net.ossindex.gradle.input.GradleArtifact;
import org.gradle.api.GradleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DependencyAuditor
{
  private static final Logger logger = LoggerFactory.getLogger(DependencyAuditor.class);

  private Map<PackageDescriptor, PackageDescriptor> parents = new HashMap<>();

  private IPackageRequest request = OssIndexApi.createPackageRequest();

  public DependencyAuditor(Set<GradleArtifact> gradleArtifacts, final List<Proxy> proxies) {
    for (Proxy proxy : proxies) {
      logger.error("Using proxy: " + proxy);
      request.addProxy(proxy.getScheme(), proxy.getHost(), proxy.getPort(), proxy.getUser(), proxy.getPassword());
    }
    addArtifactsToAudit(gradleArtifacts);
  }

  public Collection<MavenPackageDescriptor> runAudit() {
    try {
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
        if (mvnPkg.getVulnerabilityMatches() > 0) {
          results.add(mvnPkg);
        }
      }
      return results;
    }
    catch (IOException e) {
      throw new GradleException("Error trying to get audit results", e);
    }
  }

  private void addArtifactsToAudit(Set<GradleArtifact> gradleArtifacts) {
    gradleArtifacts.forEach(this::addArtifact);
  }

  private void addPackageDependencies(PackageDescriptor parent, GradleArtifact gradleArtifact) {
    PackageDescriptor pkgDep = new PackageDescriptor("maven", gradleArtifact.getGroup(), gradleArtifact.getName(),
        gradleArtifact.getVersion());
    if (!parents.containsKey(pkgDep)) {
      pkgDep = request.add("maven", gradleArtifact.getGroup(), gradleArtifact.getName(), gradleArtifact.getVersion());
      parents.put(pkgDep, parent);
    }
  }

  private void addArtifact(GradleArtifact gradleArtifact) {
    PackageDescriptor parent = request
        .add("maven", gradleArtifact.getGroup(), gradleArtifact.getName(), gradleArtifact.getVersion());
    parents.put(parent, null);
    gradleArtifact.getAllChildren().forEach(c -> addPackageDependencies(parent, c));
  }

}
