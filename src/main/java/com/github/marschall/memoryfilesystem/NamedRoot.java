package com.github.marschall.memoryfilesystem;

import static com.github.marschall.memoryfilesystem.MemoryFileSystemProvider.SCHEME;

import java.net.URI;
import java.net.URISyntaxException;

class NamedRoot extends Root {

  private final char letter;
  private final String stringValue;

  NamedRoot(MemoryFileSystem fileSystem, String name) {
    super(fileSystem);
    this.letter = name.charAt(0);
    this.stringValue = Character.toString(this.letter) + ':' + fileSystem.getSeparator();
  }
  
  @Override
  String getKey() {
    return Character.toString(letter);
  }
  
  @Override
  boolean isNamed() {
    return true;
  }
  
  @Override
  public boolean startsWith(String other) {
    return this.samePathAs(other);
  }

  @Override
  public boolean endsWith(String other) {
    return this.samePathAs(other);
  }
  
  private boolean samePathAs(String other) {
    if (other.length() != 3) {
      return false;
    }
    if (other.charAt(1) != ':') {
      return false;
    }
    char otherLast = other.charAt(2);
    if (otherLast != '/' && otherLast != getMemoryFileSystem().getSeparator().charAt(0)) {
      return false;
    }
    StringTransformer pathTransformer = this.getMemoryFileSystem().getPathTransformer();
    return pathTransformer.transform(Character.toString(this.letter))
            .equals(pathTransformer.transform(Character.toString(other.charAt(0))));
  }

  @Override
  public String toString() {
    return this.stringValue;
  }

  @Override
  public URI toUri() {
    try {
      return new URI(SCHEME, getMemoryFileSystem().getKey() + "://" + this.letter + ':', null);
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
            && this.letter == other.letter;
  }
  
  @Override
  public int hashCode() {
    // TODO pertube context
    // FIXME violates contract
    return this.getFileSystem().hashCode() ^ this.letter;
  }

}
