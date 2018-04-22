package net.ossindex.gradle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;

import groovy.lang.Closure;
import net.ossindex.gradle.audit.MavenPackageDescriptor;
import org.gradle.api.Project;

public class AuditExtensions
{
  private final Project project;

  public boolean failOnError = true;

  public List<String> ignore = new ArrayList<>();

  Collection<AuditExclusion> exclusion = new ArrayList<>();

  public AuditExtensions(Project project) {
    this.project = project;
  }

  public boolean isIgnored(MavenPackageDescriptor descriptor) {
    return isWholeArtifactIgnored(descriptor) || isSpecificVersionIgnored(descriptor);
  }

  private boolean isSpecificVersionIgnored(MavenPackageDescriptor descriptor) {
    return ignore.stream().anyMatch(ignored -> ignored.equals(descriptor.getMavenVersionId()));
  }

  private boolean isWholeArtifactIgnored(MavenPackageDescriptor descriptor) {
    return ignore.stream().anyMatch(ignored -> ignored.equals(descriptor.getMavenPackageId()));
  }

  public AuditExclusion exclusion(Closure closure) {
    AuditExclusion exclusion = (AuditExclusion) project.configure(new AuditExclusion(), closure);
    this.exclusion.add(exclusion);
    return exclusion;
  }

  public Collection<AuditExclusion> getExclusions() {
    return exclusion;
  }
}
