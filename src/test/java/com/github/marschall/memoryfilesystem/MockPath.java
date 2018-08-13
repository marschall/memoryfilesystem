package com.github.marschall.memoryfilesystem;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchEvent.Modifier;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Iterator;

final class MockPath implements Path {

  @Override
  public FileSystem getFileSystem() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isAbsolute() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Path getRoot() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Path getFileName() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Path getParent() {
    throw new UnsupportedOperationException();
  }

  @Override
  public int getNameCount() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Path getName(int index) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Path subpath(int beginIndex, int endIndex) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean startsWith(Path other) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean startsWith(String other) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean endsWith(Path other) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean endsWith(String other) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Path normalize() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Path resolve(Path other) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Path resolve(String other) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Path resolveSibling(Path other) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Path resolveSibling(String other) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Path relativize(Path other) {
    throw new UnsupportedOperationException();
  }

  @Override
  public URI toUri() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Path toAbsolutePath() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Path toRealPath(LinkOption... options) {
    throw new UnsupportedOperationException();
  }

  @Override
  public File toFile() {
    throw new UnsupportedOperationException();
  }

  @Override
  public WatchKey register(WatchService watcher, Kind<?>[] events, Modifier... modifiers) {
    throw new UnsupportedOperationException();
  }

  @Override
  public WatchKey register(WatchService watcher, Kind<?>... events) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Iterator<Path> iterator() {
    throw new UnsupportedOperationException();
  }

  @Override
  public int compareTo(Path other) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int hashCode() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean equals(Object obj) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String toString() {
    return "a mock path";
  }

}
