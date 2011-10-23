package com.google.code.memoryfilesystem;

import java.io.IOException;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

class MemoryDirectory extends MemoryEntry {

  private final ConcurrentMap<String, MemoryEntry> entries;
  
  private final BasicFileAttributes attributes;
  
  MemoryDirectory() {
    this.entries = new ConcurrentHashMap<>();
    this.attributes = new MemoryDirectoryFileAttributes();
  }
  
  class MemoryDirectoryFileAttributesView extends MemoryEntryFileAttributesView {

    /**
     * {@inheritDoc}
     */
    @Override
    public BasicFileAttributes readAttributes() throws IOException {
      return MemoryDirectory.this.attributes;
    }
    
  }
  
  class MemoryDirectoryFileAttributes extends MemoryEntryFileAttributes {

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isRegularFile() {
      return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDirectory() {
      return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSymbolicLink() {
      return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isOther() {
      return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long size() {
      // REVIEW make configurable
      return -1L;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object fileKey() {
      // REVIEW think about it
      return MemoryDirectory.this;
    }
    
  }
  
}
