package com.github.marschall.memoryfilesystem;

import static com.github.marschall.memoryfilesystem.MemoryFileSystemProvider.SCHEME;

import java.net.URI;
import java.net.URISyntaxException;

class NamedRoot extends Root {

  private final String name;

  NamedRoot(MemoryFileSystem fileSystem, String name) {
    super(fileSystem);
    this.name = name;
  }
  
  @Override
  boolean isNamed() {
    return true;
  }

  @Override
  public boolean startsWith(String other) {
    return other.equals(this.name);
  }

  @Override
  public boolean endsWith(String other) {
    return other.equals(this.name);
  }

  @Override
  public String toString() {
    return this.name;
  }

  @Override
  public URI toUri() {
    try {
      return new URI(SCHEME, getMemoryFileSystem().getKey() + "://" + this.name, null);
    } catch (URISyntaxException e) {
      throw new AssertionError("could not create URI");
    }
  }
  
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof NamedRoot)) {
      return false;
    }
    NamedRoot other = (NamedRoot) obj;
    return this.getFileSystem().equals(other.getFileSystem())
            && this.name.equals(other.name);
  }
  
  @Override
  public int hashCode() {
    // TODO pertube context
    return this.getFileSystem().hashCode() ^ this.name.hashCode();
  }

}
