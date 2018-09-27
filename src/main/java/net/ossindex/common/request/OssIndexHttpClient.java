package net.ossindex.common.request;

import java.io.IOException;
import java.net.ConnectException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.ParseException;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OssIndexHttpClient
{
  private static final Logger logger = LoggerFactory.getLogger(OssIndexHttpClient.class);
  
  private List<Proxy> proxies = new LinkedList<Proxy>();

  private static String SCHEME = "https";

  private static String HOST = "ossindex.sonatype.org";

  private static String VERSION = "api/v3";

  private static String BASE_URL = SCHEME + "://" + HOST + "/" + VERSION + "/";

  private static final int DEFAULT_PORT = 443;

  private static final String CONTENT_TYPE = "application/json";

  private Credentials credentials;

  private static HttpHost targetHost = new HttpHost(HOST, DEFAULT_PORT, SCHEME);

  static {
    String scheme = System.getProperty("OSSINDEX_SCHEME");
    if (scheme != null && !scheme.trim().isEmpty()) {
      SCHEME = scheme.trim();
    }
    String host = System.getProperty("OSSINDEX_HOST");
    if (host != null && !host.trim().isEmpty()) {
      HOST = host.trim();
    }
    String version = System.getProperty("OSSINDEX_VERSION");
    if (version != null && !version.trim().isEmpty()) {
      VERSION = version.trim();
    }
    BASE_URL = SCHEME + "://" + HOST + "/" + VERSION + "/";
  }

  private static int PROXY_SOCKET_TIMEOUT = 60000;

  private static int PROXY_CONNECT_TIMEOUT = 10000;

  private static int PROXY_CONNECTION_REQUEST_TIMEOUT = 10000;

  static {
    // Define the default proxy settings using environment variables
    String tmp = System.getenv("OSSINDEX_PROXY_SOCKET_TIMEOUT");
    if (tmp != null && !tmp.trim().isEmpty()) {
      PROXY_SOCKET_TIMEOUT = Integer.parseInt(tmp.trim());
    }
    tmp = System.getenv("OSSINDEX_PROXY_CONNECT_TIMEOUT");
    if (tmp != null && !tmp.trim().isEmpty()) {
      PROXY_CONNECT_TIMEOUT = Integer.parseInt(tmp.trim());
    }
    tmp = System.getenv("OSSINDEX_PROXY_CONNECTION_REQUEST_TIMEOUT");
    if (tmp != null && !tmp.trim().isEmpty()) {
      PROXY_CONNECTION_REQUEST_TIMEOUT = Integer.parseInt(tmp.trim());
    }
  }


  static {
    // Define the default proxy settings using properties
    String tmp = System.getProperty("OSSINDEX_PROXY_SOCKET_TIMEOUT");
    if (tmp != null && !tmp.trim().isEmpty()) {
      PROXY_SOCKET_TIMEOUT = Integer.parseInt(tmp.trim());
    }
    tmp = System.getProperty("OSSINDEX_PROXY_CONNECT_TIMEOUT");
    if (tmp != null && !tmp.trim().isEmpty()) {
      PROXY_CONNECT_TIMEOUT = Integer.parseInt(tmp.trim());
    }
    tmp = System.getProperty("OSSINDEX_PROXY_CONNECTION_REQUEST_TIMEOUT");
    if (tmp != null && !tmp.trim().isEmpty()) {
      PROXY_CONNECTION_REQUEST_TIMEOUT = Integer.parseInt(tmp.trim());
    }
  }

  private static final String USER = System.getProperty("OSSINDEX_USER");

  private static final String TOKEN = System.getProperty("OSSINDEX_TOKEN");

  /**
   * Override the server location.
   */
  static void setHost(String scheme, String host, int port) {
    BASE_URL = scheme + "://" + host + ":" + port + "/" + VERSION + "/";
    targetHost = new HttpHost(host, port, scheme);
  }

  public void setCredentials(final String user, final String token) {
    credentials = new UsernamePasswordCredentials(user, token);
  }

  /**
   * Perform the request with the given URL and JSON data.
   *
   * @param requestString Server request relative URL
   * @param data          JSON data for the request
   * @return JSON results of the request
   * @throws IOException On query problems
   */
  public String performPostRequest(String requestString, String data) throws IOException {
    HttpPost request = new HttpPost(getBaseUrl() + requestString);
    String json = null;

    request.setHeader(new BasicHeader(HttpHeaders.CONTENT_TYPE, CONTENT_TYPE));
    request.setHeader(new BasicHeader(HttpHeaders.ACCEPT, CONTENT_TYPE));
    request.setHeader(new BasicHeader(HttpHeaders.CONTENT_ENCODING, CONTENT_TYPE));

    HttpClientBuilder httpClientBuilder = HttpClients.custom()
        .setDefaultRequestConfig(RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).build());

    final HttpClientContext context = HttpClientContext.create();
    if (credentials != null) {
      CredentialsProvider provider = new BasicCredentialsProvider();
      provider.setCredentials(AuthScope.ANY, credentials);

      AuthCache authCache = new BasicAuthCache();
      authCache.put(targetHost, new BasicScheme());

      // Add AuthCache to the execution context
      context.setCredentialsProvider(provider);
      context.setAuthCache(authCache);
    }

    CloseableHttpClient httpClient = httpClientBuilder.build();
    if (proxies.size() > 0) {
      // We only check the first proxy for now
      Proxy myProxy = proxies.get(0);
      HttpHost proxy = myProxy.getHttpHost();
      RequestConfig config = RequestConfig.custom()
          .setProxy(proxy)
          .setSocketTimeout(myProxy.getProxySocketTimeout())
          .setConnectTimeout(myProxy.getProxyConnectTimeout())
          .setConnectionRequestTimeout(myProxy.getProxyConnectionRequestTimeout())
          .setProxyPreferredAuthSchemes(Arrays.asList(AuthSchemes.BASIC))
          .build();
      request.setConfig(config);
    }
    try {
      logger.debug("------------------------------------------------");
      logger.debug("OSS Index POST: " + getBaseUrl() + requestString);
      logger.debug(data);
      logger.debug("------------------------------------------------");

      request.setEntity(new StringEntity(data));
      CloseableHttpResponse response = httpClient.execute(request, context);
      int code = response.getStatusLine().getStatusCode();
      if (code < 200 || code > 299) {
        throw new ConnectException(response.getStatusLine().getReasonPhrase() + " (" + code + ")");
      }
      json = EntityUtils.toString(response.getEntity(), "UTF-8");
    }
    catch (ParseException e) {
      throw new IOException(e);
    }
    finally {
      httpClient.close();
    }
    return json;
  }

  /**
   * Get the base URL for requests
   *
   * @return The base URL
   */
  private String getBaseUrl() {
    return BASE_URL;
  }


  /**
   * Add a proxy server through which to make requests
   */
  public void addProxy(String protocol, String host, int port, String username, String password) {
    Proxy proxy = new Proxy(protocol, host, port, username, password);
    proxies.add(proxy);
  }

  /**
   * Add a proxy server through which to make requests
   */
  public void addProxy(String protocol, String host, int port, String username, String password,
                       int socketTimeout, int connectTimeout, int connectionRequestTimeout)
  {
    Proxy proxy = new Proxy(protocol, host, port, username, password, socketTimeout, connectTimeout,
        connectionRequestTimeout);
    proxies.add(proxy);
  }

  public static final void main(final String[] args) throws IOException {
    if (args.length != 1) {
      System.err.println("Usage: OssIndexHttpClient '{\"coordinates\": [\"pkg:maven/poi/poi@1.2.3\"]}'");
      return;
    }
    OssIndexHttpClient client = new OssIndexHttpClient();
    String results = client.performPostRequest("component-report", args[args.length - 1]);
    System.err.println(results);
  }

  /**
   * Simple POJO for proxy information.
   */
  class Proxy
  {
    private String protocol;

    private String host;

    private int port;

    private String username;

    private String password;

    private int proxySocketTimeout = PROXY_SOCKET_TIMEOUT;

    private int proxyConnectTimeout = PROXY_CONNECT_TIMEOUT;

    private int proxyConnectionRequestTimeout = PROXY_CONNECTION_REQUEST_TIMEOUT;

    public Proxy(String protocol, String host, int port, String username, String password) {
      this.protocol = protocol;
      this.host = host;
      this.port = port;
      this.username = username;
      this.password = password;
    }

    public int getProxyConnectionRequestTimeout() {
      return proxyConnectionRequestTimeout;
    }

    public int getProxyConnectTimeout() {
      return proxyConnectTimeout;
    }

    public int getProxySocketTimeout() {
      return proxySocketTimeout;
    }

    public Proxy(String protocol, String host, int port, String username, String password,
                 int socketTimeout, int connectTimeout, int connectionRequestTimeout)
    {
      this.protocol = protocol;
      this.host = host;
      this.port = port;
      this.username = username;
      this.password = password;
      proxySocketTimeout = socketTimeout;
      proxyConnectTimeout = connectTimeout;
      proxyConnectionRequestTimeout = connectionRequestTimeout;
    }

    public HttpHost getHttpHost() {
      return new HttpHost(host, port, protocol);
    }

  }
}
