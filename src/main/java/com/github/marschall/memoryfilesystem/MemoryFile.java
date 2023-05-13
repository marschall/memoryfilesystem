package com.github.marschall.memoryfilesystem;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.DELETE_ON_CLOSE;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.SYNC;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.util.Arrays;
import java.util.Set;

import com.github.marschall.memoryfilesystem.OneTimePermissionChecker.PermissionChecker;

class MemoryFile extends MemoryEntry implements MemoryContents {

  /**
   * The number of open streams or channels.
   *
   * <p>A negative number indicates the file is marked for deletion.
   */
  private int openCount;

  /**
   * In other to implement hard links we allow sharing hard links.
   * We do not need a hard link counter since we have a garbage collector.
   */
  private final MemoryInode inode;

  MemoryFile(String originalName, EntryCreationContext context) {
    this(originalName, context, 0);
  }

  MemoryFile(String originalName, EntryCreationContext context, int initialBlocks) {
    super(originalName, context);
    this.openCount = 0;
    this.inode = new MemoryInode(initialBlocks);
  }

  /**
   * Copy constructor.
   */
  MemoryFile(String originalName, EntryCreationContext context, MemoryFile other) {
    super(originalName, context);
    this.openCount = 0;
    this.inode = new MemoryInode(other.inode);
  }

  /**
   * Hard link constructor.
   */
  private MemoryFile(String originalName, EntryCreationContext context, MemoryInode inode, MemoryFile other) {
    super(originalName, context, other);
    this.openCount = 0;
    this.inode = inode;
  }

  MemoryFile createLink(String originalName, EntryCreationContext context) {
    return new MemoryFile(originalName, context, this.inode, this);
  }

  @Override
  MemoryEntryAttributes newMemoryEntryAttributes(EntryCreationContext context) {
    return new MemoryFileAttributes(context);
  }

  @Override
  public void accessed() {
    // here to increase the scope to public
    super.accessed();
  }

  @Override
  public void modified() {
    // here to increase the scope to public
    super.modified();
  }

  @Override
  public long size() {
    return this.inode.size();
  }

  @Override
  boolean isDirectory() {
    return false;
  }

  InputStream newInputStream(Set<? extends OpenOption> options, Path path) throws IOException {
    boolean deleteOnClose = options.contains(DELETE_ON_CLOSE);
    boolean sync = options.contains(SYNC);
    return this.newInputStream(deleteOnClose, path);
  }

  OutputStream newOutputStream(Set<? extends OpenOption> options, Path path) throws IOException {
    boolean deleteOnClose = options.contains(DELETE_ON_CLOSE);
    boolean append = options.contains(APPEND);
    boolean truncate = options.contains(TRUNCATE_EXISTING);
    if (append && truncate) {
      throw new IllegalArgumentException("invalid combination of options: " + Arrays.asList(APPEND, TRUNCATE_EXISTING));
    }
    boolean sync = options.contains(SYNC);
    if (append) {
      return this.newAppendingOutputStream(deleteOnClose, path);
    } else {
      if (truncate) {
        this.truncate(0L);
      }
      return this.newOutputStream(deleteOnClose, path);
    }
  }

  BlockChannel newChannel(Set<? extends OpenOption> options, Path path) throws IOException {
    boolean append = options.contains(APPEND);
    boolean writable = options.contains(WRITE);
    // if neither read nor write are present we default to read
    // java.nio.file.Files.newByteChannel(Path, Set<? extends OpenOption>, FileAttribute<?>...)
    boolean readable = options.contains(READ) || (!writable && !append);
    boolean deleteOnClose = options.contains(DELETE_ON_CLOSE);
    boolean sync = options.contains(SYNC);
    boolean truncate = options.contains(TRUNCATE_EXISTING);
    if (writable && append && truncate) {
      throw new IllegalArgumentException("invalid combination of options: " + Arrays.asList(WRITE, APPEND, TRUNCATE_EXISTING));
    }

    if (append) {
      return this.newAppendingChannel(readable, deleteOnClose, path);
    } else {
      if (writable) {
        if (truncate) {
          this.truncate(0L);
        }
      }
      return this.newChannel(readable, writable, deleteOnClose, path);
    }
  }

  InputStream newInputStream(boolean deleteOnClose, Path path) throws IOException {
    try (AutoRelease lock = this.writeLock()) {
      this.incrementOpenCount(path);
      return new BlockInputStream(this, deleteOnClose, path, () -> this.checkAccess(AccessMode.READ));
    }
  }

  OutputStream newOutputStream(boolean deleteOnClose, Path path) throws IOException {
    try (AutoRelease lock = this.writeLock()) {
      this.checkAccess(AccessMode.WRITE);
      this.incrementOpenCount(path);
      return new NonAppendingBlockOutputStream(this, deleteOnClose, path, () -> this.checkAccess(AccessMode.WRITE));
    }
  }

  OutputStream newAppendingOutputStream(boolean deleteOnClose, Path path) throws IOException {
    try (AutoRelease lock = this.writeLock()) {
      this.checkAccess(AccessMode.WRITE);
      this.incrementOpenCount(path);
      return new AppendingBlockOutputStream(this, deleteOnClose, path, () -> this.checkAccess(AccessMode.WRITE));
    }
  }

  BlockChannel newChannel(boolean readable, boolean writable, boolean deleteOnClose, Path path) throws IOException {
    try (AutoRelease lock = this.writeLock()) {
      this.incrementOpenCount(path);
      PermissionChecker permissionChecker;
      if (readable) {
        if (writable) {
          permissionChecker = () -> this.checkAccess(AccessMode.READ, AccessMode.WRITE);
        } else {
          permissionChecker = () -> this.checkAccess(AccessMode.READ);
        }
      } else {
        permissionChecker = () -> this.checkAccess(AccessMode.WRITE);
      }
      return new NonAppendingBlockChannel(this, readable, writable, deleteOnClose, path, permissionChecker);
    }
  }

  BlockChannel newAppendingChannel(boolean readable, boolean deleteOnClose, Path path) throws IOException {
    try (AutoRelease lock = this.writeLock()) {
      this.incrementOpenCount(path);
      PermissionChecker permissionChecker;
      if (readable) {
        permissionChecker = () -> this.checkAccess(AccessMode.READ, AccessMode.WRITE);
      } else {
        permissionChecker = () -> this.checkAccess(AccessMode.WRITE);
      }
      return new AppendingBlockChannel(this, readable, deleteOnClose, path, permissionChecker);
    }
  }

  private void incrementOpenCount(Path path) throws NoSuchFileException {
    if (this.openCount < 0) {
      throw new NoSuchFileException(path.toString());
    }
    this.openCount += 1;
  }

  int openCount() {
    return this.openCount;
  }

  void markForDeletion() {
    this.openCount = -1;
  }

  @Override
  public void closedStream(Path path, boolean delete) {
    try (AutoRelease lock = this.writeLock()) {
      this.openCount -= 1;
    }
    if (delete) {
      // intentionally not covered by Lock
      try {
        Files.delete(path);
      } catch (IOException e) {
        // ignore, only a best effort is made
      }
    }
  }

  @Override
  public void closedChannel(Path path, boolean delete) {
    try (AutoRelease lock = this.writeLock()) {
      this.openCount -= 1;
    }
    if (delete) {
      // intentionally not covered by Lock
      try {
        Files.delete(path);
      } catch (IOException e) {
        // ignore, only a best effort is made
      }
    }
  }

  @Override
  public long read(ByteBuffer dst, long position, long maximum) throws IOException {
    return this.inode.read(dst, position, maximum);
  }

  @Override
  public int readShort(ByteBuffer dst, long position) throws IOException {
    return this.inode.readShort(dst, position);
  }

  @Override
  public int read(byte[] dst, long position, int off, int len) throws IOException {
    return this.inode.read(dst, position, off, len);
  }

  @Override
  public long transferFrom(ReadableByteChannel src, long position, long count) throws IOException {
    return this.inode.transferFrom(src, position, count);
  }

  @Override
  public long transferTo(WritableByteChannel target, long position, long count) throws IOException {
    return this.inode.transferTo(target, position, count);
  }

  @Override
  public long transferTo(OutputStream target, long position) throws IOException {
    return this.inode.transferTo(target, position);
  }

  @Override
  public long write(ByteBuffer src, long position, long maximum) {
    return this.inode.write(src, position, maximum);
  }

  @Override
  public int writeShort(ByteBuffer src, long position) {
    return this.inode.writeShort(src, position);
  }

  @Override
  public int write(byte[] src, long position, int off, int len) {
    return this.inode.write(src, position, off, len);
  }

  @Override
  public long writeAtEnd(ByteBuffer src, long maximum) {
    return this.inode.writeAtEnd(src, maximum);
  }

  @Override
  public int writeAtEnd(ByteBuffer src) {
    return this.inode.writeAtEnd(src);
  }

  @Override
  public int writeAtEnd(byte[] src, int off, int len) {
    return this.inode.writeAtEnd(src, off, len);
  }

  @Override
  public void truncate(long newSize) {
    this.inode.truncate(newSize);
  }

  @Override
  public MemoryFileLock tryLock(MemoryFileLock lock) {
    return this.inode.tryLock(lock);
  }

  @Override
  public MemoryFileLock lock(MemoryFileLock lock) throws IOException {
    return this.inode.lock(lock);
  }

  @Override
  public void unlock(MemoryFileLock lock) {
    this.inode.unlock(lock);
  }

  @Override
  public String toString() {
    return "file(" + this.getOriginalName() + ')';
  }

  final class MemoryFileAttributes extends MemoryEntryAttributes {

    MemoryFileAttributes(EntryCreationContext context) {
      super(context);
    }

    @Override
    BasicFileAttributeView newBasicFileAttributeView() {
      return new MemoryFileAttributesView();
    }

    @Override
    long size() {
      return MemoryFile.this.size();
    }

  }

  boolean hasSameInodeAs(MemoryFile other) {
    return this.inode == other.inode;
  }

}
