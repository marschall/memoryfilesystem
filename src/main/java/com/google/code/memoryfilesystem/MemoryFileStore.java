package com.google.code.memoryfilesystem;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileStoreAttributeView;

class MemoryFileStore extends FileStore {

  private final String key;

  private final ClosedFileSystemChecker checker;

  MemoryFileStore(String key, ClosedFileSystemChecker checker) {
    this.key = key;
    this.checker = checker;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String name() {
    this.checker.check();
    return this.key;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String type() {
    this.checker.check();
    return MemoryFileSystemProvider.SCHEME;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isReadOnly() {
    this.checker.check();
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long getTotalSpace() throws IOException {
    this.checker.check();
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long getUsableSpace() throws IOException {
    this.checker.check();
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long getUnallocatedSpace() throws IOException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean supportsFileAttributeView(
      Class<? extends FileAttributeView> type) {
    this.checker.check();
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean supportsFileAttributeView(String name) {
    this.checker.check();
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <V extends FileStoreAttributeView> V getFileStoreAttributeView(Class<V> type) {
    this.checker.check();
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException();
  }

  /**
   * {@inheritDoc}
   */
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
