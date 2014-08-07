package com.github.marschall.memoryfilesystem;

import java.io.IOException;
import java.nio.file.AccessMode;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;

class MemorySymbolicLink extends MemoryEntry {

  private final AbstractPath target;

  private final MemorySymbolicLinkAttributesView basicFileAttributeView;

  MemorySymbolicLink(String originalName, AbstractPath target) {
    this(originalName, target, EntryCreationContext.empty());
  }


  MemorySymbolicLink(String originalName, AbstractPath target, EntryCreationContext context) {
    super(originalName, context);
    this.target = target;
    this.basicFileAttributeView = new MemorySymbolicLinkAttributesView();
  }

  AbstractPath getTarget() {
    return this.target;
  }

  @Override
  BasicFileAttributeView getBasicFileAttributeView() {
    return this.basicFileAttributeView;
  }

  @Override
  InitializingFileAttributeView getInitializingFileAttributeView() {
    return this.basicFileAttributeView;
  }


  @Override
  public String toString() {
    return "symlink(" + this.getOriginalName() + ") -> " + this.target;
  }

  class MemorySymbolicLinkAttributesView extends MemoryEntryFileAttributesView {

    @Override
    public BasicFileAttributes readAttributes() throws IOException {
      MemorySymbolicLink.this.checkAccess(AccessMode.READ);
      try (AutoRelease lock = MemorySymbolicLink.this.readLock()) {
        FileTime lastModifiedTime = MemorySymbolicLink.this.lastModifiedTime();
        FileTime lastAccessTime = MemorySymbolicLink.this.lastAccessTime();
        FileTime creationTime = MemorySymbolicLink.this.creationTime();
        return new MemorySymbolicLinkAttributes(MemorySymbolicLink.this, lastModifiedTime, lastAccessTime, creationTime);
      }
    }

  }

  static final class MemorySymbolicLinkAttributes extends MemoryEntryFileAttributes {

    MemorySymbolicLinkAttributes(Object fileKey, FileTime lastModifiedTime, FileTime lastAccessTime, FileTime creationTime) {
      super(fileKey, lastModifiedTime, lastAccessTime, creationTime);
    }

    @Override
    public boolean isRegularFile() {
      return false;
    }

    @Override
    public boolean isDirectory() {
      return false;
    }

    @Override
    public boolean isSymbolicLink() {
      return true;
    }

    @Override
    public boolean isOther() {
      return false;
    }

    @Override
    public long size() {
      // REVIEW make configurable
      return -1L;
    }

  }

}
