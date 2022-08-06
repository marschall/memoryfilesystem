package com.github.marschall.memoryfilesystem;

import static com.github.marschall.memoryfilesystem.AutoReleaseLock.autoRelease;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.AccessMode;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttributeView;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

abstract class MemoryEntry {

  // can be changed by a move/rename
  // not protected by a lock because we need it in #toString
  private volatile String originalName;
  private final MemoryEntryAttributes attributes;

  private final ReadWriteLock lock;

  MemoryEntry(String originalName, EntryCreationContext context) {
    this.originalName = originalName;
    this.lock = new ReentrantReadWriteLock();
    this.attributes = this.newMemoryEntryAttributes(context);
  }

  MemoryEntry(String originalName, EntryCreationContext context, MemoryEntry other) {
    this.originalName = originalName;
    this.lock = new ReentrantReadWriteLock();
    this.attributes = other.attributes;
  }

  abstract MemoryEntryAttributes newMemoryEntryAttributes(EntryCreationContext context);

  void initializeAttributes(MemoryEntry other) throws IOException {
    this.attributes.initializeAttributes(other.attributes);
  }


  void initializeRoot() {
    this.attributes.initializeRoot();
  }

  String getOriginalName() {
    return this.originalName;
  }

  long getNow() {
    return System.currentTimeMillis();
  }

  AutoRelease readLock() {
    return autoRelease(this.lock.readLock());
  }

  AutoRelease writeLock() {
    return autoRelease(this.lock.writeLock());
  }

  abstract boolean isDirectory();

  AutoRelease lock(LockType lockType) {
    switch (lockType) {
      case READ:
        return autoRelease(this.lock.readLock());
      case WRITE:
        return autoRelease(this.lock.writeLock());
      default:
        throw new IllegalArgumentException("unknown lock type");
    }
  }

  void checkAccess(AccessMode... modes) throws AccessDeniedException {
    this.attributes.checkAccess(modes);
  }

  boolean canRead() {
    return this.attributes.canRead();
  }

  void checkAccess(AccessMode mode) throws AccessDeniedException {
    this.attributes.checkAccess(mode);
  }

  void modified() {
    this.attributes.modified();
  }

  void accessed() {
    this.attributes.accessed();
  }

  BasicFileAttributeView getBasicFileAttributeView() {
    return this.attributes.getBasicFileAttributeView();
  }

  <A extends FileAttributeView> A getFileAttributeView(Class<A> type) throws AccessDeniedException {
    return this.attributes.getFileAttributeView(type);
  }

  <A extends BasicFileAttributes> A readAttributes(Class<A> type) throws IOException {
    return this.attributes.readAttributes(type);
  }

  void setOriginalName(String newOriginalName) {
    this.originalName = newOriginalName;
  }

}
