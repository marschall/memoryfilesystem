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

final class AbsolutePath extends AbstractPath {
  
  private final List<String> nameElements;

  AbsolutePath(MemoryFileSystem fileSystem, List<String> nameElements) {
    super(fileSystem);
    this.nameElements = nameElements;
  }

  @Override
  public boolean isAbsolute() {
    return true;
  }

  @Override
  public Path getRoot() {
    // TODO Auto-generated function stub
    return null;
  }

  @Override
  public Path getFileName() {
    // TODO Auto-generated function stub
    return null;
  }

  @Override
  public Path getParent() {
    // TODO Auto-generated function stub
    return null;
  }

  @Override
  public int getNameCount() {
    // TODO Auto-generated function stub
    return 0;
  }

  @Override
  public Path getName(int index) {
    // TODO Auto-generated function stub
    return null;
  }

  @Override
  public Path subpath(int beginIndex, int endIndex) {
    // TODO Auto-generated function stub
    return null;
  }

  @Override
  public boolean startsWith(String other) {
    // TODO Auto-generated function stub
    return false;
  }

  @Override
  public boolean endsWith(String other) {
    // TODO Auto-generated function stub
    return false;
  }

  @Override
  public Path normalize() {
    // TODO Auto-generated function stub
    return null;
  }

  @Override
  public Path resolve(String other) {
    // TODO Auto-generated function stub
    return null;
  }

  @Override
  public Path resolveSibling(String other) {
    // TODO Auto-generated function stub
    return null;
  }

  @Override
  public URI toUri() {
    // TODO Auto-generated function stub
    return null;
  }

  @Override
  public Path toAbsolutePath() {
    // TODO Auto-generated function stub
    return null;
  }

  @Override
  public Path toRealPath(LinkOption... options) throws IOException {
    // TODO Auto-generated function stub
    return null;
  }

  @Override
  public WatchKey register(WatchService watcher, Kind<?>[] events,
          Modifier... modifiers) throws IOException {
    // TODO Auto-generated function stub
    return null;
  }

  @Override
  public WatchKey register(WatchService watcher, Kind<?>... events)
          throws IOException {
    // TODO Auto-generated function stub
    return null;
  }

  @Override
  public Iterator<Path> iterator() {
    // TODO Auto-generated function stub
    return null;
  }

  @Override
  public int compareTo(Path other) {
    // TODO Auto-generated function stub
    return 0;
  }

  @Override
  boolean startsWith(AbstractPath other) {
    // TODO Auto-generated function stub
    return false;
  }

  @Override
  boolean endsWith(AbstractPath other) {
    // TODO Auto-generated function stub
    return false;
  }

  @Override
  Path resolve(AbstractPath other) {
    // TODO Auto-generated function stub
    return null;
  }

  @Override
  Path resolveSibling(AbstractPath other) {
    // TODO Auto-generated function stub
    return null;
  }

  @Override
  Path relativize(AbstractPath other) {
    // TODO Auto-generated function stub
    return null;
  }

}
