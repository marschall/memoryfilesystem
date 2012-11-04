package com.github.marschall.memoryfilesystem;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttributeView;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

class MemoryDirectory extends MemoryEntry {

  private final Map<String, MemoryEntry> entries;

  private final BasicFileAttributes attributes;

  private final BasicFileAttributeView basicFileAttributeView;

  MemoryDirectory(String originalName) {
    this(originalName, Collections.<Class<? extends FileAttributeView>>emptySet());
  }

  MemoryDirectory(String originalName, Set<Class<? extends FileAttributeView>> additionalViews) {
    super(originalName, additionalViews);
    this.entries = new HashMap<>();
    this.attributes = new MemoryDirectoryFileAttributes();
    this.basicFileAttributeView = new MemoryDirectoryFileAttributesView();
  }

  @Override
  BasicFileAttributeView getBasicFileAttributeView() {
    return this.basicFileAttributeView;
  }

  @Override
  BasicFileAttributes getBasicFileAttributes() {
    return this.attributes;
  }

  MemoryEntry getEntry(String name) {
    // TODO atime?
    return this.entries.get(name);
  }

  void addEntry(String name, MemoryEntry entry) throws IOException {
    MemoryEntry previous = this.entries.put(name, entry);
    if (previous != null) {
      // avoid double look up in common case
      // fix if we broke it
      this.entries.put(name, previous);
      // FIXME needs to be path
      throw new FileAlreadyExistsException("entry " + name + " already exists");
    }
    this.modified();
  }

  class MemoryDirectoryFileAttributesView extends MemoryEntryFileAttributesView {

    @Override
    public BasicFileAttributes readAttributes() throws IOException {
      return MemoryDirectory.this.attributes;
    }

  }

  final class MemoryDirectoryFileAttributes extends MemoryEntryFileAttributes {

    @Override
    public boolean isRegularFile() {
      return false;
    }

    @Override
    public boolean isDirectory() {
      return true;
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
      // REVIEW make configurable
      return -1L;
    }

    @Override
    public Object fileKey() {
      // REVIEW think about it
      return MemoryDirectory.this;
    }

  }

}
