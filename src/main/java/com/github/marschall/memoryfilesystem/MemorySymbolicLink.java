package com.github.marschall.memoryfilesystem;

import java.io.IOException;
import java.nio.file.AccessMode;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

class MemorySymbolicLink extends MemoryEntry {

  private final Path target;

  private final BasicFileAttributes attributes;

  private final InitializingFileAttributeView basicFileAttributeView;

  MemorySymbolicLink(String originalName, AbstractPath target) {
    this(originalName, target, EntryCreationContext.empty());
  }


  MemorySymbolicLink(String originalName, AbstractPath target, EntryCreationContext context) {
    super(originalName, context);
    this.target = target;
    this.attributes = new MemorySymbolicLinkAttributes();
    this.basicFileAttributeView = new MemorySymbolicLinkAttributesView();
  }

  Path getTarget() {
    return this.target;
  }

  @Override
  InitializingFileAttributeView getBasicFileAttributeView() {
    return this.basicFileAttributeView;
  }

  @Override
  BasicFileAttributes getBasicFileAttributes() {
    return this.attributes;
  }


  @Override
  public String toString() {
    return "symlin(" + this.getOriginalName() + ") -> " + this.target;
  }

  class MemorySymbolicLinkAttributesView extends MemoryEntryFileAttributesView {

    @Override
    public BasicFileAttributes readAttributes() throws IOException {
      MemorySymbolicLink.this.checkAccess(AccessMode.READ);
      return MemorySymbolicLink.this.attributes;
    }

  }

  final class MemorySymbolicLinkAttributes extends MemoryEntryFileAttributes {

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

    @Override
    public Object fileKey() {
      // REVIEW think about it
      return MemorySymbolicLink.this;
    }

  }

}
