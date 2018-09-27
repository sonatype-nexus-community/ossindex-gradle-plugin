package net.ossindex.common;

/**
 * Quick and dirty till an external Purl parser can be found. Assumes input is exactly what we expect.
 */
public class BasicPurl
{
  private String type;

  private String namespace;

  private String name;

  private String version;

  public BasicPurl(final String purl) {
    String p = purl;
    int index = p.indexOf(':');
    type = p.substring(0, index);
    p = p.substring(index + 1);
    index = p.indexOf('/');
    if (index > 0) {
      namespace = p.substring(0, index);
      p = p.substring(index + 1);
    }
    index = p.indexOf('@');
    if (index > 0) {
      // Has version
      name = p.substring(0, index);
      p = p.substring(index + 1);

      version = p;
    }
    else {
      // No version
      name = p;
    }
  }

  public String getType() {
    return type;
  }

  public String getNamespace() {
    return namespace;
  }

  public String getName() {
    return name;
  }

  public String getVersion() {
    return version;
  }

  public String toString() {
    if (getNamespace() != null) {
      return getType() + ":" + getNamespace() + "/" + getName() + "@" + getVersion();
    } else {
      return getType() + ":" + getName() + "@" + getVersion();
    }
  }
}
