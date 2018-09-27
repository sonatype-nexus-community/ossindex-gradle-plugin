package net.ossindex.common;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNull;

public class OssiPackagePurlTest
{
  @Test
  public void testCompletePurls() {
    BasicPurl purl = new BasicPurl("maven:org.webjars.npm/jquery@1.9.0");
    assertThat(purl.getType(), equalTo("maven"));
    assertThat(purl.getNamespace(), equalTo("org.webjars.npm"));
    assertThat(purl.getName(), equalTo("jquery"));
    assertThat(purl.getVersion(), equalTo("1.9.0"));
  }
  @Test
  public void testPurlWithoutNamespace() {
    BasicPurl purl = new BasicPurl("maven:jquery@1.9.0");
    assertThat(purl.getType(), equalTo("maven"));
    assertNull(purl.getNamespace());
    assertThat(purl.getName(), equalTo("jquery"));
    assertThat(purl.getVersion(), equalTo("1.9.0"));
  }
  @Test
  public void testPurlWithoutVersion() {
    BasicPurl purl = new BasicPurl("maven:org.webjars.npm/jquery");
    assertThat(purl.getType(), equalTo("maven"));
    assertThat(purl.getNamespace(), equalTo("org.webjars.npm"));
    assertThat(purl.getName(), equalTo("jquery"));
    assertNull(purl.getVersion());
  }
}
