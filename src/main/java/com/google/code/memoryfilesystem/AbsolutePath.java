package com.google.code.memoryfilesystem;

import java.io.IOException;
import java.net.URI;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchEvent.Modifier;
import java.util.Iterator;
import java.util.List;

final class AbsolutePath extends ElementPath {
  
  private final Path root;

  AbsolutePath(MemoryFileSystem fileSystem, Path root, List<String> nameElements) {
    super(fileSystem, nameElements);
    this.root = root;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isAbsolute() {
    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Path getRoot() {
    return this.root;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Path getParent() {
    if (this.getNameElements().size() == 1) {
      return this.root;
    } else {
      List<String> subList = this.getNameElements().subList(0, this.getNameElements().size() - 1);
      return new AbsolutePath(getMemoryFileSystem(), this.root, subList);
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
    return new AbsolutePath(getMemoryFileSystem(), this.root, subList);
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
    return this;
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
    // TODO report bug
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
    if (!(obj instanceof AbsolutePath)) {
      return false;
    }
    AbsolutePath other = (AbsolutePath) obj;
    return this.root.equals(other.root)
            && this.getNameElements().equals(other.getNameElements());
  }
  
  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    return this.root.hashCode() ^ this.getNameElements().hashCode();
  }

}
