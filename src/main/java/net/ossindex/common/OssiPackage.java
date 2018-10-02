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

import java.util.List;
import org.sonatype.goodies.packageurl.PackageUrl;

/**
 * Represents an OSS Index package.
 *
 * @author Ken Duck
 */
public class OssiPackage
{

  private String coordinates;

  private String description;

  private String reference;

  private int unfilteredVulnerabilityMatches;

  private List<OssiVulnerability> vulnerabilities;

  private transient PackageUrl purl;

  /**
   * Create a package coordinate
   */
  public OssiPackage(String type, String namespace, String artifactId, String version) {
    if (namespace == null || namespace.isEmpty()) {
      this.coordinates = "pkg:" + type + "/" + artifactId + "@" + version;
    }
    else {
      this.coordinates = "pkg:" + type + "/" + namespace + "/" + artifactId + "@" + version;
    }
  }

  private OssiPackage(final Builder builder) {
    coordinates = builder.coordinates;
    description = builder.description;
    reference = builder.reference;
    unfilteredVulnerabilityMatches = builder.unfilteredVulnerabilityMatches;
    vulnerabilities = builder.vulnerabilities;
    purl = builder.purl;
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public static Builder newBuilder(final OssiPackage copy) {
    Builder builder = new Builder();
    builder.coordinates = copy.coordinates;
    builder.description = copy.description;
    builder.reference = copy.reference;
    builder.unfilteredVulnerabilityMatches = copy.unfilteredVulnerabilityMatches;
    builder.vulnerabilities = copy.vulnerabilities;
    builder.purl = copy.purl;
    return builder;
  }

  public String getCoordinates() {
    return coordinates;
  }

  public String getDescription() {
    return description;
  }

  public String getReference() {
    return reference;
  }

  public List<OssiVulnerability> getVulnerabilities() {
    return vulnerabilities;
  }

  public int getUnfilteredVulnerabilityMatches() {
    return unfilteredVulnerabilityMatches;
  }

  public PackageUrl getPurl() {
    return purl;
  }

  public String getType() {
    if (purl == null) {
      purl = PackageUrl.parse(coordinates);
    }
    return purl.getType();
  }

  public String getNamespace() {
    if (purl == null) {
      purl = PackageUrl.parse(coordinates);
    }
    return purl.getNamespaceAsString();
  }

  public String getName() {
    if (purl == null) {
      purl = PackageUrl.parse(coordinates);
    }
    return purl.getName();
  }

  public String getVersion() {
    if (purl == null) {
      purl = PackageUrl.parse(coordinates);
    }
    return purl.getVersion();
  }

  public static final class Builder
  {
    private String coordinates;

    private String description;

    private String reference;

    private int unfilteredVulnerabilityMatches;

    private List<OssiVulnerability> vulnerabilities;

    private PackageUrl purl;

    private Builder() {}

    public Builder withCoordinates(final String val) {
      coordinates = val;
      return this;
    }

    public Builder withDescription(final String val) {
      description = val;
      return this;
    }

    public Builder withReference(final String val) {
      reference = val;
      return this;
    }

    public Builder withUnfilteredVulnerabilityMatches(final int val) {
      unfilteredVulnerabilityMatches = val;
      return this;
    }

    public Builder withVulnerabilities(final List<OssiVulnerability> val) {
      vulnerabilities = val;
      return this;
    }

    public Builder withPurl(final PackageUrl val) {
      purl = val;
      return this;
    }

    public OssiPackage build() {
      return new OssiPackage(this);
    }
  }

}
