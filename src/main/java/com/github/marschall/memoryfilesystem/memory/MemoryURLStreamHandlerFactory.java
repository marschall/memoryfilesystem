package com.github.marschall.memoryfilesystem.memory;

import java.net.URL;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

/**
 * {@link URLStreamHandlerFactory} that creates {@link URLStreamHandler} that can resolve
 * memory {@link URL}.
 *
 * @see java.net.URL#setURLStreamHandlerFactory(URLStreamHandlerFactory)
 */
public final class MemoryURLStreamHandlerFactory implements URLStreamHandlerFactory {

  public MemoryURLStreamHandlerFactory() {
    super();
  }

  @Override
  public URLStreamHandler createURLStreamHandler(String protocol) {
    return new Handler();
  }

}
