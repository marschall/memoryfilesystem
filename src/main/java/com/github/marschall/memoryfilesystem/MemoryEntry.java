package com.github.marschall.memoryfilesystem;

import static com.github.marschall.memoryfilesystem.AutoReleaseLock.autoRelease;
import static com.github.marschall.memoryfilesystem.MemoryFileSystemProperties.BASIC_FILE_ATTRIBUTE_VIEW_NAME;

import java.nio.file.AccessMode;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

abstract class MemoryEntry {
  
  private final String originalName;
  
  // protected by read and write locks
  private FileTime lastModifiedTime;
  private FileTime lastAccessTime;
  private FileTime creationTime;
  
  private final ReadWriteLock lock;
  
  MemoryEntry(String originalName) {
    this.originalName = originalName;
    this.lock = new ReentrantReadWriteLock();
    FileTime now = this.getNow();
    this.lastAccessTime = now;
    this.lastModifiedTime = now;
    this.creationTime = now;
  }


  String getOriginalName() {
    return this.originalName;
  }
  
  private FileTime getNow() {
    long now = System.currentTimeMillis();
    return FileTime.fromMillis(now);
  }
  

  AutoRelease readLock() {
    return autoRelease(this.lock.readLock());
  }

  AutoRelease writeLock() {
    return autoRelease(this.lock.writeLock());
  }
  
  FileTime lastModifiedTime() {
    try (AutoRelease lock = this.readLock()) {
      return this.lastModifiedTime;
    }
  }
  
  FileTime lastAccessTime() {
    try (AutoRelease lock = this.readLock()) {
      return this.lastAccessTime;
    }
  }
  
  FileTime creationTime() {
    try (AutoRelease lock = this.readLock()) {
      return this.creationTime;
    }
  }
  
  void checkAccess(AccessMode... modes) {
    try (AutoRelease lock = this.readLock()) {
      AccessMode unsupported = this.getUnsupported(modes);
      if (unsupported != null) {
        throw new UnsupportedOperationException("access mode " + unsupported + " is not supported");
      }
      // TODO implement
    }
  }
  
  private AccessMode getUnsupported(AccessMode... modes) {
    for (AccessMode mode : modes) {
      if (!(mode == AccessMode.READ || mode == AccessMode.WRITE || mode == AccessMode.EXECUTE)) {
        return mode;
      }
    }
    return null;
  }
  
  void modified() {
    // No write lock because this was to be folded in an operation with a write lock
    FileTime now = this.getNow();
    this.lastAccessTime = now;
    this.lastModifiedTime = now;
  }
  
  void accessed() {
    // No write lock because this was to be folded in an operation with a write lock
    this.lastAccessTime = this.getNow();
  }
  
  void setTimes(FileTime lastModifiedTime, FileTime lastAccessTime, FileTime createTime) {
    try (AutoRelease lock = this.writeLock()) {
      this.lastModifiedTime = lastModifiedTime;
      this.lastAccessTime = lastAccessTime;
      this.creationTime = createTime;
    }
  }
  
  
  <A extends BasicFileAttributes> A readAttributes(Class<A> type) {
    if (type == BasicFileAttributes.class) {
      this.accessed();
      return (A) this.getBasicFileAttributes();
    } else {
      throw new UnsupportedOperationException("file attribute view" + type + " not supported");
    }
  }
  
  abstract BasicFileAttributeView getBasicFileAttributeView();
  abstract BasicFileAttributes getBasicFileAttributes();
  
  abstract class MemoryEntryFileAttributesView implements BasicFileAttributeView {
    
    @Override
    public String name() {
      return BASIC_FILE_ATTRIBUTE_VIEW_NAME;
    }
    
    @Override
    public void setTimes(FileTime lastModifiedTime, FileTime lastAccessTime, FileTime createTime) {
        MemoryEntry.this.setTimes(lastModifiedTime, lastAccessTime, createTime);
    }
    
  }
  
  abstract class MemoryEntryFileAttributes implements BasicFileAttributes {
    
    @Override
    public FileTime lastModifiedTime() {
      return MemoryEntry.this.lastModifiedTime();
    }

    @Override
    public FileTime lastAccessTime() {
      return MemoryEntry.this.lastAccessTime();
    }

    @Override
    public FileTime creationTime() {
      return MemoryEntry.this.creationTime();
    }
    
  }

}
