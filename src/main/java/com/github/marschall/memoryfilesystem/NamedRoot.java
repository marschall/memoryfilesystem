package com.github.marschall.memoryfilesystem;

import static com.github.marschall.memoryfilesystem.MemoryFileSystemProvider.SCHEME;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.CollationKey;
import java.text.Collator;
import java.util.Arrays;

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
  

  @Override
  boolean startsWith(AbstractPath other) {
    return this.samePathAs(other);
  }

  @Override
  boolean endsWith(AbstractPath other) {
    return this.samePathAs(other);
  }
  

  private boolean samePathAs(AbstractPath other) {
    if (!(other instanceof NamedRoot)) {
      return false;
    }
    NamedRoot otherRoot = (NamedRoot) other;
    //TODO safe collator key
    Collator collator = this.getMemoryFileSystem().getCollator();
    return collator.equals(Character.toString(this.letter), Character.toString(otherRoot.letter));
    
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
    //TODO safe collator key
    Collator collator = this.getMemoryFileSystem().getCollator();
    return collator.equals(Character.toString(this.letter), Character.toString(other.charAt(0)));
  }

  @Override
  public String toString() {
    return this.stringValue;
  }

  @Override
  public URI toUri() {
    try {
      return new URI(SCHEME, this.getMemoryFileSystem().getKey() + "://" + this.letter + ':', null);
    } catch (URISyntaxException e) {
      throw new AssertionError("could not create URI");
    }
  }
  
  @Override
  int compareTo(AbstractPath other) {
    if (!other.isRoot()) {
      return -1;
    }
    // method is only invoked on methods of the same file system
    NamedRoot otherRoot = (NamedRoot) other;
    //TODO safe collator key
    Collator collator = this.getMemoryFileSystem().getCollator();
    return collator.compare(Character.toString(this.letter), Character.toString(otherRoot.letter));
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
    if (!this.getFileSystem().equals(other.getFileSystem())) {
      return false;
    }
    //TODO safe collator key
    Collator collator = this.getMemoryFileSystem().getCollator();
    return collator.equals(Character.toString(this.letter), Character.toString(other.letter));
  }
  
  @Override
  public int hashCode() {
    // TODO safe expensive
    MemoryFileSystem memoryFileSystem = this.getMemoryFileSystem();
    Collator collator = memoryFileSystem.getCollator();
    int result = 17;
    result = 31 * result + memoryFileSystem.hashCode();
    CollationKey collationKey = collator.getCollationKey(Character.toString(this.letter));
    result = 31 * result + Arrays.hashCode(collationKey.toByteArray());
    return result;
  }

}
