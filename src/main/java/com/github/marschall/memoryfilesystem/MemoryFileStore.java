package com.github.marschall.memoryfilesystem;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileStoreAttributeView;

class MemoryFileStore extends FileStore {

  private final String key;

  private final ClosedFileSystemChecker checker;

  MemoryFileStore(String key, ClosedFileSystemChecker checker) {
    this.key = key;
    this.checker = checker;
  }


  @Override
  public String name() {
    this.checker.check();
    return this.key;
  }


  @Override
  public String type() {
    this.checker.check();
    return MemoryFileSystemProvider.SCHEME;
  }


  @Override
  public boolean isReadOnly() {
    this.checker.check();
    return false;
  }


  @Override
  public long getTotalSpace() throws IOException {
    this.checker.check();
    return Runtime.getRuntime().maxMemory();
  }


  @Override
  public long getUsableSpace() throws IOException {
    this.checker.check();
    return Runtime.getRuntime().freeMemory();
  }


  @Override
  public long getUnallocatedSpace() throws IOException {
    this.checker.check();
    return Runtime.getRuntime().freeMemory();
  }


  @Override
  public boolean supportsFileAttributeView(Class<? extends FileAttributeView> type) {
    this.checker.check();
    return type == BasicFileAttributeView.class;
  }


  @Override
  public boolean supportsFileAttributeView(String name) {
    this.checker.check();
    return FileAttributeViews.BASIC.equals(name);
  }


  @Override
  public <V extends FileStoreAttributeView> V getFileStoreAttributeView(Class<V> type) {
    this.checker.check();
    return null;
  }


  @Override
  public Object getAttribute(String attribute) throws IOException {
    this.checker.check();
    throw new UnsupportedOperationException();
  }

  String getKey() {
    return this.key;
  }

}
