package net.ossindex.gradle.audit;

import java.util.Objects;

public class Proxy
{
  private String scheme;

  private String host;

  private Integer port;

  private String user;

  private String password;

  private String[] nonProxyHosts;

  public String getScheme() {
    return scheme;
  }

  public String getHost() {
    return host;
  }

  public int getPort() {
    if (port == null) {
      switch (scheme) {
        case "http":
          return 80;
        case "https":
          return 443;
        default:
          throw new IllegalArgumentException("Unknown proxy scheme: " + scheme);
      }
    }
    return port;
  }

  public String getUser() {
    return user;
  }

  public String getPassword() {
    return password;
  }

  public void setScheme(final String scheme) {
    this.scheme = scheme;
  }

  public void setHost(final String host) {
    this.host = host;
  }

  public void setPort(final Integer port) {
    this.port = port;
  }

  public void setUser(final String user) {
    this.user = user;
  }

  public void setPassword(final String password) {
    this.password = password;
  }

  public void setNonProxyHosts(final String nonProxyHosts) {
    if (nonProxyHosts != null) {
      this.nonProxyHosts = nonProxyHosts.split("|");
    }
  }

  public boolean isValid() {
    if (host == null) {
      return false;
    }

    if (nonProxyHosts != null) {
      for (String nonProxyHost: nonProxyHosts) {
        if (host.equals(nonProxyHost)) {
          return false;
        }
      }
    }

    return true;
  }

  @Override
  public String toString() {
    return user + "@" + host + ":" + getPort();
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof Proxy) {
      Proxy p = (Proxy)o;
      return Objects.equals(scheme, p.scheme) &&
          Objects.equals(host, p.host) &&
          Objects.equals(port, p.port) &&
          Objects.equals(user, p.user) &&
          Objects.equals(password, p.password) &&
          Objects.equals(nonProxyHosts, p.nonProxyHosts);
    }
    return false;
  }
}
