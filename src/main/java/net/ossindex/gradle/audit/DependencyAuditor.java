package net.ossindex.gradle.audit;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Strings;
import net.ossindex.common.IPackageRequest;
import net.ossindex.common.OssIndexApi;
import net.ossindex.common.OssiPackage;
import net.ossindex.common.PackageCoordinate;
import net.ossindex.common.filter.IVulnerabilityFilter;
import net.ossindex.common.filter.VulnerabilityFilterFactory;
import net.ossindex.gradle.AuditExclusion;
import net.ossindex.gradle.AuditExtensions;
import net.ossindex.gradle.input.GradleArtifact;
import org.gradle.api.GradleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DependencyAuditor
{
  private static final Logger logger = LoggerFactory.getLogger(DependencyAuditor.class);

  private final AuditExtensions config;

  private Map<OssiPackage, OssiPackage> parents = new HashMap<>();

  private IPackageRequest request;

  public DependencyAuditor(final AuditExtensions auditConfig,
                           Set<GradleArtifact> gradleArtifacts,
                           final List<Proxy> proxies)
  {
    this.config = auditConfig;
    switch(proxies.size()) {
      case 0:
        logger.info("Direct OSSI connection (" + proxies.size() + " proxies configured)");
        break;
      default:
        logger.info("Using proxy (" + proxies.size() + ")");
        for (Proxy proxy : proxies) {
          logger.info("  * " + proxy);
          OssIndexApi.addProxy(proxy.getScheme(), proxy.getHost(), proxy.getPort(), proxy.getUser(), proxy.getPassword());
        }
        break;
    }

    request = OssIndexApi.createPackageRequest();
    configure();
    addArtifactsToAudit(gradleArtifacts);
  }

  private void configure() {
    if (config != null) {
      // Filter configuration
      IVulnerabilityFilter filter = VulnerabilityFilterFactory.getInstance().createVulnerabilityFilter();
      Collection<AuditExclusion> exclusions = config.getExclusions();
      for (AuditExclusion exclusion : exclusions) {
        exclusion.apply(filter);
      }
      request.addVulnerabilityFilter(filter);

      // Cache configuration
      if (!Strings.isNullOrEmpty(config.cache)) {
        File file = new File(config.cache);
        if (file.exists()) {
          if (!file.isFile()) {
            throw new GradleException("cache option must specify a file (" + config.cache + ")");
          }
          if (!file.canWrite()) {
            throw new GradleException("cannot write to the specified cache file (" + config.cache + ")");
          }
        }
        File parentDir = file.getParentFile();
        if (parentDir.exists()) {
          if (!parentDir.canWrite()) {
            throw new GradleException("cannot write to cache dir (" + config.cache + ")");
          }
          if (!parentDir.canExecute()) {
            throw new GradleException(
                "cannot access cache dir, need execute permissions on dir (" + config.cache + ")");
          }
        }
        else {
          if (!parentDir.mkdirs()) {
            throw new GradleException("cannot create dir for cache (" + config.cache + ")");
          }
        }
        request.setCacheFile(file.getAbsolutePath());
      }

      // Credentials configuration
      if (!Strings.isNullOrEmpty(config.user) && !Strings.isNullOrEmpty(config.token)) {
        request.setCredentials(config.user, config.token);
      }

      if (config.packagesPerRequest != null) {
        request.setMaximumPackagesPerRequest(config.packagesPerRequest);
      }

      if (config.cacheTimeout != null) {
        request.setCacheTimeout(config.cacheTimeout);
      }
    }
  }

  public Collection<MavenPackageDescriptor> runAudit() {
    try {
      List<MavenPackageDescriptor> results = new LinkedList<>();
      Collection<OssiPackage> packages = request.run();
      for (OssiPackage pkg : packages) {
        MavenPackageDescriptor mvnPkg = new MavenPackageDescriptor(pkg);
        if (parents.containsKey(pkg)) {
          OssiPackage parent = parents.get(pkg);
          if (parent != null) {
            mvnPkg.setParent(new MavenIdWrapper(parent));
          }
        }
        if (mvnPkg.getUnfilteredVulnerabilityCount() > 0) {
          results.add(mvnPkg);
        }
      }
      return results;
    }
    catch (IOException e) {
      if (e.getMessage().contains("(429)")) {
        // Non-registered users should be told about registering
        if(Strings.isNullOrEmpty(config.user)) {
          throw new GradleException("Too many requests (429): Use OSS Index credentials for increased rate limit.", e);
        }
        // Registered users can choose whether this is an error or not
        else {
          // If the user has decided that 429 is a warning
          if (Boolean.FALSE.equals(config.rateLimitAsError)) {
            logger.info("Too many requests (429) trying to get audit results. Current results have been cached,");
            logger.info("wait 60+ minutes then run again to audit more packages. If you run builds once a day");
            logger.info("and always see this message, you may want to run the build more often or increase the");
            logger.info("cache timeout to 48 or more hours.");
            return Collections.emptyList();
          }
          // Otherwise 429 is an error
          else {
            throw new GradleException("Error trying to get audit results: " + e.getMessage(), e);
          }
        }
      }
      // Unknown exceptions are errors
      else {
        throw new GradleException("Error trying to get audit results: " + e.getMessage(), e);
      }
    }
  }

  private void addArtifactsToAudit(Set<GradleArtifact> gradleArtifacts) {
    gradleArtifacts.forEach(this::addArtifact);
  }

  private void addArtifact(GradleArtifact gradleArtifact) {
    PackageCoordinate parentCoordinate = buildCoordinate(gradleArtifact);
    OssiPackage parent = request.add(Collections.singletonList(parentCoordinate));
    parents.put(parent, null);
    gradleArtifact.getAllChildren().forEach(c -> addPackageDependencies(parent, parentCoordinate, c));
  }

  private void addPackageDependencies(OssiPackage parent,
                                      PackageCoordinate parentCoordinate,
                                      GradleArtifact gradleArtifact)
  {
    OssiPackage pkgDep = new OssiPackage("maven", gradleArtifact.getGroup(), gradleArtifact.getName(),
        gradleArtifact.getVersion());
    if (!parents.containsKey(pkgDep)) {
      PackageCoordinate childCoordinate = buildCoordinate(gradleArtifact);
      pkgDep = request.add(Arrays.asList(new PackageCoordinate[]{parentCoordinate, childCoordinate}));
      parents.put(pkgDep, parent);
    }
  }

  private PackageCoordinate buildCoordinate(final GradleArtifact gradleArtifact) {
    return PackageCoordinate.newBuilder()
        .withFormat("maven")
        .withNamespace(gradleArtifact.getGroup())
        .withName(gradleArtifact.getName())
        .withVersion(gradleArtifact.getVersion())
        .build();
  }

  private String toString(PackageCoordinate pkg) {
    return pkg.getNamespace() + ":" + pkg.getNamespace() + ":" + pkg.getName() + ":" + pkg.getVersion();
  }
}
