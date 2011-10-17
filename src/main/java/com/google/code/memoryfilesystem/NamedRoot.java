package com.google.code.memoryfilesystem;

import java.net.URI;
import java.net.URISyntaxException;

class NamedRoot extends Root {

  private final String name;

  NamedRoot(MemoryFileSystem fileSystem, String name) {
    super(fileSystem);
    this.name = name;
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public boolean startsWith(String other) {
    return other.equals(this.name);
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public boolean endsWith(String other) {
    return other.equals(this.name);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return this.name;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public URI toUri() {
    try {
      return new URI(MemoryFileSystemProvider.SCHEME, getMemoryFileSystem().getKey() + "://" + this.name, null);
    } catch (URISyntaxException e) {
      throw new AssertionError("could not create URI");
    }
  }

}
