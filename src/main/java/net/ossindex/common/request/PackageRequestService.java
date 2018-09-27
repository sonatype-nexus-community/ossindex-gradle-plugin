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
package net.ossindex.common.request;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.ossindex.common.IPackageRequest;
import net.ossindex.common.PackageCoordinate;
import net.ossindex.common.OssiPackage;
import net.ossindex.common.OssiVulnerability;
import net.ossindex.common.filter.IVulnerabilityFilter;
import net.ossindex.common.filter.VulnerabilityFilterFactory;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Perform a package request.
 *
 * @author Ken Duck
 */
public class PackageRequestService
    implements IPackageRequest
{
  private static final Logger LOG = LoggerFactory.getLogger(PackageRequestService.class);

  private static final long FILE_LOCK_WAIT = 1000;

  private static int MAX_PACKAGES_PER_QUERY = 128;

  private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

  private final OssIndexHttpClient client;

  private boolean debug = false;

  /**
   * Packages returned from the server are in the same order as the packages SENT to the server.
   */
  private PackageRequestDto.Builder packages = PackageRequestDto.newBuilder();

  /**
   * Map of coordinates to their inclusion paths
   */
  private Map<String, List<PackageCoordinate>> paths = new HashMap<>();

  /**
   * List of all filters to apply
   */
  private List<IVulnerabilityFilter> filters = new LinkedList<>();

  private File cacheFile;

  private int cacheTimeout = 12;

  public PackageRequestService(OssIndexHttpClient client) {
    this.client = client;
    File home = new File(System.getProperty("user.home"));
    File cacheDir = new File(home, ".ossindex");
    cacheFile = new File(cacheDir, "gradle.cache");
  }

  public void addVulnerabilityFilter(IVulnerabilityFilter filter) {
    filters.add(filter);
  }

  public void setCacheFile(String path) {
    cacheFile = path != null ? new File(path) : null;
  }

  @Override
  public void setCredentials(final String user, final String token) {
    client.setCredentials(user, token);
  }

  @Override
  public void setMaximumPackagesPerRequest(final int count) {
    MAX_PACKAGES_PER_QUERY = count;
  }

  @Override
  public void setCacheTimeout(final int hours) {
    cacheTimeout = hours;
  }

  /*
   * (non-Javadoc)
   * @see net.ossindex.common.IPackageRequest#add(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
   */
  @Override
  public OssiPackage add(String type, String namespace, String artifactId, String version) {
    PackageCoordinate pkg = PackageCoordinate.newBuilder()
        .withFormat(type)
        .withNamespace(namespace)
        .withName(artifactId)
        .withVersion(version)
        .build();
    return add(Collections.singletonList(pkg));
  }

  @Override
  public OssiPackage add(List<PackageCoordinate> path) {
    if (path != null && !path.isEmpty()) {
      PackageCoordinate pkg = path.get(path.size() - 1);

      // Build a default response for the query
      OssiPackage desc = new OssiPackage(pkg.getType(), pkg.getNamespace(), pkg.getName(), pkg.getVersion());

      // Add the package path to the packages and path lists
      String coord = desc.getCoordinates();
      packages.withCoordinate(coord);
      paths.put(coord.toLowerCase(), path);

      return desc;
    }
    return null;
  }

  /*
   * (non-Javadoc)
   * @see net.ossindex.common.IPackageRequest#run()
   */
  @Override
  public synchronized Collection<OssiPackage> run() throws IOException {
    // Setup the cache
    if (cacheFile != null && !cacheFile.exists()) {
      File cacheDir = cacheFile.getParentFile();
      if (!cacheDir.exists()) {
        cacheDir.mkdirs();
      }
    }
    DB db = null;
    if (cacheFile != null) {
      try {
        db = DBMaker
            .fileDB(cacheFile)
            .transactionEnable()
            .fileLockWait(FILE_LOCK_WAIT)
            .make();
      } catch (Exception e) {
        LOG.error("Could not create cache file (" + cacheFile + "): " + e.getMessage());
        db = DBMaker.memoryDB().make();
      }
    } else {
      db = DBMaker.memoryDB().make();
    }

    HTreeMap<String, String> cache = db.hashMap("cache")
        .keySerializer(Serializer.STRING)
        .valueSerializer(Serializer.STRING)
        .expireAfterCreate(cacheTimeout, TimeUnit.HOURS)
        .createOrOpen();

    try {
      PackageRequestDto dto = packages.build();
      String[] coords = dto.getCoordinates();

      Collection<OssiPackage> results = new HashSet<OssiPackage>();

      // Maximum number of packages per request is 128, so we may need to make multiple requests. The top level
      // loop ensures we check for all coordinates.
      int count = 0;
      while(count < coords.length) {

        PackageRequestDto.Builder usePackages = PackageRequestDto.newBuilder();

        // The second loop only continues until we have enough coords for a single request
        while (count < coords.length && usePackages.size() < MAX_PACKAGES_PER_QUERY) {
          String coord = coords[count++];
          // FIXME: Assumption that all coordinates are case insensitive
          coord = coord.toLowerCase();
          if (cache.containsKey(coord)) {
            LOG.debug("Using cache: " + coord);
            OssiPackage pkg = gson.fromJson(cache.get(coord), OssiPackage.class);
            results.add(pkg);
          }
          else {
            LOG.debug("Add to request: " + coord);
            usePackages.withCoordinate(coord);
          }
        }

        PackageRequestDto useDto = usePackages.build();
        if (useDto.getCoordinates().length > 0) {
          String data = gson.toJson(useDto);
          // Perform the OSS Index query
          String response = client.performPostRequest("component-report", data);

          // Convert the results to Java objects
          Type listType = new TypeToken<List<OssiPackage>>() { }.getType();
          Collection<OssiPackage> newResults = gson.fromJson(response, listType);
          for (OssiPackage pkg : newResults) {
            try {
              results.add(pkg);
              String cacheCoord = pkg.getCoordinates().toLowerCase();
              LOG.debug("Adding to cache: " + cacheCoord);
              cache.put(cacheCoord, gson.toJson(pkg));
              db.commit();
            }
            catch (Exception e) {
              db.rollback();
              throw e;
            }
          }
        }
      }
      results = filterResults(results); // This will remove vulnerabilities from packages
      return results;
    } finally {
      db.close();
    }
  }

  private Collection<OssiPackage> filterResults(final Collection<OssiPackage> pkgs) {
    if (!filters.isEmpty()) {
      List<OssiPackage> results = new LinkedList<>();

      for (OssiPackage pkg: pkgs) {
        String coord = pkg.getCoordinates().toLowerCase();
        if (paths.containsKey(coord)) {
          List<PackageCoordinate> path = paths.get(coord);
          results.add(filterPackage(pkg, path));
        } else {
          LOG.error("WARNING: Could not find inclusion path for " + coord);
          results.add(filterPackage(pkg, Collections.emptyList()));
        }
      }

      return results;
    } else {
      return pkgs;
    }
  }

  private OssiPackage filterPackage(final OssiPackage pkg, final List<PackageCoordinate> path)
  {
    List<OssiVulnerability> vulns = pkg.getVulnerabilities();
    if (vulns != null && !vulns.isEmpty()) {
      OssiPackage.Builder builder = OssiPackage.newBuilder()
          .withCoordinates(pkg.getCoordinates())
          .withDescription(pkg.getDescription())
          .withReference(pkg.getReference())
          .withUnfilteredVulnerabilityMatches(vulns.size());
      // Build a new list of vulnerabilities
      List<OssiVulnerability> filteredVulns = new LinkedList<>();

      // See if any vulnerabilities are filtered
      for (OssiVulnerability vuln : vulns) {
        String vid = vuln.getId();
        for (IVulnerabilityFilter filter : filters) {
          if (!VulnerabilityFilterFactory.shouldFilter(filter, path, vid)) {
            filteredVulns.add(vuln);
          }
        }
      }

      builder.withVulnerabilities(filteredVulns);
      return builder.build();
    }

    // No vulns, just return the original
    return pkg;
  }

  public boolean isDebug() {
    return debug;
  }

  public void setDebug(final boolean debug) {
    this.debug = debug;
  }
}
