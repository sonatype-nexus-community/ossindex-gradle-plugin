/**
 * Copyright (c) 2017 Vör Security Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * * Neither the name of the <organization> nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.ossindex.common;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import net.ossindex.common.filter.IVulnerabilityFilter;
import net.ossindex.common.filter.VulnerabilityFilterFactory;
import net.ossindex.common.request.OssIndexHttpClient;
import net.ossindex.common.request.PackageRequestService;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test the package requests.
 *
 * @author Ken Duck
 */
public class PackageRequestTest
{
  private IPackageRequest request;

  private IVulnerabilityFilter filter;

  private PackageCoordinate vulnerablePkg;

  private PackageCoordinate nonVulnerablePkg;

  private PackageCoordinate anotherPkg;

  private PackageCoordinate vulnerablePkgVersion;

  private PackageCoordinate nonVulnerablePkgVersion;

  private PackageCoordinate anotherPkgVersion;

  @Before
  public void before() throws IOException {
    OssIndexHttpClient client = mock(OssIndexHttpClient.class);
    when(client.performPostRequest(anyString(), anyString())).thenReturn(getPostResponse());
    request = new PackageRequestService(client);
    filter = VulnerabilityFilterFactory.getInstance().createVulnerabilityFilter();

    vulnerablePkg = buildTestPackage();
    nonVulnerablePkg = buildNonVulnerablePackage();
    anotherPkg = buildAnotherPackage();

    vulnerablePkgVersion = PackageCoordinate.newBuilder(vulnerablePkg).withVersion("1.2.3").build();
    nonVulnerablePkgVersion = PackageCoordinate.newBuilder(nonVulnerablePkg).withVersion("2.3.4").build();
    anotherPkgVersion = PackageCoordinate.newBuilder(anotherPkg).withVersion("3.4.5").build();
  }

  @Test
  public void singlePackageRequest() throws IOException {
    request.add("maven", "org.webjars.npm", "jQuery", "1.2.3");
    Collection<OssiPackage> packages = request.run();
    assertEquals(1, packages.size());
    OssiPackage actualPkg = packages.iterator().next();

    List<OssiVulnerability> vulns = actualPkg.getVulnerabilities();
    assertEquals(3, vulns.size());
  }

  @Test
  public void singleFilteredPackageRequest() throws IOException {
    filter.ignorePackage(Collections.singletonList(vulnerablePkg));
    request.addVulnerabilityFilter(filter);

    request.add("maven", "org.webjars.npm", "jQuery", "1.2.3");
    Collection<OssiPackage> packages = request.run();
    assertEquals(1, packages.size());
    OssiPackage actualPkg = packages.iterator().next();

    List<OssiVulnerability> vulns = actualPkg.getVulnerabilities();
    assertEquals(0, vulns.size());
  }

  @Test
  public void unFilteredPackageRequest() throws IOException {
    filter.ignorePackage(Collections.singletonList(nonVulnerablePkg));
    request.addVulnerabilityFilter(filter);

    request.add("maven", "org.webjars.npm", "jQuery", "1.2.3");
    Collection<OssiPackage> packages = request.run();
    assertEquals(1, packages.size());
    OssiPackage actualPkg = packages.iterator().next();

    List<OssiVulnerability> vulns = actualPkg.getVulnerabilities();
    assertEquals(3, vulns.size());
  }

  @Test
  public void singleFilteredIssueRequest() throws IOException {
    filter.ignoreVulnerability("49da4413-af2b-4e55-acc0-9c752e30dde4");
    request.addVulnerabilityFilter(filter);

    request.add("maven", "org.webjars.npm", "jQuery", "1.2.3");
    Collection<OssiPackage> packages = request.run();
    assertEquals(1, packages.size());
    OssiPackage actualPkg = packages.iterator().next();

    List<OssiVulnerability> vulns = actualPkg.getVulnerabilities();
    assertEquals(2, vulns.size());
  }

  @Test
  public void unFilteredIssueRequest() throws IOException {
    filter.ignoreVulnerability("1234567890");
    request.addVulnerabilityFilter(filter);

    request.add("maven", "org.webjars.npm", "jQuery", "1.2.3");
    Collection<OssiPackage> packages = request.run();
    assertEquals(1, packages.size());
    OssiPackage actualPkg = packages.iterator().next();

    List<OssiVulnerability> vulns = actualPkg.getVulnerabilities();
    assertEquals(3, vulns.size());
  }

  @Test
  public void singleFilteredPackageIssueRequest() throws IOException {
    filter.ignoreVulnerability(Collections.singletonList(vulnerablePkg), "49da4413-af2b-4e55-acc0-9c752e30dde4");
    request.addVulnerabilityFilter(filter);

    request.add("maven", "org.webjars.npm", "jQuery", "1.2.3");
    Collection<OssiPackage> packages = request.run();
    assertEquals(1, packages.size());
    OssiPackage actualPkg = packages.iterator().next();

    List<OssiVulnerability> vulns = actualPkg.getVulnerabilities();
    assertEquals(2, vulns.size());
  }

  @Test
  public void unFilteredPackageIssueRequest1() throws IOException {
    filter.ignoreVulnerability(Collections.singletonList(nonVulnerablePkg), "49da4413-af2b-4e55-acc0-9c752e30dde4");
    request.addVulnerabilityFilter(filter);

    request.add("maven", "org.webjars.npm", "jQuery", "1.2.3");
    Collection<OssiPackage> packages = request.run();
    assertEquals(1, packages.size());
    OssiPackage actualPkg = packages.iterator().next();

    List<OssiVulnerability> vulns = actualPkg.getVulnerabilities();
    assertEquals(3, vulns.size());
  }

  @Test
  public void unFilteredPackageIssueRequest2() throws IOException {
    filter.ignoreVulnerability(Collections.singletonList(vulnerablePkg), "1234567890");
    request.addVulnerabilityFilter(filter);

    request.add("maven", "org.webjars.npm", "jQuery", "1.2.3");
    Collection<OssiPackage> packages = request.run();
    assertEquals(1, packages.size());
    OssiPackage actualPkg = packages.iterator().next();

    List<OssiVulnerability> vulns = actualPkg.getVulnerabilities();
    assertEquals(3, vulns.size());
  }

  @Test
  public void filterVulnerabilityInImmediateDependency() throws IOException {
    filter.ignoreVulnerability(Collections.singletonList(vulnerablePkg), "49da4413-af2b-4e55-acc0-9c752e30dde4");
    request.addVulnerabilityFilter(filter);

    request.add(Collections.singletonList(vulnerablePkgVersion));
    Collection<OssiPackage> packages = request.run();
    assertEquals(1, packages.size());
    OssiPackage actualPkg = packages.iterator().next();

    List<OssiVulnerability> vulns = actualPkg.getVulnerabilities();
    assertEquals(2, vulns.size());
  }

  @Test
  public void filterVulnerabilityByChildOnly() throws IOException {
    filter.ignoreVulnerability(Collections.singletonList(vulnerablePkg), "49da4413-af2b-4e55-acc0-9c752e30dde4");
    request.addVulnerabilityFilter(filter);

    request.add(Arrays.asList(new PackageCoordinate[] {anotherPkgVersion, vulnerablePkgVersion}));
    Collection<OssiPackage> packages = request.run();
    assertEquals(1, packages.size());
    OssiPackage actualPkg = packages.iterator().next();

    List<OssiVulnerability> vulns = actualPkg.getVulnerabilities();
    assertEquals(2, vulns.size());
  }

  @Test
  public void filterVulnerabilityByCompletePath() throws IOException {
    filter.ignoreVulnerability(Arrays.asList(new PackageCoordinate[] {anotherPkg, vulnerablePkg}), "49da4413-af2b-4e55-acc0-9c752e30dde4");
    request.addVulnerabilityFilter(filter);

    request.add(Arrays.asList(new PackageCoordinate[] {anotherPkgVersion, vulnerablePkgVersion}));
    Collection<OssiPackage> packages = request.run();
    assertEquals(1, packages.size());
    OssiPackage actualPkg = packages.iterator().next();

    List<OssiVulnerability> vulns = actualPkg.getVulnerabilities();
    assertEquals(2, vulns.size());
  }

  @Test
  public void filterVulnerabilityByCompletePathWithVersion() throws IOException {
    filter.ignoreVulnerability(Arrays.asList(new PackageCoordinate[] {anotherPkgVersion, vulnerablePkgVersion}), "49da4413-af2b-4e55-acc0-9c752e30dde4");
    request.addVulnerabilityFilter(filter);

    request.add(Arrays.asList(new PackageCoordinate[] {anotherPkgVersion, vulnerablePkgVersion}));
    Collection<OssiPackage> packages = request.run();
    assertEquals(1, packages.size());
    OssiPackage actualPkg = packages.iterator().next();

    List<OssiVulnerability> vulns = actualPkg.getVulnerabilities();
    assertEquals(2, vulns.size());
  }

  @Test
  public void dontFilterVulnerabilityByIncorrectPath() throws IOException {
    filter.ignoreVulnerability(Arrays.asList(new PackageCoordinate[] {nonVulnerablePkg, vulnerablePkg}), "49da4413-af2b-4e55-acc0-9c752e30dde4");
    request.addVulnerabilityFilter(filter);

    request.add(Arrays.asList(new PackageCoordinate[] {anotherPkgVersion, vulnerablePkgVersion}));
    Collection<OssiPackage> packages = request.run();
    assertEquals(1, packages.size());
    OssiPackage actualPkg = packages.iterator().next();

    List<OssiVulnerability> vulns = actualPkg.getVulnerabilities();
    assertEquals(3, vulns.size());
  }

  @Test
  public void filterVulnerabilityInChildByParentPackage() throws IOException {
    filter.ignoreVulnerability(Arrays.asList(new PackageCoordinate[] {anotherPkg}), "49da4413-af2b-4e55-acc0-9c752e30dde4");
    request.addVulnerabilityFilter(filter);

    request.add(Arrays.asList(new PackageCoordinate[] {anotherPkgVersion, vulnerablePkgVersion}));
    Collection<OssiPackage> packages = request.run();
    assertEquals(1, packages.size());
    OssiPackage actualPkg = packages.iterator().next();

    List<OssiVulnerability> vulns = actualPkg.getVulnerabilities();
    assertEquals(2, vulns.size());
  }

  @Test
  public void filterVulnerabilitiesInChildByParentPackage() throws IOException {
    filter.ignoreVulnerability(Arrays.asList(new PackageCoordinate[] {anotherPkg}), "49da4413-af2b-4e55-acc0-9c752e30dde4");
    request.addVulnerabilityFilter(filter);

    request.add(Arrays.asList(new PackageCoordinate[] {anotherPkgVersion, vulnerablePkgVersion}));
    Collection<OssiPackage> packages = request.run();
    assertEquals(1, packages.size());
    OssiPackage actualPkg = packages.iterator().next();

    List<OssiVulnerability> vulns = actualPkg.getVulnerabilities();
    assertEquals(2, vulns.size());
  }

  @Test
  public void filterVulnerabilityInChildByParentPackageVersion() throws IOException {
    filter.ignoreVulnerability(Arrays.asList(new PackageCoordinate[] {anotherPkgVersion}), "49da4413-af2b-4e55-acc0-9c752e30dde4");
    request.addVulnerabilityFilter(filter);

    request.add(Arrays.asList(new PackageCoordinate[] {anotherPkgVersion, vulnerablePkgVersion}));
    Collection<OssiPackage> packages = request.run();
    assertEquals(1, packages.size());
    OssiPackage actualPkg = packages.iterator().next();

    List<OssiVulnerability> vulns = actualPkg.getVulnerabilities();
    assertEquals(2, vulns.size());
  }

  @Test
  public void filterVulnerabilityInChildByParentPackageWithOddVersion() throws IOException {
    PackageCoordinate filterParent = PackageCoordinate.newBuilder()
        .withFormat("maven")
        .withNamespace("scot.disclosure")
        .withName("aps-unit-test-utils")
        .build();

    PackageCoordinate filterChild = PackageCoordinate.newBuilder()
        .withFormat("maven")
        .withNamespace("javax.servlet")
        .withName("jstl")
        .withVersion("1.2")
        .build();

    filter.ignoreVulnerability(Arrays.asList(new PackageCoordinate[] {filterParent, filterChild}), "52f593c8-7729-435c-b9df-a7bb9ded8589");
    filter.ignoreVulnerability(Arrays.asList(new PackageCoordinate[] {filterParent, filterChild}), "49da4413-af2b-4e55-acc0-9c752e30dde4");
    filter.ignoreVulnerability(Arrays.asList(new PackageCoordinate[] {filterParent, filterChild}), "3b3ba2f8-9c2c-4afe-b593-75c6b3fd4bb7");
    request.addVulnerabilityFilter(filter);

    PackageCoordinate parent = PackageCoordinate.newBuilder()
        .withFormat("maven")
        .withNamespace("scot.disclosure")
        .withName("aps-unit-test-utils")
        .withVersion("0.3.0-20180517102126-f776111-46")
        .build();

    PackageCoordinate child = PackageCoordinate.newBuilder()
        .withFormat("maven")
        .withNamespace("javax.servlet")
        .withName("jstl")
        .withVersion("1.2")
        .build();

    request.add(Arrays.asList(new PackageCoordinate[] {parent, child}));
    Collection<OssiPackage> packages = request.run();
    assertEquals(1, packages.size());
    OssiPackage actualPkg = packages.iterator().next();

    List<OssiVulnerability> vulns = actualPkg.getVulnerabilities();
    assertEquals(0, vulns.size());
  }


  private PackageCoordinate buildTestPackage() {
    PackageCoordinate pkg = PackageCoordinate.newBuilder()
        .withFormat("maven")
        .withNamespace("org.webjars.npm")
        .withName("jQuery")
        .build();
    return pkg;
  }

  private PackageCoordinate buildNonVulnerablePackage() {
    PackageCoordinate pkg = PackageCoordinate.newBuilder()
        .withFormat("maven")
        .withNamespace("org.webjars.npm")
        .withName("jQuery-patched")
        .build();
    return pkg;
  }

  private PackageCoordinate buildAnotherPackage() {
    PackageCoordinate pkg = PackageCoordinate.newBuilder()
        .withFormat("maven")
        .withNamespace("my.project")
        .withName("MyProg")
        .build();
    return pkg;
  }

  public String getPostResponse() {
    return "[\n" +
        "  {\n" +
        "    \"coordinates\": \"maven:org.webjars.npm/jquery@1.2.3\",\n" +
        "    \"description\": \"WebJar for jquery\",\n" +
        "    \"reference\": \"https://ossindex.sonatype.org/component/maven:org.webjars.npm/jquery@1.2.3\",\n" +
        "    \"vulnerabilities\": [\n" +
        "      {\n" +
        "        \"id\": \"49da4413-af2b-4e55-acc0-9c752e30dde4\",\n" +
        "        \"title\": \"CWE-79: Improper Neutralization of Input During Web Page Generation ('Cross-site Scripting')\",\n" +
        "        \"description\": \"The software does not neutralize or incorrectly neutralizes user-controllable input before it is placed in output that is used as a web page that is served to other users.\",\n" +
        "        \"cvssScore\": 7.2,\n" +
        "        \"cvssVector\": \"CVSS:3.0/AV:N/AC:L/PR:N/UI:N/S:C/C:L/I:L/A:N\",\n" +
        "        \"cwe\": \"CWE-79\",\n" +
        "        \"reference\": \"https://ossindex.sonatype.org/vuln/49da4413-af2b-4e55-acc0-9c752e30dde4\"\n" +
        "      },\n" +
        "      {\n" +
        "        \"id\": \"52f593c8-7729-435c-b9df-a7bb9ded8589\",\n" +
        "        \"title\": \"CWE-79: Improper Neutralization of Input During Web Page Generation ('Cross-site Scripting')\",\n" +
        "        \"description\": \"The software does not neutralize or incorrectly neutralizes user-controllable input before it is placed in output that is used as a web page that is served to other users.\",\n" +
        "        \"cvssScore\": 6.1,\n" +
        "        \"cvssVector\": \"CVSS:3.0/AV:N/AC:L/PR:N/UI:R/S:C/C:L/I:L/A:N\",\n" +
        "        \"cwe\": \"CWE-79\",\n" +
        "        \"reference\": \"https://ossindex.sonatype.org/vuln/52f593c8-7729-435c-b9df-a7bb9ded8589\"\n" +
        "      },\n" +
        "      {\n" +
        "        \"id\": \"3b3ba2f8-9c2c-4afe-b593-75c6b3fd4bb7\",\n" +
        "        \"title\": \"[CVE-2015-9251] jQuery before 3.0.0 is vulnerable to Cross-site Scripting (XSS) attacks when a c...\",\n" +
        "        \"description\": \"jQuery before 3.0.0 is vulnerable to Cross-site Scripting (XSS) attacks when a cross-domain Ajax request is performed without the dataType option, causing text/javascript responses to be executed.\",\n" +
        "        \"cvssScore\": 6.1,\n" +
        "        \"cvssVector\": \"CVSS:3.0/AV:N/AC:L/PR:N/UI:R/S:C/C:L/I:L/A:N\",\n" +
        "        \"reference\": \"https://ossindex.sonatype.org/vuln/3b3ba2f8-9c2c-4afe-b593-75c6b3fd4bb7\"\n" +
        "      }\n" +
        "    ]\n" +
        "  }\n" +
        "]";
  }
}
