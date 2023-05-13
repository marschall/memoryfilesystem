package com.github.marschall.memoryfilesystem;

import java.nio.file.attribute.BasicFileAttributeView;

class MemorySymbolicLink extends MemoryEntry {

  private AbstractPath target;

  MemorySymbolicLink(String originalName, AbstractPath target, EntryCreationContext context) {
    super(originalName, context);
    this.target = target;
  }

  @Override
  MemoryEntryAttributes newMemoryEntryAttributes(EntryCreationContext context) {
    return new MemorySymbolicLAttributes(context);
  }

  @Override
  boolean isDirectory() {
    return false;
  }

  AbstractPath getTarget() {
    return this.target;
  }

  void setTarget(AbstractPath target) {
    this.target = target;
  }


  @Override
  public String toString() {
    return "symlink(" + this.getOriginalName() + ") -> " + this.target;
  }

  static final class MemorySymbolicLAttributes extends MemoryEntryAttributes {

    MemorySymbolicLAttributes(EntryCreationContext context) {
      super(context);
    }

    @Override
    BasicFileAttributeView newBasicFileAttributeView() {
      return new MemorySymbolicLinkAttributesView();
    }

    @Override
    long size() {
      return -1;
    }

  }

}
