package com.google.code.memoryfilesystem;

import static com.google.code.memoryfilesystem.AutoReleaseLock.autoRelease;

import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

abstract class MemoryEntry {
  
  private FileTime lastModifiedTime;
  private FileTime lastAccessTime;
  private FileTime creationTime;
  
  private final ReadWriteLock lock;
  

  MemoryEntry() {
    this.lock = new ReentrantReadWriteLock();
    long now = System.currentTimeMillis();
    this.lastAccessTime = FileTime.fromMillis(now);
    this.lastModifiedTime = FileTime.fromMillis(now);
    this.creationTime = FileTime.fromMillis(now);
  }
  

  private AutoRelease readLock() {
    return autoRelease(this.lock.readLock());
  }

  private AutoRelease writeLock() {
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
  
  void setTimes(FileTime lastModifiedTime, FileTime lastAccessTime, FileTime createTime) {
    try (AutoRelease lock = this.writeLock()) {
      this.lastModifiedTime = lastModifiedTime;
      this.lastAccessTime = lastAccessTime;
      this.creationTime = createTime;
    }
  }
  
  abstract BasicFileAttributeView getBasicFileAttributeView();
  
  abstract class MemoryEntryFileAttributesView implements BasicFileAttributeView {
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String name() {
      return "basic";
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void setTimes(FileTime lastModifiedTime, FileTime lastAccessTime, FileTime createTime) {
        MemoryEntry.this.setTimes(lastModifiedTime, lastAccessTime, createTime);
    }
    
  }
  
  abstract class MemoryEntryFileAttributes implements BasicFileAttributes {
    
    /**
     * {@inheritDoc}
     */
    @Override
    public FileTime lastModifiedTime() {
      return MemoryEntry.this.lastModifiedTime();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FileTime lastAccessTime() {
      return MemoryEntry.this.lastAccessTime();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FileTime creationTime() {
      return MemoryEntry.this.creationTime();
    }
    
  }

}
