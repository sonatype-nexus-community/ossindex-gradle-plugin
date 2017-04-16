package net.ossindex.gradle;

import net.ossindex.gradle.audit.DependencyAuditor;
import net.ossindex.gradle.input.ArtifactGatherer;
import net.ossindex.gradle.input.GradleArtifact;
import net.ossindex.gradle.output.AuditResultReporter;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Set;

public class OssIndexPlugin implements Plugin<Project> {

    private static final Logger logger = LoggerFactory.getLogger(OssIndexPlugin.class);

    @Override
    public void apply(Project project) {
        Task audit = project.task("audit");
        audit.doLast(this::doAudit);
    }

    private void doAudit(Task task) {
        ArtifactGatherer gatherer = new ArtifactGatherer();
        Set<GradleArtifact> gradleArtifacts = gatherer.gatherResolvedArtifacts(task.getProject());
        DependencyAuditor auditor = new DependencyAuditor(gradleArtifacts);
        AuditResultReporter reporter = new AuditResultReporter(gradleArtifacts);

        logger.info(String.format("Found %s gradleArtifacts to audit", gradleArtifacts.size()));

        try {
            reporter.reportResult(auditor.runAudit());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
