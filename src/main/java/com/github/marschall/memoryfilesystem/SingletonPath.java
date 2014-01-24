package com.github.marschall.memoryfilesystem;

import java.io.IOException;
import java.net.URI;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.text.CollationKey;
import java.text.Collator;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

final class SingletonPath extends ElementPath {

  private final String fileName;

  SingletonPath(MemoryFileSystem fileSystem, String fileName) {
    super(fileSystem);
    this.fileName = fileName;
  }

  @Override
  public boolean isAbsolute() {
    return false;
  }

  @Override
  public Path getRoot() {
    return null;
  }

  @Override
  public Path getFileName() {
    return this;
  }

  @Override
  public Path getParent() {
    return null;
  }

  @Override
  public int getNameCount() {
    return 1;
  }

  @Override
  public Path getName(int index) {
    if (index != 0) {
      throw new IllegalArgumentException("index must be 0 but was " + index);
    }
    return this;
  }

  @Override
  public Path subpath(int beginIndex, int endIndex) {
    if (beginIndex != 0) {
      throw new IllegalArgumentException("begin index must be 1");
    }
    if (endIndex != 1) {
      throw new IllegalArgumentException("end index must be 1");
    }
    return this;
  }

  @Override
  public boolean startsWith(String other) {
    Path path = this.getMemoryFileSystem().getPath(other);
    return this.isEqualPath((AbstractPath) path);
  }

  @Override
  public boolean endsWith(String other) {
    Path path = this.getMemoryFileSystem().getPath(other);
    return this.isEqualPath((AbstractPath) path);
  }

  @Override
  public Path normalize() {
    return this;
  }

  @Override
  public URI toUri() {
    return this.toAbsolutePath().toUri();
  }

  @Override
  public Path toAbsolutePath() {
    return this.getMemoryFileSystem().getDefaultPath().resolve(this);
  }

  @Override
  public Path toRealPath(LinkOption... options) throws IOException {
    return this.getMemoryFileSystem().toRealPath(this, options);
  }

  @Override
  public Iterator<Path> iterator() {
    return Collections.<Path>singleton(this).iterator();
  }

  @Override
  List<String> getNameElements() {
    return Collections.singletonList(this.fileName);
  }

  @Override
  String getNameElement(int index) {
    if (index != 0) {
      throw new IllegalArgumentException("index must be 0 but was " + index);
    }
    return this.fileName;
  }

  @Override
  String getLastNameElement() {
    return this.fileName;
  }

  @Override
  int compareToNonRoot(ElementPath other) {
    if (other.isAbsolute()) {
      return 1;
    }
    if (other.getNameCount() == 0) {
      return 1;
    }
    Collator collator = this.getMemoryFileSystem().getCollator();
    int comparison = collator.compare(this.fileName, other.getNameElement(0));
    if (comparison != 0) {
      return comparison;
    }
    if (other.getNameCount() > 1) {
      return -1;
    } else {
      return 0;
    }
  }

  @Override
  boolean startsWith(AbstractPath other) {
    return this.isEqualPath(other);
  }

  @Override
  boolean endsWith(AbstractPath other) {
    return this.isEqualPath(other);
  }

  private boolean isEqualPath(AbstractPath other) {
    if (other.isAbsolute()) {
      return false;
    }
    if (other.isRoot()) {
      return false;
    }
    if (other instanceof ElementPath) {
      ElementPath otherPath = (ElementPath) other;
      if (otherPath.getNameCount() != 1) {
        return false;
      }
      Collator collator = this.getMemoryFileSystem().getCollator();
      return collator.equals(this.fileName, otherPath.getLastNameElement());
    } else {
      throw new IllegalArgumentException("can't check for #startsWith against " + other);
    }
  }

  @Override
  Path resolve(ElementPath other) {
    List<String> newNameElement = CompositeList.create(this.getNameElements(), other.getNameElements());
    return AbstractPath.createRelative(this.getMemoryFileSystem(), newNameElement);
  }

  @Override
  Path resolveSibling(AbstractPath other) {
    return other;
  }

  @Override
  boolean isRoot() {
    return false;
  }

  @Override
  Path relativize(AbstractPath other) {
    if (other.isAbsolute()) {
      // only support relativization against relative paths
      throw new IllegalArgumentException("can only relativize a relative path against a relative path");
    }
    if (other instanceof ElementPath) {
      // normal case
      ElementPath otherPath = (ElementPath) other;
      if (otherPath.startsWith(this)) {
        if (otherPath.getNameCount() == 1) {
          return this.getMemoryFileSystem().getEmptyPath();
        } else {
          return other.subpath(1, otherPath.getNameCount());
        }
      } else {
        List<String> newNameElement = CompositeList.create(Collections.singletonList(".."), otherPath.getNameElements());
        return AbstractPath.createRelative(this.getMemoryFileSystem(), newNameElement);
      }
    } else {
      // unknown case
      throw new IllegalArgumentException("unsupported path argument");
    }
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof SingletonPath)) {
      return false;
    }
    SingletonPath other = (SingletonPath) obj;
    // compareTo take memory file system key into account to ensure
    // a.compareTo(b) == 0 iff a.equals(b)
    MemoryFileSystem memoryFileSystem = this.getMemoryFileSystem();
    return memoryFileSystem.equals(other.getMemoryFileSystem())
            && memoryFileSystem.getCollator().equals(this.fileName, other.fileName);
  }

  @Override
  public int hashCode() {
    MemoryFileSystem memoryFileSystem = this.getMemoryFileSystem();
    Collator collator = memoryFileSystem.getCollator();
    // TODO expensive, safe
    CollationKey collationKey = collator.getCollationKey(this.fileName);

    int result = 17;
    result = 31 * result + memoryFileSystem.hashCode();
    result = 31 * result + Arrays.hashCode(collationKey.toByteArray());
    return result;
  }

  @Override
  public String toString() {
    return this.fileName;
  }

}
