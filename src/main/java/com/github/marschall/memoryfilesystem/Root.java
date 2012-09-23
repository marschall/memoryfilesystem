package com.github.marschall.memoryfilesystem;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchEvent.Modifier;
import java.nio.file.LinkOption;
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
  public Path resolve(String other) {
    // TODO Auto-generated method stub
    return null;
  }



  @Override
  public Path resolveSibling(String other) {
    // TODO Auto-generated method stub
    return null;
  }


  @Override
  public Path toAbsolutePath() {
    return this;
  }


  @Override
  public Path toRealPath(LinkOption... options) throws IOException {
    // TODO Auto-generated function stub
    return null;
  }


  @Override
  public WatchKey register(WatchService watcher, Kind<?>[] events,
      Modifier... modifiers) throws IOException {
    // TODO Auto-generated method stub
    return null;
  }


  @Override
  public WatchKey register(WatchService watcher, Kind<?>... events)
      throws IOException {
    // TODO Auto-generated method stub
    return null;
  }


  @Override
  public Iterator<Path> iterator() {
    return Collections.emptyIterator();
  }


  @Override
  public int compareTo(Path other) {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  boolean startsWith(AbstractPath other) {
    return this == other;
  }

  @Override
  boolean endsWith(AbstractPath other) {
    return this == other;
  }

  @Override
  Path resolve(AbstractPath other) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  Path resolveSibling(AbstractPath other) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  Path relativize(AbstractPath other) {
    // TODO Auto-generated method stub
    return null;
  }

}
