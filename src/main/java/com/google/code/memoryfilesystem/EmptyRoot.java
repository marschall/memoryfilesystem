package com.google.code.memoryfilesystem;

import java.net.URI;
import java.net.URISyntaxException;
import static com.google.code.memoryfilesystem.MemoryFileSystemProvider.SCHEME;

class EmptyRoot extends Root {

  static final String SLASH = "/";

  EmptyRoot(MemoryFileSystem fileSystem) {
    super(fileSystem);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  boolean isNamed() {
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean startsWith(String other) {
    // intentionally trigger NPE if other is null (default file system behaves the same way)
    return other.equals(SLASH);
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public boolean endsWith(String other) {
    // intentionally trigger NPE if other is null (default file system behaves the same way)
    return other.equals(SLASH);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return SLASH;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public URI toUri() {
    try {
      return new URI(SCHEME, getMemoryFileSystem().getKey() + ":///", null);
    } catch (URISyntaxException e) {
      throw new AssertionError("could not create URI");
    }
  }

}
