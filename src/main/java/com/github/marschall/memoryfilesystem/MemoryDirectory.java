package com.github.marschall.memoryfilesystem;

import static java.nio.file.attribute.PosixFilePermission.GROUP_EXECUTE;
import static java.nio.file.attribute.PosixFilePermission.OTHERS_EXECUTE;
import static java.nio.file.attribute.PosixFilePermission.OWNER_EXECUTE;

import java.io.IOException;
import java.nio.file.AccessMode;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

class MemoryDirectory extends MemoryEntry {

  private final Map<String, MemoryEntry> entries;

  private final InitializingFileAttributeView basicFileAttributeView;

  private static final Set<PosixFilePermission> EXECUTE = EnumSet.of(OWNER_EXECUTE, GROUP_EXECUTE, OTHERS_EXECUTE);

  MemoryDirectory(String originalName) {
    this(originalName, EntryCreationContext.empty());
  }

  MemoryDirectory(String originalName, EntryCreationContext context) {
    super(originalName, context);
    this.entries = new HashMap<>();
    this.basicFileAttributeView = new MemoryDirectoryFileAttributesView();
  }

  private static Set<PosixFilePermission> addExecute(Set<PosixFilePermission> umask) {
    if (umask.isEmpty()) {
      return EXECUTE;
    }
    Set<PosixFilePermission> copy = EnumSet.copyOf(umask);
    copy.addAll(EXECUTE);
    return copy;
  }

  DirectoryStream<Path> newDirectoryStream(Path basePath, Filter<? super Path> filter) {
    // REVIEW simply copying is not super scalable
    // REVIEW eager filtering might be nice
    List<String> elements = new ArrayList<>(this.entries.keySet());
    return new MemoryDirectoryStream(basePath, filter, elements);
  }

  @Override
  InitializingFileAttributeView getBasicFileAttributeView() {
    return this.basicFileAttributeView;
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
    if (!this.isEmpty()) {
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

  @Override
  public String toString() {
    return "directory(" + this.getOriginalName() + ')';
  }

  void removeEntry(String name) {
    // TODO check for result
    this.entries.remove(name);
    this.modified();
  }

  class MemoryDirectoryFileAttributesView extends MemoryEntryFileAttributesView {

    @Override
    public BasicFileAttributes readAttributes() throws IOException {
      MemoryDirectory.this.checkAccess(AccessMode.READ);
      try (AutoRelease lock = MemoryDirectory.this.readLock()) {
        return new MemoryDirectoryFileAttributes(MemoryDirectory.this, MemoryDirectory.this.lastModifiedTime, MemoryDirectory.this.lastAccessTime, MemoryDirectory.this.creationTime);
      }
    }

  }

  static final class MemoryDirectoryFileAttributes extends MemoryEntryFileAttributes {

    MemoryDirectoryFileAttributes(Object fileKey, FileTime lastModifiedTime, FileTime lastAccessTime, FileTime creationTime) {
      super(fileKey, lastModifiedTime, lastAccessTime, creationTime);
    }

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

  }

}
