package net.ossindex.integrationtests;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Set;

import net.ossindex.gradle.OssIndexPlugin;
import net.ossindex.gradle.audit.AuditorFactory;
import net.ossindex.gradle.audit.DependencyAuditor;
import net.ossindex.gradle.audit.Proxy;
import net.ossindex.gradle.input.ArtifactGatherer;
import net.ossindex.gradle.input.GradleArtifact;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.plugins.ExtensionContainer;
import org.junit.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ProxyTests
{
  private static final String PROXY_HOST = "example.com";
  private static final Integer PROXY_PORT = 8080;
  private static final String PROXY_USER = "username";
  private static final String PROXY_PASS = "password";
  private static final String NON_PROXY_HOSTS = "ossindex.net|ossindex.sonatype.org";

  @Test
  public void noProxyTest() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
    Project project = mock(Project.class);

    OssIndexPlugin plugin = new OssIndexPlugin();
    AuditorFactory factory = mockAuditorFactory();
    plugin.setAuditorFactory(mockAuditorFactory());
    plugin.apply(project);
    runAudit(project, plugin);

    verify(factory).getDependencyAuditor(Collections.EMPTY_SET, Collections.EMPTY_LIST);
  }

  @Test
  public void httpProxyTest() throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
    Project project = mockProject();
    mockProxy(project, "http");

    OssIndexPlugin plugin = new OssIndexPlugin();
    AuditorFactory factory = mockAuditorFactory();
    plugin.setAuditorFactory(mockAuditorFactory());

    plugin.apply(project);
    runAudit(project, plugin);

    verify(factory).getDependencyAuditor(Collections.EMPTY_SET, Collections.singletonList(getProxy("http")));
  }

  @Test
  public void httpsProxyTest() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
    Project project = mockProject();
    mockProxy(project, "https");

    OssIndexPlugin plugin = new OssIndexPlugin();
    AuditorFactory factory = mockAuditorFactory();
    plugin.setAuditorFactory(mockAuditorFactory());

    plugin.apply(project);
    runAudit(project, plugin);

    verify(factory).getDependencyAuditor(Collections.EMPTY_SET, Collections.singletonList(getProxy("https")));
  }

  /**
   * A terrible hack that shortcuts the internal workings of gradle to allow some simple tests to work
   */
  private void runAudit(final Project project, final OssIndexPlugin plugin)
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException
  {
    Method method = OssIndexPlugin.class.getDeclaredMethod("doAudit", Task.class);
    method.setAccessible(true);
    method.invoke(plugin, project.task("audit"));
  }

  private AuditorFactory mockAuditorFactory() {
    AuditorFactory factory = mock(AuditorFactory.class);

    ArtifactGatherer gatherer = mock(ArtifactGatherer.class);
    when(gatherer.gatherResolvedArtifacts(any())).thenReturn(Collections.EMPTY_SET);
    when(factory.getGatherer()).thenReturn(gatherer);

    DependencyAuditor auditor  = mock(DependencyAuditor.class);
    when(factory.getDependencyAuditor(any(), any())).thenReturn(auditor);
    return factory;
  }

  private Project mockProject() {
    Project project = mock(Project.class);

    ExtensionContainer extension = mock(ExtensionContainer.class);
    when(project.getExtensions()).thenReturn(extension);

    Task audit = mock(Task.class);
    when(audit.getProject()).thenReturn(project);
    when(project.task("audit")).thenReturn(audit);

    return project;
  }

  private void mockProxy(final Project project, final String scheme) {
    when(project.findProperty("systemProp." + scheme + ".proxyHost")).thenReturn(PROXY_HOST);
    when(project.findProperty("systemProp." + scheme + ".proxyPort")).thenReturn(PROXY_PORT.toString());
    when(project.findProperty("systemProp." + scheme + ".proxyUser")).thenReturn(PROXY_USER);
    when(project.findProperty("systemProp." + scheme + ".proxyPassword")).thenReturn(PROXY_PASS);
    when(project.findProperty("systemProp." + scheme + ".nonProxyHosts")).thenReturn(null);
  }

  private Proxy getProxy(final String scheme) {
    Proxy proxy = new Proxy();
    proxy.setHost(PROXY_HOST);
    proxy.setPort(PROXY_PORT);
    proxy.setUser(PROXY_USER);
    proxy.setPassword(PROXY_PASS);
    proxy.setNonProxyHosts(null);
    return proxy;
  }
}
