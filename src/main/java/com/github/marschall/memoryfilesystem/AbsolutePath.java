package com.github.marschall.memoryfilesystem;

import static java.lang.Math.min;

import java.io.IOException;
import java.net.URI;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchEvent.Modifier;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.List;

final class AbsolutePath extends ElementPath {
  
  private final Root root;

  AbsolutePath(MemoryFileSystem fileSystem, Root root, List<String> nameElements) {
    super(fileSystem, nameElements);
    this.root = root;
  }

  @Override
  public boolean isAbsolute() {
    return true;
  }

  @Override
  public Path getRoot() {
    return this.root;
  }
  
  @Override
  public String toString() {
    StringBuilder buffer = new StringBuilder();
    buffer.append(this.root);
    boolean first = true;
    String separator = this.getFileSystem().getSeparator();
    for (String element : this.getNameElements()) {
      if (!first) {
        buffer.append(separator);
      }
      buffer.append(element);
      first = false;
    }
    return buffer.toString();
  }

  @Override
  public Path getParent() {
    if (this.getNameElements().size() == 1) {
      return this.root;
    } else {
      List<String> subList = this.getNameElements().subList(0, this.getNameElements().size() - 1);
      return new AbsolutePath(getMemoryFileSystem(), this.root, subList);
    }
  }


  @Override
  public Path getName(int index) {
    if (index < 0) {
      throw new IllegalArgumentException("index must be positive but was " + index);
    }
    if (index >= this.getNameCount()) {
      throw new IllegalArgumentException("index must not be bigger than " + (this.getNameCount() - 1) +  " but was " + index);
    }
    List<String> subList = this.getNameElements().subList(0, index + 1);
    return new AbsolutePath(getMemoryFileSystem(), this.root, subList);
  }


  @Override
  public Path subpath(int beginIndex, int endIndex) {
    // TODO Auto-generated function stub
    throw new UnsupportedOperationException();
  }


  @Override
  public Path normalize() {
    // TODO Auto-generated function stub
    throw new UnsupportedOperationException();
  }


  @Override
  public Path resolveSibling(String other) {
    // TODO Auto-generated function stub
    throw new UnsupportedOperationException();
  }


  @Override
  public URI toUri() {
    // TODO Auto-generated function stub
    throw new UnsupportedOperationException();
  }


  @Override
  public Path toAbsolutePath() {
    return this;
  }


  @Override
  public Path toRealPath(LinkOption... options) throws IOException {
    // TODO Auto-generated function stub
    throw new UnsupportedOperationException();
  }


  @Override
  public WatchKey register(WatchService watcher, Kind<?>[] events,
          Modifier... modifiers) throws IOException {
    // TODO report bug
    // TODO Auto-generated function stub
    throw new UnsupportedOperationException();
  }


  @Override
  public WatchKey register(WatchService watcher, Kind<?>... events) throws IOException {
    // TODO Auto-generated function stub
    throw new UnsupportedOperationException();
  }


  @Override
  public int compareTo(Path other) {
    // TODO Auto-generated function stub
    throw new UnsupportedOperationException();
  }


  @Override
  boolean startsWith(AbstractPath other) {
    // TODO Auto-generated function stub
    throw new UnsupportedOperationException();
  }


  @Override
  boolean endsWith(AbstractPath other) {
    // TODO Auto-generated function stub
    throw new UnsupportedOperationException();
  }


  @Override
  Path resolve(AbstractPath other) {
    if (other instanceof ElementPath) {
      ElementPath otherPath = (ElementPath) other;
      List<String> resolvedElements = new ArrayList<>(this.getNameCount() + otherPath.getNameCount());
      resolvedElements.addAll(this.getNameElements());
      resolvedElements.addAll(otherPath.getNameElements());
      return new AbsolutePath(this.getMemoryFileSystem(), this.root, resolvedElements);
    } else {
      throw new IllegalArgumentException("can't resolve" + other);
    }
  }


  @Override
  Path resolveSibling(AbstractPath other) {
    // TODO Auto-generated function stub
    throw new UnsupportedOperationException();
  }


  @Override
  Path relativize(AbstractPath other) {
    if (!other.isAbsolute()) {
      // only support relativization against absolute paths
      throw new IllegalArgumentException("can only relativize an absolute path against an absolute path");
    }
    if (!other.getRoot().equals(this.root)) {
      // only support relativization against paths with same root
      throw new IllegalArgumentException("paths must have the same root");
    }
    //TODO normalize
    if (other.equals(this.root)) {
      // other is my root, easy 
      return new RelativePath(this.getMemoryFileSystem(), new HomogenousList<String>("..", this.getNameCount()));
    } else if (other instanceof ElementPath) {
      // normal case
      ElementPath otherPath = (ElementPath) other;
      List<String> relativeElements = new ArrayList<>();
      int firstDifferenceIndex = firstDifferenceIndex(this.getNameElements(), otherPath.getNameElements());
      for (int i = firstDifferenceIndex; i < this.getNameCount(); ++i) {
        relativeElements.add("..");
      }
      if (firstDifferenceIndex < other.getNameCount()) {
        relativeElements.addAll(otherPath.getNameElements().subList(firstDifferenceIndex, otherPath.getNameCount()));
      }
      return new RelativePath(this.getMemoryFileSystem(), relativeElements);
    } else {
      // unknown case
      throw new IllegalArgumentException("unsupported path argument");
    }
  }
  

  private int firstDifferenceIndex(List<?> l1, List<?> l2) {
    int endIndex = min(l1.size(), l2.size());
    for (int i = 0; i < endIndex; ++i) {
      if (!l1.get(i).equals(l2.get(i))) {
        return i;
      }
    }
    return endIndex;
  }
  
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof AbsolutePath)) {
      return false;
    }
    AbsolutePath other = (AbsolutePath) obj;
    return this.root.equals(other.root)
            && this.getNameElements().equals(other.getNameElements());
  }
  

  @Override
  public int hashCode() {
    // TODO disturb context
    return this.root.hashCode() ^ this.getNameElements().hashCode();
  }

}
