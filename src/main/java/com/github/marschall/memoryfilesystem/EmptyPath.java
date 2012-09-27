package com.github.marschall.memoryfilesystem;

import java.io.IOException;
import java.net.URI;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchEvent.Modifier;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;


final class EmptyPath extends ElementPath {

  EmptyPath(MemoryFileSystem memoryFileSystem) {
    super(memoryFileSystem);
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
    return null;
  }

  @Override
  public Path getParent() {
    return null;
  }

  @Override
  public int getNameCount() {
    return 0;
  }

  @Override
  public Path getName(int index) {
    throw new IllegalArgumentException("empty path does not support #getName(int)");
  }

  @Override
  public Path subpath(int beginIndex, int endIndex) {
    throw new IllegalArgumentException("can't create a subpath on an empty path");
  }

  @Override
  public boolean startsWith(String other) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean endsWith(String other) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException();
  }

  @Override
  public Path normalize() {
    return this;
  }

  @Override
  public URI toUri() {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException();
  }

  @Override
  public Path toAbsolutePath() {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException();
  }

  @Override
  public Path toRealPath(LinkOption... options) throws IOException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException();
  }

  @Override
  public WatchKey register(WatchService watcher, Kind<?>[] events,
          Modifier... modifiers) throws IOException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException();
  }

  @Override
  public WatchKey register(WatchService watcher, Kind<?>... events) throws IOException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException();
  }

  @Override
  public Iterator<Path> iterator() {
    return Collections.<Path>emptyList().iterator();
  }

  @Override
  public int compareTo(Path other) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException();
  }

  @Override
  List<String> getNameElements() {
    return Collections.emptyList();
  }

  @Override
  String getNameElement(int index) {
    throw new IndexOutOfBoundsException("empty path does not support #getNameElement(int)");
  }

  @Override
  String getLastNameElement() {
    throw new IndexOutOfBoundsException("empty path does not support #getLastNameElement()");
  }

  @Override
  boolean startsWith(AbstractPath other) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException();
  }

  @Override
  boolean endsWith(AbstractPath other) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException();
  }

  @Override
  Path resolve(AbstractPath other) {
    return other;
  }

  @Override
  Path resolveSibling(AbstractPath other) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException();
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
      return other;
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
    if (!(obj instanceof EmptyPath)) {
      return false;
    }
    EmptyPath other = (EmptyPath) obj;
    return this.getFileSystem().equals(other.getFileSystem());
  }
  
  @Override
  public int hashCode() {
    return this.getFileSystem().hashCode();
  }
  
  @Override
  public String toString() {
    return "";
  }
  

}
