package com.github.marschall.memoryfilesystem;

import static com.github.marschall.memoryfilesystem.MemoryFileSystemProvider.SCHEME;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;

class EmptyRoot extends Root {

  static final String SLASH = "/";

  EmptyRoot(MemoryFileSystem fileSystem) {
    super(fileSystem);
  }
  
  @Override
  boolean isNamed() {
    return false;
  }

  @Override
  public boolean startsWith(String other) {
    // intentionally trigger NPE if other is null (default file system behaves the same way)
    return other.equals(SLASH);
  }


  @Override
  public boolean endsWith(String other) {
    // intentionally trigger NPE if other is null (default file system behaves the same way)
    return other.equals(SLASH);
  }
  

  @Override
  boolean startsWith(AbstractPath other) {
    return this == other;
  }

  @Override
  boolean endsWith(AbstractPath other) {
    return this == other;
  }
  
  @Override
  String getKey() {
    return SLASH;
  }

  @Override
  public String toString() {
    return SLASH;
  }

  @Override
  public URI toUri() {
    try {
      return new URI(SCHEME, getMemoryFileSystem().getKey() + ":///", null);
    } catch (URISyntaxException e) {
      throw new AssertionError("could not create URI");
    }
  }
  
  @Override
  int compareTo(AbstractPath other) {
    return other == this ? 0 : -1;
  }
  
  // no need to define equals and hashCode since
  // identity is fine since there is only one empty root per file system

}
