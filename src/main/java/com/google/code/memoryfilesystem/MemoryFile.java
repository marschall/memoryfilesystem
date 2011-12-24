package com.google.code.memoryfilesystem;

import java.io.IOException;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;

class MemoryFile extends MemoryEntry {
  
  private MemoryContents contents;

  private final BasicFileAttributes attributes;

  private BasicFileAttributeView basicFileAttributeView;
  
  MemoryFile() {
    this.attributes = new MemoryFileAttributes();
    this.basicFileAttributeView = new MemoryFileAttributesView();
    this.contents = new MemoryContents(16);
  }
  
  /**
   * {@inheritDoc}
   */
  @Override
  BasicFileAttributeView getBasicFileAttributeView() {
    return this.basicFileAttributeView;
  }
  
  
  class MemoryFileAttributesView extends MemoryEntryFileAttributesView {

    /**
     * {@inheritDoc}
     */
    @Override
    public BasicFileAttributes readAttributes() throws IOException {
      return MemoryFile.this.attributes;
    }
    
  }
  
  final class MemoryFileAttributes extends MemoryEntryFileAttributes {


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isRegularFile() {
      return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDirectory() {
      return false;
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

    @Override
    public long size() {
      return contents.size();
    }

    @Override
    public Object fileKey() {
      // REVIEW think about it
      return MemoryFile.this;
    }
    
  }


}
