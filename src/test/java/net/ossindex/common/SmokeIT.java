package net.ossindex.common;

import java.io.IOException;
import java.net.ConnectException;
import java.util.Collection;
import java.util.Collections;

import org.junit.Test;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SmokeIT
{
  @Test
  public void simpleTest() throws IOException {
    IPackageRequest client = getVulnerableCoordinateRequest();
    client.setCacheFile(null);
    Collection<OssiPackage> results = client.run();
    assertFalse(results.isEmpty());
  }

  @Test
  public void badCredentialsTest() throws IOException {
    IPackageRequest client = getVulnerableCoordinateRequest();
    client.setCredentials("invalidUser", "invalidToken");
    client.setCacheFile(null);
    try {
      client.run();
      // Bad credentials should mean we never get here
      fail();
    } catch (ConnectException e) {
      assertTrue(e.getMessage().contains("401"));
    }
  }

  private IPackageRequest getVulnerableCoordinateRequest() {
    IPackageRequest client = OssIndexApi.createPackageRequest();
    PackageCoordinate coord = PackageCoordinate.newBuilder()
        .withFormat("maven")
        .withNamespace("poi")
        .withName("poi")
        .withVersion("1.2.3")
        .build();
    client.add(Collections.singletonList(coord));
    return client;
  }
}
