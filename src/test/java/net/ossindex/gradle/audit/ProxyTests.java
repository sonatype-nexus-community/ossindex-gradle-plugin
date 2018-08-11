package net.ossindex.gradle.audit;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;

import net.ossindex.gradle.AuditExtensions;
import net.ossindex.gradle.OssIndexPlugin;
import net.ossindex.gradle.input.ArtifactGatherer;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.plugins.ExtensionContainer;
import org.junit.Test;

import static org.gradle.internal.impldep.org.junit.Assert.assertEquals;
import static org.gradle.internal.impldep.org.testng.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * A series of tests that simulate the operation of gradle and ensure that appropriate proxy arguments are
 * passed to the DependencyAuditor.
 */
public class ProxyTests
{
  private static final String PROXY_HOST = "example.com";

  private static final Integer PROXY_PORT = 8080;

  private static final String PROXY_USER = "username";

  private static final String PROXY_PASS = "password";

  private static final String NON_PROXY_HOSTS = "ossindex.net|ossindex.sonatype.org";

  /**
   * Ensure that OssIndexPlugin properly assembles the proxy argument and passes it to the DependencyAuditor.
   * If the dependency auditor receives proxy information it is passed directly to the OSS Index request
   * library.
   */
  @Test
  public void noProxyTest() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
    Project project = mockProject();

    OssIndexPlugin plugin = new OssIndexPlugin();
    AuditorFactory factory = mockAuditorFactory();
    plugin.setAuditorFactory(factory);

    // Simulate the process the gradle runs
    runGradleSimulation(project, plugin);

    verify(factory).getDependencyAuditor(null, Collections.EMPTY_SET, Collections.EMPTY_LIST);
  }

  /**
   * Ensure that OssIndexPlugin properly assembles the proxy argument and passes it to the DependencyAuditor.
   */
  @Test
  public void settingsProxyIT()
      throws InvocationTargetException, IllegalAccessException, NoSuchMethodException, NoSuchFieldException
  {
    Project project = mockProject();
    AuditExtensions settings = new AuditExtensions(project);
    settings.proxyScheme = "http";
    settings.proxyHost = PROXY_HOST;
    settings.proxyPort = PROXY_PORT;
    settings.proxyUser = PROXY_USER;
    settings.proxyPassword = PROXY_PASS;
    settings.nonProxyHosts = null;

    OssIndexPlugin plugin = new OssIndexPlugin();
    Field field = OssIndexPlugin.class.getDeclaredField("settings");
    try {
      field.setAccessible(true);
      field.set(plugin, settings);

      AuditorFactory factory = mockAuditorFactory();
      plugin.setAuditorFactory(factory);

      // Simulate the process the gradle runs
      runGradleSimulation(project, plugin);

      verify(factory)
          .getDependencyAuditor(null, Collections.EMPTY_SET, Collections.singletonList(getExpectedProxy("http")));
    } finally {
      field.set(plugin, null);
    }
  }

  /**
   * Ensure that OssIndexPlugin properly assembles the proxy argument and passes it to the DependencyAuditor.
   */
  @Test
  public void httpLocalProxyTest() throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
    Project project = mockProject();

    // Mock the proxy being provided as project properties
    mockLocalProxy(project, "http");

    OssIndexPlugin plugin = new OssIndexPlugin();
    AuditorFactory factory = mockAuditorFactory();
    plugin.setAuditorFactory(factory);

    // Simulate the process the gradle runs
    runGradleSimulation(project, plugin);

    verify(factory).getDependencyAuditor(null, Collections.EMPTY_SET, Collections.singletonList(getExpectedProxy("http")));
  }

  /**
   * Ensure that OssIndexPlugin properly assembles the proxy argument and passes it to the DependencyAuditor.
   */
  @Test
  public void httpsLocalProxyTest() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
    Project project = mockProject();

    // Mock the proxy being provided as project properties
    mockLocalProxy(project, "https");

    OssIndexPlugin plugin = new OssIndexPlugin();
    AuditorFactory factory = mockAuditorFactory();
    plugin.setAuditorFactory(factory);

    // Simulate the process the gradle runs
    runGradleSimulation(project, plugin);

    verify(factory).getDependencyAuditor(null, Collections.EMPTY_SET, Collections.singletonList(getExpectedProxy("https")));
  }

  /**
   * Ensure that OssIndexPlugin properly assembles the proxy argument and passes it to the DependencyAuditor.
   */
  @Test
  public void httpSystemProxyTest() throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
    Project project = mockProject();

    // Mock the proxy being provided as project properties
    mockSystemProxy(project, "http");

    OssIndexPlugin plugin = new OssIndexPlugin();
    AuditorFactory factory = mockAuditorFactory();
    plugin.setAuditorFactory(factory);

    // Simulate the process the gradle runs
    runGradleSimulation(project, plugin);

    verify(factory).getDependencyAuditor(null, Collections.EMPTY_SET, Collections.singletonList(getExpectedProxy("http")));
  }

  /**
   * Ensure that OssIndexPlugin properly assembles the proxy argument and passes it to the DependencyAuditor.
   */
  @Test
  public void httpsSystemProxyTest() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
    Project project = mockProject();

    // Mock the proxy being provided as project properties
    mockSystemProxy(project, "https");

    OssIndexPlugin plugin = new OssIndexPlugin();
    AuditorFactory factory = mockAuditorFactory();
    plugin.setAuditorFactory(factory);

    // Simulate the process the gradle runs
    runGradleSimulation(project, plugin);

    verify(factory).getDependencyAuditor(null, Collections.EMPTY_SET, Collections.singletonList(getExpectedProxy("https")));
  }

  /**
   * Mock the factory used by the auditor. The factory was specifically created to simplify unit tests. The factory
   * allows the assembly of:
   *
   * - The artifact gatherer. This is normally provided by gradle, but needs mocking for testing purposes.
   * - The dependency auditor. For normal usage this is directly instantiated.
   */
  private AuditorFactory mockAuditorFactory() {
    AuditorFactory factory = mock(AuditorFactory.class);

    ArtifactGatherer gatherer = mock(ArtifactGatherer.class);
    when(gatherer.gatherResolvedArtifacts(any())).thenReturn(Collections.EMPTY_SET);
    when(factory.getGatherer()).thenReturn(gatherer);

    DependencyAuditor auditor = mock(DependencyAuditor.class);
    when(factory.getDependencyAuditor(eq(null), any(), any())).thenReturn(auditor);
    return factory;
  }

  /**
   * Assemble the gradle project with appropriate task.
   */
  private Project mockProject() {
    Project project = mock(Project.class);

    ExtensionContainer extension = mock(ExtensionContainer.class);
    when(project.getExtensions()).thenReturn(extension);
    when(project.getDisplayName()).thenReturn("Mock Mock");

    Task audit = mock(Task.class);
    when(audit.getProject()).thenReturn(project);
    when(project.task("audit")).thenReturn(audit);

    return project;
  }

  /**
   * Mock the project properties for a specified proxy
   */
  private void mockLocalProxy(final Project project, final String scheme) {
    when(project.hasProperty(scheme + ".proxyHost")).thenReturn(true);
    when(project.findProperty(scheme + ".proxyHost")).thenReturn(PROXY_HOST);
    when(project.findProperty(scheme + ".proxyPort")).thenReturn(PROXY_PORT.toString());
    when(project.findProperty(scheme + ".proxyUser")).thenReturn(PROXY_USER);
    when(project.findProperty(scheme + ".proxyPassword")).thenReturn(PROXY_PASS);
    when(project.findProperty(scheme + ".nonProxyHosts")).thenReturn(null);
  }

  /**
   * Mock the project properties for a specified proxy
   */
  private void mockSystemProxy(final Project project, final String scheme) {
    when(project.hasProperty("systemProp." + scheme + ".proxyHost")).thenReturn(true);
    when(project.findProperty("systemProp." + scheme + ".proxyHost")).thenReturn(PROXY_HOST);
    when(project.findProperty("systemProp." + scheme + ".proxyPort")).thenReturn(PROXY_PORT.toString());
    when(project.findProperty("systemProp." + scheme + ".proxyUser")).thenReturn(PROXY_USER);
    when(project.findProperty("systemProp." + scheme + ".proxyPassword")).thenReturn(PROXY_PASS);
    when(project.findProperty("systemProp." + scheme + ".nonProxyHosts")).thenReturn(null);
  }

  /**
   * The proxy expected as part of the test
   */
  private Proxy getExpectedProxy(final String scheme) {
    Proxy proxy = new Proxy();
    proxy.setHost(PROXY_HOST);
    proxy.setPort(PROXY_PORT);
    proxy.setUser(PROXY_USER);
    proxy.setPassword(PROXY_PASS);
    proxy.setNonProxyHosts(null);
    return proxy;
  }


  /**
   * Simulate gradle operations
   *
   * This is a terrible hack that shortcuts the internal workings of gradle to allow some simple tests to work
   */
  private void runGradleSimulation(final Project project, final OssIndexPlugin plugin)
      throws NoSuchMethodException, IllegalAccessException, InvocationTargetException
  {
    plugin.apply(project);

    Method method = OssIndexPlugin.class.getDeclaredMethod("doAudit", Task.class);
    method.setAccessible(true);
    method.invoke(plugin, project.task("audit"));
  }

}
