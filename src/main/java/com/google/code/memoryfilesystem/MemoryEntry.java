package com.google.code.memoryfilesystem;

import static com.google.code.memoryfilesystem.AutoReleaseLock.autoRelease;
import static com.google.code.memoryfilesystem.MemoryFileSystemProperties.BASIC_FILE_ATTRIBUTE_VIEW_NAME;

import java.nio.file.AccessMode;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

abstract class MemoryEntry {
  
  // protected by read and write locks
  private FileTime lastModifiedTime;
  private FileTime lastAccessTime;
  private FileTime creationTime;
  
  private final ReadWriteLock lock;
  

  MemoryEntry() {
    this.lock = new ReentrantReadWriteLock();
    FileTime now = this.getNow();
    this.lastAccessTime = now;
    this.lastModifiedTime = now;
    this.creationTime = now;
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
  
  void setTimes(FileTime lastModifiedTime, FileTime lastAccessTime, FileTime createTime) {
    try (AutoRelease lock = this.writeLock()) {
      this.lastModifiedTime = lastModifiedTime;
      this.lastAccessTime = lastAccessTime;
      this.creationTime = createTime;
    }
  }
  
  abstract BasicFileAttributeView getBasicFileAttributeView();
  
  abstract <A extends BasicFileAttributes> A readAttributes(Class<A> type);
  
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
