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

    public static JunitXmlReportWriter junitXmlReportWriter = null;

    public static String junitReport = null;
    public static Integer instanceId;

    private static AuditExtensions settings = null;

    private static final Logger logger = LoggerFactory.getLogger(OssIndexPlugin.class);
    private List<Proxy> proxies = new LinkedList<>();

    private AuditorFactory factory = new AuditorFactory();
    private AuditResultReporter reporter = null;

    public void setAuditorFactory(AuditorFactory factory) {
        this.factory = factory;
    }
    private Project project = null;

    public OssIndexPlugin() {
        if (this.junitXmlReportWriter == null) {
            this.junitXmlReportWriter = new JunitXmlReportWriter();
        }

        if (this.instanceId == null) { instanceId = 0; }
    }

    @Override
    public void apply(Project project) {
        instanceId += 1;
        this.project = project;

        project.getExtensions().create("audit", AuditExtensions.class);
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

        if (this.junitReport == null) {
            this.junitReport = settings.junitReport;
        }

        ArtifactGatherer gatherer = factory.getGatherer();
        Set<GradleArtifact> gradleArtifacts = gatherer != null ? gatherer.gatherResolvedArtifacts(task.getProject()) : null;
        DependencyAuditor auditor = factory.getDependencyAuditor(gradleArtifacts, proxies);

        reporter = new AuditResultReporter(gradleArtifacts,
            getAuditExtensions(task.getProject()),
            this,
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
            PackageTreeReporter treeReporter = new PackageTreeReporter(getAuditExtensions(task.getProject()));
            treeReporter.reportDependencyTree(gradleArtifacts, packagesWithVulnerabilities);

            if (this.junitReport != null && (instanceId -= 1) == 0) {
                try {
                    System.out.println("Creating Junit Report");
                    junitXmlReportWriter.writeXmlReport(junitReport);
                } catch (Exception e) {
                    System.out.println("Failed to create JUnit Plugin report: " + e.getMessage());
                }
            } else {
                System.out.println("Junit Report not required. Skipping report generation.");
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
