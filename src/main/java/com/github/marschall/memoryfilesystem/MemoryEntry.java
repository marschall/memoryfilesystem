package com.github.marschall.memoryfilesystem;

import static com.github.marschall.memoryfilesystem.AutoReleaseLock.autoRelease;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.AccessMode;
import java.nio.file.FileSystemException;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.DosFileAttributeView;
import java.nio.file.attribute.DosFileAttributes;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileOwnerAttributeView;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserDefinedFileAttributeView;
import java.nio.file.attribute.UserPrincipal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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

  private final Map<String, InitializingFileAttributeView> additionalAttributes;

  MemoryEntry(String originalName, EntryCreationContext context) {
    this.originalName = originalName;
    this.lock = new ReentrantReadWriteLock();
    FileTime now = this.getNow();
    this.lastAccessTime = now;
    this.lastModifiedTime = now;
    this.creationTime = now;
    if (context.additionalViews.isEmpty()) {
      this.additionalAttributes = Collections.emptyMap();
    } else if (context.additionalViews.size() == 1) {
      InitializingFileAttributeView view = this.instantiate(context.firstView(), context);
      this.additionalAttributes = Collections.singletonMap(view.name(), view);
    } else {
      this.additionalAttributes = new HashMap<>(context.additionalViews.size());
      for (Class<? extends FileAttributeView> viewClass : context.additionalViews) {
        InitializingFileAttributeView view = this.instantiate(viewClass, context);
        this.additionalAttributes.put(view.name(), view);
      }
    }
  }

  void initializeAttributes(MemoryEntry other) throws IOException {
    try (AutoRelease lock = this.writeLock()) {
      other.getBasicFileAttributeView().initializeFrom(this.getBasicFileAttributeView());
      for (InitializingFileAttributeView view : this.additionalAttributes.values()) {
        view.initializeFrom(other.additionalAttributes);
      }
    }
  }


  void initializeRoot() {
    try (AutoRelease lock = this.readLock()) {
      for (InitializingFileAttributeView view : this.additionalAttributes.values()) {
        view.initializeRoot();
      }
    }
  }

  private InitializingFileAttributeView instantiate(Class<? extends FileAttributeView> viewClass,
          EntryCreationContext context) {
    if (viewClass == PosixFileAttributeView.class) {
      return new MemoryPosixFileAttributeView(context);
    } else if (viewClass == DosFileAttributeView.class) {
      return new MemoryDosFileAttributeView();
    } if (viewClass == UserDefinedFileAttributeView.class) {
      return new MemoryUserDefinedFileAttributeView();
    } else {
      throw new IllegalArgumentException("unknown file attribute view: " + viewClass);
    }
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
      if (lastModifiedTime != null) {
        this.lastModifiedTime = lastModifiedTime;
      }
      if (lastAccessTime != null) {
        this.lastAccessTime = lastAccessTime;
      }
      if (createTime != null) {
        this.creationTime = createTime;
      }
    }
  }

  <A extends FileAttributeView> A getFileAttributeView(Class<A> type) {
    try (AutoRelease lock = this.readLock()) {
      this.checkAccess(AccessMode.READ);
      if (type == BasicFileAttributeView.class) {
        return (A) this.getBasicFileAttributeView();
      } else {
        String name = null;
        if (type == FileOwnerAttributeView.class) {
          // owner is either mapped to POSIX or ACL
          // TODO POSIX and ACL?
          if (this.additionalAttributes.containsKey(FileAttributeViews.POSIX)) {
            name = FileAttributeViews.POSIX;
          } else if (this.additionalAttributes.containsKey(FileAttributeViews.ACL)) {
            name = FileAttributeViews.ACL;
          }
        } else {
          name = FileAttributeViews.mapAttributeView(type);
        }
        if (name == null) {
          throw new UnsupportedOperationException("file attribute view" + type + " not supported");
        }
        FileAttributeView view = this.additionalAttributes.get(name);
        if (view != null) {
          return (A) view;
        } else {
          throw new UnsupportedOperationException("file attribute view" + type + " not supported");
        }
      }
    }
  }

  <A extends BasicFileAttributes> A readAttributes(Class<A> type) throws IOException {
    try (AutoRelease lock = this.readLock()) {
      this.checkAccess(AccessMode.READ);
      if (type == BasicFileAttributes.class) {
        return (A) this.getBasicFileAttributes();
      } else {
        String viewName = FileAttributeViews.mapFileAttributes(type);
        if (viewName != null) {
          FileAttributeView view = this.additionalAttributes.get(viewName);
          if (view instanceof BasicFileAttributeView) {
            return (A) ((BasicFileAttributeView) view).readAttributes();
          } else {
            throw new UnsupportedOperationException("file attributes " + type + " not supported");
          }
        } else {
          throw new UnsupportedOperationException("file attributes " + type + " not supported");
        }
      }
    }
  }

  abstract InitializingFileAttributeView getBasicFileAttributeView();
  abstract BasicFileAttributes getBasicFileAttributes();

  abstract class MemoryEntryFileAttributesView implements InitializingFileAttributeView {

    @Override
    public String name() {
      return FileAttributeViews.BASIC;
    }

    @Override
    public void initializeFrom(BasicFileAttributeView basicFileAttributeView) throws IOException {
      BasicFileAttributes otherAttributes = basicFileAttributeView.readAttributes();
      this.setTimes(otherAttributes.lastModifiedTime(), otherAttributes.lastAccessTime(), otherAttributes.creationTime());

    }

    @Override
    public void initializeFrom(Map<String, ? extends FileAttributeView> additionalAttributes) {
      // ignore
    }

    @Override
    public void initializeRoot() {
      // ignore
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

  abstract class DelegatingFileAttributes implements BasicFileAttributeView, BasicFileAttributes, InitializingFileAttributeView {


    @Override
    public void setTimes(FileTime lastModifiedTime, FileTime lastAccessTime, FileTime createTime) throws IOException {
      MemoryEntry.this.getBasicFileAttributeView().setTimes(lastModifiedTime, lastAccessTime, createTime);
    }

    @Override
    public void initializeFrom(BasicFileAttributeView basicFileAttributeView) {
      // ignore
    }

    @Override
    public void initializeRoot() {
      // ignore
    }

    @Override
    public void initializeFrom(Map<String, ? extends FileAttributeView> additionalAttributes) {
      FileAttributeView selfAttributes = additionalAttributes.get(this.name());
      if (selfAttributes != null) {
        this.initializeFromSelf(selfAttributes);
      }
    }

    abstract void initializeFromSelf(FileAttributeView selfAttributes);

    @Override
    public FileTime lastModifiedTime() {
      return MemoryEntry.this.getBasicFileAttributes().lastModifiedTime();
    }

    @Override
    public FileTime lastAccessTime() {
      return MemoryEntry.this.getBasicFileAttributes().lastAccessTime();
    }

    @Override
    public FileTime creationTime() {
      return MemoryEntry.this.getBasicFileAttributes().creationTime();
    }

    @Override
    public boolean isRegularFile() {
      return MemoryEntry.this.getBasicFileAttributes().isRegularFile();
    }

    @Override
    public boolean isDirectory() {
      return MemoryEntry.this.getBasicFileAttributes().isDirectory();
    }

    @Override
    public boolean isSymbolicLink() {
      return MemoryEntry.this.getBasicFileAttributes().isSymbolicLink();
    }

    @Override
    public boolean isOther() {
      return MemoryEntry.this.getBasicFileAttributes().isOther();
    }

    @Override
    public long size() {
      return MemoryEntry.this.getBasicFileAttributes().size();
    }

    @Override
    public Object fileKey() {
      return MemoryEntry.this.getBasicFileAttributes().fileKey();
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
    public void initializeRoot() {
      this.hidden = true;
      this.system = true;
    }

    @Override
    public DosFileAttributes readAttributes() {
      MemoryEntry.this.checkAccess(AccessMode.READ);
      return this;
    }

    @Override
    void initializeFromSelf(FileAttributeView selfAttributes) {
      MemoryDosFileAttributeView other = (MemoryDosFileAttributeView) selfAttributes;
      this.readOnly = other.readOnly;
      this.hidden = other.hidden;
      this.system = other.system;
      this.archive = other.archive;
    }

    @Override
    public void setReadOnly(boolean value) {
      try (AutoRelease lock = MemoryEntry.this.writeLock()) {
        MemoryEntry.this.checkAccess(AccessMode.WRITE);
        this.readOnly = value;
      }
    }

    @Override
    public void setHidden(boolean value) {
      try (AutoRelease lock = MemoryEntry.this.writeLock()) {
        MemoryEntry.this.checkAccess(AccessMode.WRITE);
        this.hidden = value;
      }

    }

    @Override
    public void setSystem(boolean value) {
      try (AutoRelease lock = MemoryEntry.this.writeLock()) {
        MemoryEntry.this.checkAccess(AccessMode.WRITE);
        this.system = value;
      }

    }

    @Override
    public void setArchive(boolean value) {
      try (AutoRelease lock = MemoryEntry.this.writeLock()) {
        MemoryEntry.this.checkAccess(AccessMode.WRITE);
        this.archive = value;
      }

    }

    @Override
    public boolean isHidden() {
      try (AutoRelease lock = MemoryEntry.this.readLock()) {
        return this.hidden;
      }
    }

    @Override
    public boolean isArchive() {
      try (AutoRelease lock = MemoryEntry.this.readLock()) {
        return this.archive;
      }
    }

    @Override
    public boolean isSystem() {
      try (AutoRelease lock = MemoryEntry.this.readLock()) {
        return this.system;
      }
    }


    @Override
    public boolean isReadOnly() {
      try (AutoRelease lock = MemoryEntry.this.readLock()) {
        return this.readOnly;
      }
    }

  }

  abstract class MemoryFileOwnerAttributeView extends DelegatingFileAttributes implements FileOwnerAttributeView {

    private UserPrincipal owner;

    MemoryFileOwnerAttributeView(EntryCreationContext context) {
      if (context.user == null) {
        throw new NullPointerException("owner");
      }
      this.owner = context.user;
    }

    @Override
    public UserPrincipal getOwner() {
      try (AutoRelease lock = MemoryEntry.this.readLock()) {
        return this.owner;
      }
    }

    @Override
    public void setOwner(UserPrincipal owner) throws IOException {
      // TODO check same file system
      if (owner == null) {
        throw new IllegalArgumentException("owner must not be null");
      }
      try (AutoRelease lock = MemoryEntry.this.writeLock()) {
        MemoryEntry.this.checkAccess(AccessMode.WRITE);
        this.owner = owner;
      }
    }

  }

  class MemoryPosixFileAttributeView extends MemoryFileOwnerAttributeView implements PosixFileAttributeView, PosixFileAttributes {

    private GroupPrincipal group;
    private Set<PosixFilePermission> perms;

    MemoryPosixFileAttributeView(EntryCreationContext context) {
      super(context);
      if (context.group == null) {
        throw new NullPointerException("group");
      }
      this.group = context.group;
      this.perms = this.saveCopy(context.umask);
    }

    @Override
    public String name() {
      return FileAttributeViews.POSIX;
    }

    @Override
    void initializeFromSelf(FileAttributeView selfAttributes) {
      MemoryPosixFileAttributeView other = (MemoryPosixFileAttributeView) selfAttributes;
      this.group = other.group;
      this.perms = this.saveCopy(other.perms);
    }

    @Override
    public GroupPrincipal group() {
      try (AutoRelease lock = MemoryEntry.this.readLock()) {
        return this.group;
      }
    }

    @Override
    public UserPrincipal owner() {
      return this.getOwner();
    }

    @Override
    public void setGroup(GroupPrincipal group) throws IOException {
      // TODO check same file system
      if (group == null) {
        throw new IllegalArgumentException("group must not be null");
      }
      try (AutoRelease lock = MemoryEntry.this.writeLock()) {
        MemoryEntry.this.checkAccess(AccessMode.WRITE);
        this.group = group;
      }
    }

    @Override
    public PosixFileAttributes readAttributes() throws IOException {
      MemoryEntry.this.checkAccess(AccessMode.READ);
      return this;
    }

    @Override
    public Set<PosixFilePermission> permissions() {
      try (AutoRelease lock = MemoryEntry.this.readLock()) {
        return this.perms;
      }
    }

    private Set<PosixFilePermission> saveCopy(Set<PosixFilePermission> perms) {
      if (perms.isEmpty()) {
        return Collections.emptySet();
      } else if (perms.size() == 1) {
        return Collections.singleton(perms.iterator().next());
      } else {
        // make a defensive copy
        // does a type check on all elements
        // checks all elements for null
        // efficient storage
        Set<PosixFilePermission> copy = EnumSet.noneOf(PosixFilePermission.class);
        copy.addAll(perms);
        return copy;
      }
    }

    @Override
    public void setPermissions(Set<PosixFilePermission> perms) throws IOException {
      if (perms == null) {
        throw new IllegalArgumentException("permissions must not be null");
      }
      try (AutoRelease lock = MemoryEntry.this.writeLock()) {
        MemoryEntry.this.checkAccess(AccessMode.WRITE);
        this.perms = this.saveCopy(perms);
      }
    }

  }

  class MemoryUserDefinedFileAttributeView extends DelegatingFileAttributes implements UserDefinedFileAttributeView {

    // can potentially be null
    // try to delay instantiating as long as possible to keep per file object overhead minimal
    // protected by lock of memory entry
    private Map<String, byte[]> values;

    @Override
    public BasicFileAttributes readAttributes() throws IOException {
      throw new UnsupportedOperationException("readAttributes");
    }

    @Override
    void initializeFromSelf(FileAttributeView selfAttributes) {
      MemoryUserDefinedFileAttributeView other = (MemoryUserDefinedFileAttributeView) selfAttributes;
      if (other.values == null) {
        this.values = null;
      } else {
        this.values = new HashMap<>(other.values.size());
        for (Entry<String, byte[]> entry : other.values.entrySet()) {
          this.values.put(entry.getKey(), entry.getValue().clone());
        }
      }

    }

    private Map<String, byte[]> getValues() {
      if (this.values == null) {
        this.values = new HashMap<>(3);
      }
      return this.values;
    }

    @Override
    public String name() {
      return "user";
    }

    @Override
    public List<String> list() throws IOException {
      try (AutoRelease lock = MemoryEntry.this.readLock()) {
        if (this.values == null) {
          return Collections.emptyList();
        } else {
          Set<String> keys = this.getValues().keySet();
          return new ArrayList<String>(keys);
        }
      }
    }

    @Override
    public int size(String name) throws IOException {
      try (AutoRelease lock = MemoryEntry.this.readLock()) {
        byte[] value = this.getValue(name);
        return value.length;
      }
    }

    private byte[] getValue(String name) throws FileSystemException {
      if (name == null) {
        throw new NullPointerException("name is null");
      }
      if (this.values == null) {
        throw new FileSystemException(null, null, "attribute " + name + " not present");
      }
      byte[] value = this.values.get(name);
      if (value == null) {
        throw new FileSystemException(null, null, "attribute " + name + " not present");
      }
      return value;
    }

    @Override
    public int read(String name, ByteBuffer dst) throws IOException {
      try (AutoRelease lock = MemoryEntry.this.readLock()) {
        byte[] value = this.getValue(name);
        int remaining = dst.remaining();
        int required = value.length;
        if (remaining < required) {
          throw new FileSystemException(null, null, required + " bytes in buffer required but only " + remaining + " available");
        }
        int startPosition = dst.position();
        dst.put(value);
        int endPosition = dst.position();
        // TODO check if successful?
        return endPosition - startPosition;
      }
    }

    @Override
    public int write(String name, ByteBuffer src) throws IOException {
      try (AutoRelease lock = MemoryEntry.this.writeLock()) {
        if (name == null) {
          throw new NullPointerException("name is null");
        }
        if (src == null) {
          throw new NullPointerException("buffer is null");
        }
        int remaining = src.remaining();
        byte[] dst = new byte[remaining];
        int startPosition = src.position();
        src.get(dst);
        int endPosition = src.position();
        this.getValues().put(name, dst);
        // TODO check if successful?
        return endPosition - startPosition;
      }
    }

    @Override
    public void delete(String name) throws IOException {
      try (AutoRelease lock = MemoryEntry.this.writeLock()) {
        if (this.values != null) {
          if (name == null) {
            throw new NullPointerException("name is null");
          }
          this.values.remove(name);
        }
      }
    }
  }

}
