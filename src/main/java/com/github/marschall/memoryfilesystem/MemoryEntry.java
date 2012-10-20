package com.github.marschall.memoryfilesystem;

import static com.github.marschall.memoryfilesystem.AutoReleaseLock.autoRelease;
import static com.github.marschall.memoryfilesystem.MemoryFileSystemProperties.BASIC_FILE_ATTRIBUTE_VIEW_NAME;

import java.io.IOException;
import java.nio.file.AccessMode;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.DosFileAttributeView;
import java.nio.file.attribute.DosFileAttributes;
import java.nio.file.attribute.FileOwnerAttributeView;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.util.Set;
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
      // throw new AccessDeniedException
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
      this.checkAccess(AccessMode.WRITE);
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
  
  abstract class DelegatingFileAttributes implements BasicFileAttributeView, BasicFileAttributes {
    

    @Override
    public void setTimes(FileTime lastModifiedTime, FileTime lastAccessTime, FileTime createTime) throws IOException {
      getBasicFileAttributeView().setTimes(lastModifiedTime, lastAccessTime, createTime);
    }
    
    @Override
    public FileTime lastModifiedTime() {
      return getBasicFileAttributes().lastModifiedTime();
    }

    @Override
    public FileTime lastAccessTime() {
      return getBasicFileAttributes().lastAccessTime();
    }

    @Override
    public FileTime creationTime() {
      return getBasicFileAttributes().creationTime();
    }

    @Override
    public boolean isRegularFile() {
      return getBasicFileAttributes().isRegularFile();
    }

    @Override
    public boolean isDirectory() {
      return getBasicFileAttributes().isDirectory();
    }

    @Override
    public boolean isSymbolicLink() {
      return getBasicFileAttributes().isSymbolicLink();
    }

    @Override
    public boolean isOther() {
      return getBasicFileAttributes().isOther();
    }

    @Override
    public long size() {
      return getBasicFileAttributes().size();
    }

    @Override
    public Object fileKey() {
      return getBasicFileAttributes().fileKey();
    }
    
  }
  
  class MemoryDosFileAttributeView extends DelegatingFileAttributes implements DosFileAttributeView, DosFileAttributes {
    
    private boolean readOnly;
    private boolean hidden;
    private boolean system;
    private boolean archive;
    
    @Override
    public String name() {
      return "dos";
    }

    @Override
    public DosFileAttributes readAttributes() throws IOException {
      return this;
    }

    @Override
    public void setReadOnly(boolean value) throws IOException {
      try (AutoRelease lock = writeLock()) {
        checkAccess(AccessMode.WRITE);
        readOnly = value;
      }
    }

    @Override
    public void setHidden(boolean value) throws IOException {
      try (AutoRelease lock = writeLock()) {
        checkAccess(AccessMode.WRITE);
        hidden = value;
      }
      
    }

    @Override
    public void setSystem(boolean value) throws IOException {
      try (AutoRelease lock = writeLock()) {
        checkAccess(AccessMode.WRITE);
        system = value;
      }
      
    }

    @Override
    public void setArchive(boolean value) throws IOException {
      try (AutoRelease lock = writeLock()) {
        checkAccess(AccessMode.WRITE);
        archive = value;
      }
      
    }

    @Override
    public boolean isHidden() {
      try (AutoRelease lock = readLock()) {
        checkAccess(AccessMode.WRITE);
        return this.hidden;
      }
    }

    @Override
    public boolean isArchive() {
      try (AutoRelease lock = readLock()) {
        checkAccess(AccessMode.WRITE);
        return this.archive;
      }
    }

    @Override
    public boolean isSystem() {
      try (AutoRelease lock = readLock()) {
        checkAccess(AccessMode.WRITE);
        return this.system;
      }
    }
    

    @Override
    public boolean isReadOnly() {
      try (AutoRelease lock = readLock()) {
        checkAccess(AccessMode.WRITE);
        return this.readOnly;
      }
    }
    
  }
  
  abstract class MemoryFileOwnerAttributeView extends DelegatingFileAttributes implements FileOwnerAttributeView {
    
    private UserPrincipal owner;

    @Override
    public UserPrincipal getOwner() {
      try (AutoRelease lock = readLock()) {
        return this.owner;
      }
    }
    
    @Override
    public void setOwner(UserPrincipal owner) throws IOException {
      // TODO check same file system
      try (AutoRelease lock = writeLock()) {
        checkAccess(AccessMode.WRITE);
        this.owner = owner;
      }
    }
    
  }
  
  class MemoryPosixFileAttributeView extends MemoryFileOwnerAttributeView implements PosixFileAttributeView, PosixFileAttributes {

    private GroupPrincipal group;
    private Set<PosixFilePermission> perms;

    @Override
    public String name() {
      return "posix";
    }

    @Override
    public GroupPrincipal group() {
      try (AutoRelease lock = readLock()) {
        return this.group;
      }
    }
    
    @Override
    public UserPrincipal owner() {
      return getOwner();
    }
    
    @Override
    public void setGroup(GroupPrincipal group) throws IOException {
      // TODO check same file system
      try (AutoRelease lock = writeLock()) {
        checkAccess(AccessMode.WRITE);
        this.group = group;
      }
    }
    
    @Override
    public PosixFileAttributes readAttributes() throws IOException {
      return this;
    }

    @Override
    public Set<PosixFilePermission> permissions() {
      try (AutoRelease lock = readLock()) {
        return this.perms;
      }
    }

    @Override
    public void setPermissions(Set<PosixFilePermission> perms) throws IOException {
      try (AutoRelease lock = writeLock()) {
        checkAccess(AccessMode.WRITE);
        this.perms = perms;
      }
    }
    
  }

}
