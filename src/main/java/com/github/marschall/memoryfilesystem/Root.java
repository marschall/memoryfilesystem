package com.github.marschall.memoryfilesystem;

import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Iterator;

abstract class Root extends AbstractPath {


  Root(MemoryFileSystem fileSystem) {
    super(fileSystem);
  }

  abstract boolean isNamed();


  @Override
  public boolean isAbsolute() {
    return true;
  }

  abstract String getKey();

  @Override
  boolean isRoot() {
    return true;
  }


  @Override
  public Path getRoot() {
    return this;
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
    throw new IllegalArgumentException("root does not have any name elements");
  }


  @Override
  public Path subpath(int beginIndex, int endIndex) {
    throw new IllegalArgumentException("can't create subpath of root");
  }

  @Override
  public Path normalize() {
    return this;
  }


  @Override
  public Path toAbsolutePath() {
    return this;
  }


  @Override
  public Path toRealPath(LinkOption... options) {
    return this;
  }


  @Override
  public Iterator<Path> iterator() {
    return Collections.emptyIterator();
  }


  @Override
  Path resolve(ElementPath other) {
    return AbstractPath.createAbsolute(this.getMemoryFileSystem(), this, other.getNameElements());
  }

  @Override
  Path resolveSibling(AbstractPath other) {
    return other;
  }

  @Override
  Path relativize(AbstractPath other) {
    if (!other.isAbsolute()) {
      // only support relativization against absolute paths
      throw new IllegalArgumentException("can only relativize an absolute path against an absolute path");
    }
    if (!other.getRoot().equals(this)) {
      // only support relativization against paths with same root
      throw new IllegalArgumentException("paths must have the same root");
    }
    if (other.equals(this)) {
      // other is me
      return createRelative(this.getMemoryFileSystem(), Collections.<String>emptyList());
    }

    if (other instanceof ElementPath) {
      // normal case
      ElementPath otherPath = (ElementPath) other;
      return createRelative(this.getMemoryFileSystem(), otherPath.getNameElements());
    } else {
      // unknown case
      throw new IllegalArgumentException("unsupported path argument");
    }
  }

}
