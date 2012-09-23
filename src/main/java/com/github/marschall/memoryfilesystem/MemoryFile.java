package com.github.marschall.memoryfilesystem;

import java.io.IOException;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;

class MemoryFile extends MemoryEntry {
  
  private final MemoryContents contents;

  private final BasicFileAttributes attributes;

  private final BasicFileAttributeView basicFileAttributeView;
  
  MemoryFile() {
    this.attributes = new MemoryFileAttributes();
    this.basicFileAttributeView = new MemoryFileAttributesView();
    this.contents = new MemoryContents(16);
  }
  
  @Override
  BasicFileAttributeView getBasicFileAttributeView() {
    return this.basicFileAttributeView;
  }
  
  <A extends BasicFileAttributes> A readAttributes(Class<A> type) {
    if (type == BasicFileAttributes.class) {
      this.accessed();
      return (A) this.attributes;
    } else {
      throw new UnsupportedOperationException("file attribute view" + type + " not supported");
    }
  }
  
  
  class MemoryFileAttributesView extends MemoryEntryFileAttributesView {

    @Override
    public BasicFileAttributes readAttributes() throws IOException {
      return MemoryFile.this.attributes;
    }
    
  }
  
  final class MemoryFileAttributes extends MemoryEntryFileAttributes {

    @Override
    public boolean isRegularFile() {
      return true;
    }

    @Override
    public boolean isDirectory() {
      return false;
    }

    @Override
    public boolean isSymbolicLink() {
      return false;
    }

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
