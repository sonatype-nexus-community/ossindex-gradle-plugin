package net.ossindex.gradle.audit;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import net.ossindex.gradle.AuditExtensions;
import net.ossindex.gradle.OssIndexPlugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.plugins.ExtensionContainer;
import org.junit.Test;

import static org.gradle.internal.impldep.org.junit.Assert.assertEquals;
import static org.gradle.internal.impldep.org.testng.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProxyUnitTest
{
  private static final String PROXY_HOST = "example.com";

  private static final Integer PROXY_PORT = 8080;

  private static final String PROXY_USER = "username";

  private static final String PROXY_PASS = "password";

  /**
   * Ensure that OssIndexPlugin properly assembles the proxy argument and passes it to the DependencyAuditor.
   */
  @Test
  public void settingsProxyTest()
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

      Method method = OssIndexPlugin.class.getDeclaredMethod("getProxy", Project.class, String.class);
      method.setAccessible(true);
      Proxy proxy = (Proxy) method.invoke(plugin, project, "http");
      assertNotNull(proxy);
      assertEquals(getExpectedProxy("http"), proxy);
    } finally {
      field.set(plugin, null);
    }
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
}
