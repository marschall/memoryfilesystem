package com.github.marschall.memoryfilesystem;

import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttributeView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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

  DirectoryStream<Path> newDirectoryStream(Path basePath, Filter<? super Path> filter) {
    // REVIEW simply copying is not super scalable
    // REVIEW eager filtering might be nice
    List<String> elements = new ArrayList<>(this.entries.keySet());
    return new MemoryDirectoryStream(basePath, filter, elements);
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

  MemoryEntry getEntryOrException(String name, Path path) throws IOException {
    MemoryEntry entry = this.getEntry(name);
    if (entry == null) {
      throw new NoSuchFileException(path.toString());
    }
    return entry;
  }

  boolean isEmpty() {
    return this.entries.isEmpty();
  }

  void checkEmpty(Path path) throws IOException {
    if (this.isEmpty()) {
      throw new DirectoryNotEmptyException(path.toString());
    }
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

  void removeEntry(String name) {
    // TODO check for result
    this.entries.remove(name);
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
