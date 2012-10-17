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
    return "".equals(other);
  }

  @Override
  public boolean endsWith(String other) {
    return "".equals(other);
  }
  

  @Override
  boolean startsWith(AbstractPath other) {
    return other == this;
  }

  @Override
  boolean endsWith(AbstractPath other) {
    return other == this;
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
    return this.getMemoryFileSystem().getDefaultPath();
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
    return Collections.emptyIterator();
  }
  
  @Override
  int compareToNonRoot(AbstractPath other) {
    return -1;
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
  Path resolve(AbstractPath other) {
    return other;
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
      return other;
    } else {
      // unknown case
      throw new IllegalArgumentException("unsupported path argument");
    }
  }
  
  // since this is a singleton per file system there is no need to override
  // #equals and #hashCode
  
  @Override
  public String toString() {
    return "";
  }
  

}
