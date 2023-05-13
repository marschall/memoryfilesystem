package com.github.marschall.memoryfilesystem.memory;

import com.github.marschall.memoryfilesystem.MemoryFileSystemProvider;

import java.net.InetAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * {@link URLStreamHandler} that can resolve memory URLs.
 * <p>
 * This class will be instantiated by the JDK.
 * <p>
 * Users need to add @ {@code com.github.marschall.memoryfilesystem} to the
 * {@code java.protocol.handler.pkgs} system property (comma separated).
 * <pre><code>
 * -Djava.protocol.handler.pkgs=com.github.marschall.memoryfilesystem
 * </code></pre>
 *
 */
public class Handler extends URLStreamHandler {

  /**
   * Default constructor to be called by JDK classes.
   */
  public Handler() {
    super();
  }

  @Override
  protected URLConnection openConnection(URL url) {
    if (url == null) {
      throw new IllegalArgumentException("url was null");
    }

    String protocol = url.getProtocol();

    if (!MemoryFileSystemProvider.SCHEME.equals(protocol)) {
      throw new UnsupportedOperationException("Cannot use protocol '"
          + protocol + "' for this implementation");
    }

    return new MemoryURLConnection(url);
  }

  @Override
  protected URLConnection openConnection(URL url, Proxy proxy) {
    // we do not support proxies, therefore by API contract we should ignore proxies
    if (proxy == null) {
      throw new IllegalArgumentException("proxy was null");
    }
    return this.openConnection(url);
  }

  @Override
  protected boolean hostsEqual(URL u1, URL u2) {
    // only called when both have the same protocol
    // memory URLs have not hosts
    return true;
  }

  @Override
  protected InetAddress getHostAddress(URL u) {
    // we have not host
    return null;
  }

}
