package com.google.code.memoryfilesystem;

import java.io.IOException;
import java.net.URI;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchEvent.Modifier;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Iterator;
import java.util.List;

final class RelativePath extends ElementPath {
  

  RelativePath(MemoryFileSystem fileSystem, List<String> nameElements) {
    super(fileSystem, nameElements);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isAbsolute() {
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Path getRoot() {
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Path getParent() {
    if (this.getNameElements().size() == 1) {
      return null;
    } else {
      List<String> subList = this.getNameElements().subList(0, this.getNameElements().size() - 1);
      return new RelativePath(getMemoryFileSystem(), subList);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Path getName(int index) {
    if (index < 0) {
      throw new IllegalArgumentException("index must be positive but was " + index);
    }
    if (index >= this.getNameCount()) {
      throw new IllegalArgumentException("index must not be bigger than " + (this.getNameCount() - 1) +  " but was " + index);
    }
    List<String> subList = this.getNameElements().subList(0, index + 1);
    return new RelativePath(getMemoryFileSystem(), subList);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Path subpath(int beginIndex, int endIndex) {
    // TODO Auto-generated function stub
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Path normalize() {
    // TODO Auto-generated function stub
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Path resolve(String other) {
    // TODO Auto-generated function stub
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Path resolveSibling(String other) {
    // TODO Auto-generated function stub
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public URI toUri() {
    // TODO Auto-generated function stub
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Path toAbsolutePath() {
    // TODO Auto-generated function stub
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Path toRealPath(LinkOption... options) throws IOException {
    // TODO Auto-generated function stub
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public WatchKey register(WatchService watcher, Kind<?>[] events,
          Modifier... modifiers) throws IOException {
    // TODO Auto-generated function stub
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public WatchKey register(WatchService watcher, Kind<?>... events)
          throws IOException {
    // TODO Auto-generated function stub
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Iterator<Path> iterator() {
    // TODO Auto-generated function stub
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int compareTo(Path other) {
    // TODO Auto-generated function stub
    return 0;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  boolean startsWith(AbstractPath other) {
    // TODO Auto-generated function stub
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  boolean endsWith(AbstractPath other) {
    // TODO Auto-generated function stub
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  Path resolve(AbstractPath other) {
    // TODO Auto-generated function stub
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  Path resolveSibling(AbstractPath other) {
    // TODO Auto-generated function stub
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  Path relativize(AbstractPath other) {
    // TODO Auto-generated function stub
    return null;
  }
  
  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof RelativePath)) {
      return false;
    }
    ElementPath other = (ElementPath) obj;
    return this.getNameElements().equals(other.getNameElements());
  }
  
  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    return this.getNameElements().hashCode();
  }

}
