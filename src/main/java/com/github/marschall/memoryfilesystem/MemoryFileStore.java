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
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException();
  }


  @Override
  public long getUsableSpace() throws IOException {
    this.checker.check();
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException();
  }


  @Override
  public long getUnallocatedSpace() throws IOException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException();
  }


  @Override
  public boolean supportsFileAttributeView(Class<? extends FileAttributeView> type) {
    this.checker.check();
    return type == BasicFileAttributeView.class;
  }


  @Override
  public boolean supportsFileAttributeView(String name) {
    this.checker.check();
    return MemoryFileSystemProperties.BASIC_FILE_ATTRIBUTE_VIEW_NAME.equals(name);
  }


  @Override
  public <V extends FileStoreAttributeView> V getFileStoreAttributeView(Class<V> type) {
    this.checker.check();
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException();
  }


  @Override
  public Object getAttribute(String attribute) throws IOException {
    this.checker.check();
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException();
  }

  String getKey() {
    return this.key;
  }

}
