package net.ossindex.gradle;

import net.ossindex.gradle.audit.AuditorFactory;
import net.ossindex.gradle.audit.DependencyAuditor;
import net.ossindex.gradle.audit.MavenPackageDescriptor;
import net.ossindex.gradle.audit.Proxy;
import net.ossindex.gradle.input.ArtifactGatherer;
import net.ossindex.gradle.input.GradleArtifact;
import net.ossindex.gradle.output.AuditResultReporter;
import net.ossindex.gradle.output.JunitXmlReportWriter;
import net.ossindex.gradle.output.PackageTreeReporter;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class OssIndexPlugin implements Plugin<Project> {

    private JunitXmlReportWriter junitXmlReportWriter = new JunitXmlReportWriter();
    public static String junitReport = null;

    private static AuditExtensions settings = null;
    private static final Logger logger = LoggerFactory.getLogger(OssIndexPlugin.class);
    private List<Proxy> proxies = new LinkedList<>();
    private Project project = null;
    private AuditorFactory factory = new AuditorFactory();
    public void setAuditorFactory(AuditorFactory factory) {
        this.factory = factory;
    }

    @Override
    public void apply(Project project) {
        this.project = project;

        project.getExtensions().create("audit", AuditExtensions.class, project);
        Task audit = project.task("audit");
        Proxy proxy = getProxy(project, "http");

        if (proxy != null) {
            proxies.add(proxy);
        }proxy = getProxy(project, "https");
        if (proxy != null) {
            proxies.add(proxy);
        }
        audit.doLast(this::doAudit);
    }

    private Proxy getProxy(Project project, String scheme) {
        Proxy proxy = new Proxy();
        proxy.setHost((String)project.findProperty("systemProp." + scheme + ".proxyHost"));
        Object port = project.findProperty("systemProp." + scheme + ".proxyPort");
        proxy.setPort(port == null ? null : Integer.parseInt((String)port));
        proxy.setUser((String)project.findProperty("systemProp." + scheme + ".proxyUser"));
        proxy.setPassword((String)project.findProperty("systemProp." + scheme + ".proxyPassword"));
        proxy.setNonProxyHosts((String)project.findProperty("systemProp." + scheme + ".nonProxyHosts"));
        if (proxy.isValid()) {
            return proxy;
        } else {
            return null;
        }
    }

    private void doAudit(Task task) {
        if (this.settings == null) {
            this.settings = getAuditExtensions(task.getProject());
        }
        // Mocked tests may not have settings
        junitReport = settings != null ? settings.junitReport : null;
        if (settings != null) {
            junitXmlReportWriter.init(junitReport);
        }

        ArtifactGatherer gatherer = factory.getGatherer();
        Set<GradleArtifact> gradleArtifacts = gatherer != null ? gatherer.gatherResolvedArtifacts(task.getProject()) : null;

        AuditExtensions auditConfig = getAuditExtensions(task.getProject());
        DependencyAuditor auditor = factory.getDependencyAuditor(auditConfig, gradleArtifacts, proxies);

        String tmpTask = project.getDisplayName().split(" ")[1].replaceAll("\'","") + ":audit";
        AuditResultReporter reporter = new AuditResultReporter(gradleArtifacts,
            getAuditExtensions(task.getProject()),
            junitXmlReportWriter,
            project.getDisplayName().split(" ")[1].replaceAll("\'","") + ":audit");

        logger.info(String.format("Found %s gradleArtifacts to audit", gradleArtifacts.size()));

        Collection<MavenPackageDescriptor> packagesWithVulnerabilities = auditor.runAudit();

        try {
            reporter.reportResult(packagesWithVulnerabilities);
        } catch (GradleException e) {
            if (shouldFailOnError(task.getProject())) {
                throw e;
            }
        } finally {
            PackageTreeReporter treeReporter = new PackageTreeReporter(auditConfig);
            treeReporter.reportDependencyTree(gradleArtifacts, packagesWithVulnerabilities);
            if (junitReport != null) {
                try {
                    junitXmlReportWriter.writeXmlReport(junitReport);
                    junitXmlReportWriter = null;
                } catch (Exception e) {
                    System.out.println("Failed to create JUnit Plugin report:  " + e.getMessage());
                }
            }
        }
    }

    private boolean shouldFailOnError(Project project) {
        return getAuditExtensions(project).failOnError;
    }

    private AuditExtensions getAuditExtensions(Project project) {
        return (AuditExtensions) project.getExtensions().getByName("audit");
    }
}
