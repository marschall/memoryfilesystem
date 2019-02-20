package com.github.marschall.memoryfilesystem;

import static java.nio.file.attribute.PosixFilePermission.GROUP_EXECUTE;
import static java.nio.file.attribute.PosixFilePermission.OTHERS_EXECUTE;
import static java.nio.file.attribute.PosixFilePermission.OWNER_EXECUTE;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.AccessMode;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

class MemoryDirectory extends MemoryEntry {

  private final Map<String, MemoryEntry> entries;

  private static final Set<PosixFilePermission> EXECUTE = EnumSet.of(OWNER_EXECUTE, GROUP_EXECUTE, OTHERS_EXECUTE);

  MemoryDirectory(String originalName, EntryCreationContext context) {
    super(originalName, context);
    this.entries = new HashMap<>();
  }

  @Override
  MemoryEntryAttributes newMemoryEntryAttributes(EntryCreationContext context) {
    return new MemoryDirectoryAttributes(context);
  }

  private static Set<PosixFilePermission> addExecute(Set<PosixFilePermission> perms) {
    Set<PosixFilePermission> copy = EnumSet.copyOf(perms);
    copy.addAll(EXECUTE);
    return copy;
  }

  DirectoryStream<Path> newDirectoryStream(Path basePath, Filter<? super Path> filter) throws AccessDeniedException {
    // REVIEW simply copying is not super scalable
    // REVIEW eager filtering might be nice
    this.checkAccess(AccessMode.EXECUTE);
    List<String> elements = new ArrayList<>(this.entries.size());
    for (MemoryEntry entry : this.entries.values()) {
      elements.add(entry.getOriginalName());
    }
    return new MemoryDirectoryStream(basePath, filter, elements);
  }

  MemoryEntry getEntry(String name) {
    // TODO atime?
    return this.entries.get(name);
  }

  MemoryEntry getEntryOrException(String name, Path path) throws IOException {
    this.checkAccess(AccessMode.EXECUTE);
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

  // caller have to check for write permissions
  // we can't do it here because that may break operations that involve
  // two directories
  void addEntry(String name, MemoryEntry entry, Path originalPath) throws IOException {
    MemoryEntry previous = this.entries.put(name, entry);
    if (previous != null) {
      // avoid double look up in common case
      // fix if we broke it
      this.entries.put(name, previous);
      throw new FileAlreadyExistsException(originalPath.toString());
    }
    this.modified();
  }

  @Override
  public String toString() {
    return "directory(" + this.getOriginalName() + ')';
  }

  //caller have to check for write permissions
  // we can't do it here because that may break operations that involve
  // two directories
  void removeEntry(String name) {
    // TODO check for result
    this.entries.remove(name);
    this.modified();
  }

  static final class MemoryDirectoryAttributes extends MemoryEntryAttributes {

    /**
     * {@link java.nio.file.attribute.BasicFileAttributes#size()} says it's unspecified.
     */
    static final long SIZE = -1L;

    MemoryDirectoryAttributes(EntryCreationContext context) {
      super(context);
    }

    @Override
    BasicFileAttributeView newBasicFileAttributeView() {
      return new MemoryDirectoryFileAttributesView();
    }

    @Override
    long size() {
      return SIZE;
    }

  }

}
