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
import java.util.Collection;
import java.util.List;

import net.ossindex.common.filter.IVulnerabilityFilter;

/** Interface to be implemented for the package request API.
 *
 * @author Ken Duck
 *
 */
public interface IPackageRequest
{

  /** Add a new artifact to search for.
   *
   * @param pm Name of the package manager
   * @param groupId Group ID for the package
   * @param artifactId Artifact ID for the package
   * @param version Version number for request
   * @return A package descriptor containing the information
   */
  public OssiPackage add(String pm, String groupId, String artifactId, String version);

  /** Add a new package/version (with dependency information) to search for. This is useful for advanced
   * filtering.
   *
   * @param path A path from the root (including) package to the dependency package. The packager being checked
   *            is the last one placed on the list (max index).
   * @return A package descriptor containing the information
   */
  public OssiPackage add(List<PackageCoordinate> path);

  /**
   * Filter the request results using the specified filter(s)
   */
  public void addVulnerabilityFilter(IVulnerabilityFilter filter);

  /**
   * Execute the request.
   *
   * @throws IOException
   */
  public Collection<OssiPackage> run() throws IOException;

  /**
   * Set the file path to be used by the cache.
   * Set to null to disable the cache.
   */
  public void setCacheFile(String path);

  /**
   * Set the credentials to be used in the OSS Index query
   */
  public void setCredentials(String user, String token);

  /**
   *
   */
  public void setMaximumPackagesPerRequest(int count);

  /**
   * Number of hours till the cache expires. Only affects new items in the cache.
   */
  public void setCacheTimeout(int hours);
}
