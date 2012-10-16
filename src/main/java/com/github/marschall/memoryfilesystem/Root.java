package com.github.marschall.memoryfilesystem;

import java.io.IOException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchEvent.Modifier;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
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
  public Path toRealPath(LinkOption... options) throws IOException {
    // TODO Auto-generated function stub
    throw new UnsupportedOperationException();
  }


  @Override
  public WatchKey register(WatchService watcher, Kind<?>[] events,
      Modifier... modifiers) throws IOException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException();
  }


  @Override
  public WatchKey register(WatchService watcher, Kind<?>... events)
      throws IOException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException();
  }


  @Override
  public Iterator<Path> iterator() {
    return Collections.emptyIterator();
  }


  @Override
  int compareTo(AbstractPath other) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException();
  }


  @Override
  Path resolve(AbstractPath other) {
    if (other instanceof ElementPath) {
      // TODO root?
      ElementPath otherPath = (ElementPath) other;
      return AbstractPath.createAboslute(this.getMemoryFileSystem(), this, otherPath.getNameElements());
    } else {
      throw new IllegalArgumentException("can't resolve" + other);
    }
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
      return createRelative(getMemoryFileSystem(), Collections.<String>emptyList());
    }
    
    //TODO normalize
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
