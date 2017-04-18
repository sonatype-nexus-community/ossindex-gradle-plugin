package net.ossindex.gradle;

import net.ossindex.gradle.audit.DependencyAuditor;
import net.ossindex.gradle.audit.MavenPackageDescriptor;
import net.ossindex.gradle.input.ArtifactGatherer;
import net.ossindex.gradle.input.GradleArtifact;
import net.ossindex.gradle.output.AuditResultReporter;
import net.ossindex.gradle.output.PackageTreeReporter;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Set;

public class OssIndexPlugin implements Plugin<Project> {

    private static final Logger logger = LoggerFactory.getLogger(OssIndexPlugin.class);

    @Override
    public void apply(Project project) {
        project.getExtensions().create("audit", AuditExtensions.class);
        Task audit = project.task("audit");
        audit.doLast(this::doAudit);
    }

    private void doAudit(Task task) {
        ArtifactGatherer gatherer = new ArtifactGatherer();
        Set<GradleArtifact> gradleArtifacts = gatherer.gatherResolvedArtifacts(task.getProject());
        DependencyAuditor auditor = new DependencyAuditor(gradleArtifacts);

        AuditResultReporter reporter = new AuditResultReporter(gradleArtifacts, getAuditExtensions(task.getProject()));

        logger.info(String.format("Found %s gradleArtifacts to audit", gradleArtifacts.size()));

        Collection<MavenPackageDescriptor> packagesWithVulnerabilities = auditor.runAudit();

        try {
            reporter.reportResult(packagesWithVulnerabilities);
        } catch (GradleException e) {
            if (shouldFailOnError(task.getProject())) {
                throw e;
            }
        } finally {
            PackageTreeReporter treeReporter = new PackageTreeReporter(getAuditExtensions(task.getProject()));
            treeReporter.reportDependencyTree(gradleArtifacts, packagesWithVulnerabilities);
        }

    }

    private boolean shouldFailOnError(Project project) {
        return getAuditExtensions(project).failOnError;
    }

    private AuditExtensions getAuditExtensions(Project project) {
        return (AuditExtensions) project.getExtensions().getByName("audit");
    }
}
