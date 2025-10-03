package com.github.marschall.memoryfilesystem;

import static com.github.marschall.memoryfilesystem.AutoReleaseLock.autoRelease;
import static java.nio.file.AccessMode.WRITE;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.CREATE_NEW;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.channels.FileChannel;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemException;
import java.nio.file.FileSystemLoopException;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.NotLinkException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.StandardOpenOption;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.DosFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileOwnerAttributeView;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.attribute.UserPrincipal;
import java.nio.file.spi.FileSystemProvider;
import java.text.Collator;
import java.time.Instant;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

class MemoryFileSystem extends FileSystem implements FileSystemContext {

  private static final Set<String> UNSUPPORTED_INITIAL_ATTRIBUTES;

  private static final Set<OpenOption> NO_OPEN_OPTIONS = Collections.emptySet();

  private static final FileAttribute<?>[] NO_FILE_ATTRIBUTES = new FileAttribute<?>[0];

  private final String key;

  private final String separator;

  private final MemoryFileSystemProvider provider;

  private final MemoryFileStore store;

  private final Iterable<FileStore> stores;

  private final ClosedFileSystemChecker checker;

  private volatile Map<Root, MemoryDirectory> roots;

  private volatile Map<String, Root> rootByKey;

  private final ConcurrentMap<AbsolutePath, List<MemoryWatchKey>> watchKeys;

  private volatile AbstractPath defaultPath;

  private final MemoryUserPrincipalLookupService userPrincipalLookupService;

  private final PathParser pathParser;

  private final EmptyPath emptyPath;

  // computes the file name to be stored of a file
  private final StringTransformer storeTransformer;

  // computes the look up key of a file name
  private final StringTransformer lookUpTransformer;

  private final Collator collator;

  private final Set<Class<? extends FileAttributeView>> additionalViews;

  private final Set<String> supportedFileAttributeViews;

  /**
   * Operations involving multiple paths (copy and move) need to use
   * lock ordering to avoid deadlocks. During the lock acquisition phase
   * no changes must be made that might affect the ordering. This involves
   * deleting and creating files (with a different capitalization) or links.
   * We assume deleting is less common so we block this operation.
   */
  private final ReadWriteLock pathOrderingLock;

  private final Set<PosixFilePermission> umask;

  private final TemporalUnit resolution;

  private final boolean supportFileChannelOnDirectory;

  static {
    Set<String> unsupported = new HashSet<>(3);
    unsupported.add("lastAccessTime");
    unsupported.add("creationTime");
    unsupported.add("lastModifiedTime");

    UNSUPPORTED_INITIAL_ATTRIBUTES = Collections.unmodifiableSet(unsupported);
  }

  MemoryFileSystem(String key, String separator, PathParser pathParser, MemoryFileSystemProvider provider, MemoryFileStore store,
          MemoryUserPrincipalLookupService userPrincipalLookupService, ClosedFileSystemChecker checker, StringTransformer storeTransformer,
          StringTransformer lookUpTransformer, Collator collator, Set<Class<? extends FileAttributeView>> additionalViews,
          Set<PosixFilePermission> umask, TemporalUnit resolution, boolean supportDirectoryFileChannelHack) {
    this.key = key;
    this.separator = separator;
    this.pathParser = pathParser;
    this.provider = provider;
    this.store = store;
    this.userPrincipalLookupService = userPrincipalLookupService;
    this.checker = checker;
    this.storeTransformer = storeTransformer;
    this.lookUpTransformer = lookUpTransformer;
    this.collator = collator;
    this.additionalViews = additionalViews;
    this.umask = umask;
    this.resolution = resolution;
    this.supportFileChannelOnDirectory = supportDirectoryFileChannelHack;
    this.stores = Collections.<FileStore>singletonList(store);
    this.watchKeys = new ConcurrentHashMap<>(1);
    this.emptyPath = new EmptyPath(this);
    this.supportedFileAttributeViews = this.buildSupportedFileAttributeViews(additionalViews);
    this.pathOrderingLock = new ReentrantReadWriteLock();
  }

  private Set<String> buildSupportedFileAttributeViews(Set<Class<? extends FileAttributeView>> additionalViews) {
    if (additionalViews.isEmpty()) {
      return Collections.singleton(FileAttributeViews.BASIC);
    } else {
      Set<String> views = new HashSet<>(additionalViews.size() + 2);
      views.add(FileAttributeViews.BASIC);
      for (Class<? extends FileAttributeView> viewClass : additionalViews) {
        if (FileOwnerAttributeView.class.isAssignableFrom(viewClass)) {
          views.add(FileAttributeViews.OWNER);
        }
        if (viewClass != FileOwnerAttributeView.class) {
          views.add(FileAttributeViews.mapAttributeView(viewClass));
        }
      }
      return Collections.unmodifiableSet(views);
    }
  }

  String getKey() {
    return this.key;
  }

  Set<PosixFilePermission> getUmask() {
    return this.umask;
  }

  EntryCreationContext newEntryCreationContext(Path path, FileAttribute<?>[] attributes) throws IOException {
    Set<PosixFilePermission> permissions = EnumSet.allOf(PosixFilePermission.class);

    for (FileAttribute<?> attribute: attributes) {
      if (attribute instanceof PosixFileAttributes) {
        permissions = ((PosixFileAttributes) attribute).permissions();
        break;
      }
    }

    UserPrincipal user = this.getCurrentUser();
    GroupPrincipal group = this.getGroupOf(user);
    return new EntryCreationContext(this.additionalViews, permissions, user, group, this, path);
  }

  private UserPrincipal getCurrentUser() {
    UserPrincipal currentUser = CurrentUser.get();
    if (currentUser != null) {
      return currentUser;
    } else {
      return this.userPrincipalLookupService.getDefaultUser();
    }
  }

  private GroupPrincipal getGroupOf(UserPrincipal user) throws IOException {
    // TODO is this always true?
    return this.userPrincipalLookupService.lookupPrincipalByGroupName(user.getName());
  }

  EmptyPath getEmptyPath() {
    return this.emptyPath;
  }


  /**
   * Sets the root directories.
   *
   * <p>This is a bit annoying.</p>
   *
   * @param rootDirectories the root directories, not {@code null},
   *  should not be modified, no defensive copy will be made
   */
  void setRootDirectories(Map<Root, MemoryDirectory> rootDirectories) {
    this.roots = rootDirectories;
    this.rootByKey = this.buildRootsByKey(rootDirectories.keySet());
  }

  private Map<String, Root> buildRootsByKey(Collection<Root> rootDirectories) {
    if (rootDirectories.isEmpty()) {
      throw new IllegalArgumentException("a file system root must be present");
    } else if (rootDirectories.size() == 1) {
      Root root = rootDirectories.iterator().next();
      String key = this.lookUpTransformer.transform(root.getKey());
      return Collections.singletonMap(key, root);
    } else {
      Map<String, Root> map = new HashMap<>(rootDirectories.size());
      for (Root root : rootDirectories) {
        String key = this.lookUpTransformer.transform(root.getKey());
        map.put(key, root);
      }
      return map;
    }
  }

  /**
   * Sets the current working directory.
   *
   * <p>This is used to resolve relative paths. This has to be set
   * after {@link #setRootDirectories(Map)}</p>.
   *
   * @param currentWorkingDirectory the default directory path
   */
  void setCurrentWorkingDirectory(String currentWorkingDirectory) {
    this.defaultPath = this.getPath(currentWorkingDirectory);
    if (!this.defaultPath.isAbsolute()) {
      throw new IllegalArgumentException("current working directory must be absolute");
    }
  }

  AbstractPath getDefaultPath() {
    return this.defaultPath;
  }


  FileChannel newFileChannel(AbstractPath path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
    this.checker.check();
    MemoryEntry entry = this.getEntry(path, options, attrs);
    if (entry instanceof MemoryFile) {
      return ((MemoryFile) entry).newChannel(options, path);
    }
    if (entry instanceof MemoryDirectory) {
      boolean isRead = options.contains(StandardOpenOption.READ);
      boolean isWrite = options.contains(StandardOpenOption.WRITE);
      if (this.supportFileChannelOnDirectory && isRead && !isWrite) {
        return new DirectoryChannel();
      }
    }
    throw new FileSystemException(path.toAbsolutePath().toString(), null, "is not a file");
  }

  private static Set<OpenOption> toOptionSet(Set<OpenOption> defaultOptions, OpenOption... options) {
    if (options == null || options.length == 0) {
      return defaultOptions;
    } else {
      Set<OpenOption> optionsSet = new HashSet<>(options.length);
      Collections.addAll(optionsSet, options);
      return optionsSet;
    }
  }

  InputStream newInputStream(AbstractPath path, OpenOption... options) throws IOException {
    this.checker.check();
    Set<OpenOption> optionsSet = toOptionSet(Collections.emptySet(), options);
    MemoryFile file = this.getFile(path, optionsSet);
    return file.newInputStream(optionsSet, path);
  }

  OutputStream newOutputStream(AbstractPath path, OpenOption... options) throws IOException {
    this.checker.check();
    Set<OpenOption> optionsSet = toOptionSet(DefaultOpenOptions.INSTANCE, options);
    MemoryFile file = this.getFile(path, optionsSet);
    return file.newOutputStream(optionsSet, path);
  }

  private static void checkSupportedInitialAttributes(FileAttribute<?>... attrs) {
    if (attrs != null) {
      for (FileAttribute<?> attribute : attrs) {
        String attributeName = attribute.name();
        if (UNSUPPORTED_INITIAL_ATTRIBUTES.contains(attributeName)) {
          throw new UnsupportedOperationException("'" + attributeName + "' not supported as initial attribute");
        }
      }
    }
  }

  private GetEntryResult getEntry(AbstractPath path, Set<? extends OpenOption> options, FileAttribute<?>[] attrs, boolean followSymLinks, Set<MemorySymbolicLink> encounteredSymlinks) throws IOException {

    FileAttribute<?>[] newAttributes = this.applyUmask(attrs); // TODO lazy
    AbstractPath absolutePath = (AbstractPath) path.toAbsolutePath().normalize();
    MemoryDirectory rootDirectory = this.getRootDirectory(absolutePath);
    if (absolutePath.isRoot()) {
      return new GetEntryResult(rootDirectory);
    }

    AbstractPath parent = (AbstractPath) absolutePath.getParent();
    return this.withWriteLockOnLastDo(rootDirectory, parent, followSymLinks, encounteredSymlinks, new MemoryDirectoryBlock<GetEntryResult>() {

      @Override
      public GetEntryResult value(MemoryDirectory directory) throws IOException {
        ElementPath elementPath = (ElementPath) absolutePath;
        boolean isCreateNew = options.contains(CREATE_NEW);
        String fileName = elementPath.getLastNameElement();
        String key = MemoryFileSystem.this.lookUpTransformer.transform(fileName);

        EntryCreationContext creationContext = MemoryFileSystem.this.newEntryCreationContext(absolutePath, newAttributes);
        if (isCreateNew) {
          MemoryFile file = this.createEntryOnAccess(path, newAttributes, directory, elementPath, creationContext);
          return new GetEntryResult(file);
        } else {
          directory.checkAccess(AccessMode.EXECUTE);
          MemoryEntry storedEntry = directory.getEntry(key);
          if (storedEntry == null) {
            boolean isCreate = options.contains(CREATE);
            if (isCreate) {
              MemoryFile file = this.createEntryOnAccess(path, newAttributes, directory, elementPath, creationContext);
              return new GetEntryResult(file);
            } else {
              throw new NoSuchFileException(path.toString());
            }
          }
          if (storedEntry instanceof MemorySymbolicLink && followSymLinks) {
            MemorySymbolicLink link = (MemorySymbolicLink) storedEntry;
            if (!encounteredSymlinks.add(link)) {
              throw new FileSystemLoopException(path.toString());
            }
            AbstractPath linkTarget = link.getTarget();
            if (linkTarget.isAbsolute()) {
              return new GetEntryResult(linkTarget);
            } else {
              return new GetEntryResult((AbstractPath) parent.resolve(linkTarget));
            }
          } else {
            return new GetEntryResult(storedEntry);
          }

        }
      }

      private MemoryFile createEntryOnAccess(AbstractPath path, FileAttribute<?>[] newAttributes, MemoryDirectory directory,
              ElementPath elementPath, EntryCreationContext creationContext) throws IOException {
        String fileName = elementPath.getLastNameElement();
        String key = MemoryFileSystem.this.lookUpTransformer.transform(fileName);
        String name = MemoryFileSystem.this.storeTransformer.transform(fileName);
        MemoryFile file = new MemoryFile(name, creationContext);
        checkSupportedInitialAttributes(newAttributes);
        AttributeAccessors.setAttributes(file, newAttributes);
        directory.checkAccess(WRITE);
        // will throw an exception if already present
        directory.addEntry(key, file, path);
        return file;
      }
    });
  }

  static final class GetEntryResult {

    final MemoryEntry entry;
    final AbstractPath linkTarget;

    GetEntryResult(MemoryEntry entry) {
      this.entry = entry;
      this.linkTarget = null;
    }

    GetEntryResult(AbstractPath linkTarget) {
      this.entry = null;
      this.linkTarget = linkTarget;
    }

  }

  private MemoryFile getFile(AbstractPath path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
    MemoryEntry file = this.getEntry(path, options);
    if (file instanceof MemoryFile) {
      return (MemoryFile) file;
    } else {
      throw new FileSystemException(path.toString().toString(), null, "is not a file");
    }
  }

  private MemoryEntry getEntry(AbstractPath path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
    boolean followSymLinks = Options.isFollowSymLinks(options);
    Set<MemorySymbolicLink> encounteredSymlinks;
    if (followSymLinks) {
      // we don't expect to encounter many symlinks so we initialize to a lower than the default value of 16
      // TODO optimized set
      encounteredSymlinks = new HashSet<>(4);
    } else {
      encounteredSymlinks = Collections.emptySet();
    }
    GetEntryResult result = this.getEntry(path, options, attrs, followSymLinks, encounteredSymlinks);
    while (result.entry == null) {
      result = this.getEntry(result.linkTarget, options, attrs, followSymLinks, encounteredSymlinks);
    }
    return result.entry;
  }

  private FileAttribute<?>[] applyUmask(FileAttribute<?>[] attributes) {
    if (this.umask.isEmpty()) {
      return attributes;
    }

    int length = attributes.length;
    boolean changed = false;
    FileAttribute<?>[] copy = null;

    for (int i = 0; i < length; i++) {
      FileAttribute<?> attribute = attributes[i];
      if (changed) {
        copy[i] = attribute;
        continue;
      }
      if ("posix:permissions".equals(attribute.name())) {
        copy = new FileAttribute[length];
        changed = true;
        if (i > 0) {
          System.arraycopy(attributes, 0, copy, 0, i - 1);
        }
        Set<PosixFilePermission> perms = (Set<PosixFilePermission>) attribute.value();
        Set<PosixFilePermission> newPerms = EnumSet.copyOf(perms);
        newPerms.removeAll(this.umask);
        copy[i] = PosixFilePermissions.asFileAttribute(newPerms);
      }
    }

    if (changed) {
      return copy;
    } else {
      // umask is set so we can reasonably sure posix permissions are supported
      // add permissions and set the to the umask
      FileAttribute<?>[] withPermissions = new FileAttribute[length + 1];
      System.arraycopy(attributes, 0, withPermissions, 0, length);
      EnumSet<PosixFilePermission> permissions = EnumSet.allOf(PosixFilePermission.class);
      permissions.removeAll(this.umask);
      withPermissions[length] = PosixFilePermissions.asFileAttribute(permissions);
      return withPermissions;
    }
  }

  DirectoryStream<Path> newDirectoryStream(AbstractPath abstractPath, Filter<? super Path> filter) throws IOException {
    return this.accessFileReading(abstractPath, true,  entry -> {
      if (!(entry instanceof MemoryDirectory)) {
        throw new NotDirectoryException(abstractPath.toString());
      }
      MemoryDirectory directory = (MemoryDirectory) entry;
      return directory.newDirectoryStream(abstractPath, filter);
    });
  }


  void createDirectory(AbstractPath path, FileAttribute<?>... attrs) throws IOException {
    FileAttribute<?>[] masked = this.applyUmask(attrs);
    this.createFile(path, name -> {
      EntryCreationContext context = MemoryFileSystem.this.newEntryCreationContext(path, masked);
      MemoryDirectory directory = new MemoryDirectory(name, context);
      AttributeAccessors.setAttributes(directory, masked);
      return directory;
    });
  }

  void createSymbolicLink(AbstractPath link, AbstractPath target, FileAttribute<?>... attrs) throws IOException {

    FileAttribute<?>[] masked = this.applyUmask(attrs);
    this.createFile(link, name -> {
      EntryCreationContext context = MemoryFileSystem.this.newEntryCreationContext(link, masked);
      MemorySymbolicLink symbolicLink = new MemorySymbolicLink(name, target, context);
      AttributeAccessors.setAttributes(symbolicLink, masked);
      return symbolicLink;
    });
  }

  void createLink(AbstractPath link, AbstractPath existing) throws IOException {
    MemoryFile existingFile = this.getFile(existing);
    if (existingFile == null) {
      throw new FileSystemException(link.toString(), existing.toString(), "hard links are only supported for regular files");
    }
    this.createFile(link, name -> {
      EntryCreationContext context = MemoryFileSystem.this.newEntryCreationContext(link, NO_FILE_ATTRIBUTES);
      return existingFile.createLink(name, context);
    });
  }

  boolean isSameFile(AbstractPath path, AbstractPath path2) throws IOException {
    MemoryFile file = this.getFile(path);
    if (file == null) {
      return false;
    }

    MemoryFile file2 = this.getFile(path2);
    if (file2 == null) {
      return false;
    }

    return file.hasSameInodeAs(file2);
  }

  private MemoryFile getFile(AbstractPath existing) throws IOException {
    return this.accessFileReading(existing, true, entry -> {
      if (entry instanceof MemoryFile) {
        return (MemoryFile) entry;
      }
      return null;
    });
  }

  private void createFile(AbstractPath path, MemoryEntryCreator creator) throws IOException {
    this.checker.check();
    AbstractPath absolutePath = (AbstractPath) path.toAbsolutePath().normalize();
    if (absolutePath.isRoot()) {
      throw new FileAlreadyExistsException(path.toString(), null, "can not create root");
    }
    ElementPath elementPath = (ElementPath) absolutePath;

    this.accessDirectoryWriting((AbstractPath) elementPath.getParent(), true, directory -> {
      String name = MemoryFileSystem.this.storeTransformer.transform(elementPath.getLastNameElement());
      MemoryEntry newEntry = creator.create(name);
      String key = MemoryFileSystem.this.lookUpTransformer.transform(newEntry.getOriginalName());
      directory.checkAccess(WRITE);
      directory.addEntry(key, newEntry, path);
      return null;
    });

  }

  AbstractPath toRealPath(AbstractPath abstractPath, LinkOption... options) throws IOException  {
    this.checker.check();
    AbstractPath absolutePath = (AbstractPath) abstractPath.toAbsolutePath().normalize();
    boolean followSymLinks = Options.isFollowSymLinks(options);
    Set<MemorySymbolicLink> encounteredSymlinks;
    if (followSymLinks) {
      // we don't expect to encounter many symlinks so we initialize to a lower than the default value of 16
      // TODO optimized set
      encounteredSymlinks = new HashSet<>(4);
    } else {
      encounteredSymlinks = Collections.emptySet();
    }
    MemoryDirectory root = this.getRootDirectory(absolutePath);
    return this.toRealPath(root, absolutePath, encounteredSymlinks, followSymLinks);
  }


  private AbstractPath toRealPath(MemoryDirectory root, AbstractPath path, Set<MemorySymbolicLink> encounteredLinks, boolean followSymLinks) throws IOException {
    if (path.isRoot()) {
      return (AbstractPath) path.getRoot();
    } else if (path instanceof ElementPath) {

      ElementPath elementPath = (ElementPath) path;
      List<String> nameElements = elementPath.getNameElements();
      int pathElementCount = nameElements.size();
      List<String> realPath = new ArrayList<>(pathElementCount);
      List<AutoRelease> locks = new ArrayList<>(pathElementCount + 1);
      try {
        locks.add(root.readLock());
        MemoryDirectory parent = root;
        for (int i = 0; i < pathElementCount; ++i) {
          String fileName = nameElements.get(i);
          String key = this.lookUpTransformer.transform(fileName);
          MemoryEntry current = parent.getEntryOrException(key, path);
          locks.add(current.readLock());
          realPath.add(current.getOriginalName());

          if (followSymLinks && current instanceof MemorySymbolicLink) {
            MemorySymbolicLink link = (MemorySymbolicLink) current;
            if (!encounteredLinks.add(link)) {
              throw new FileSystemLoopException(path.toString());
            }
            Path newLookUpPath = this.resolveSymlink(path, link, nameElements, pathElementCount, i);
            return this.toRealPath(root, (AbstractPath) newLookUpPath.normalize(), encounteredLinks, followSymLinks);
          }

          if (current instanceof MemoryDirectory) {
            parent = (MemoryDirectory) current;
          } else if (i < (pathElementCount - 1)) {
            // all except last must be a directory
            throw new NotDirectoryException(path.toString());
          }

        }
      } finally {
        for (int i = locks.size() - 1; i >= 0; --i) {
          AutoRelease lock = locks.get(i);
          lock.close();
        }
      }
      return AbstractPath.createAbsolute(this, (Root) path.getRoot(), realPath);

    } else {
      throw new IllegalArgumentException("unknown path type" + path);
    }
  }

  void checkAccess(AbstractPath path, AccessMode... modes) throws IOException {
    this.checker.check();
    // java.nio.file.spi.FileSystemProvider#checkAccess(Path, AccessMode...)
    // says we should follow symbolic links
    this.accessFileReading(path, true, entry -> {
      entry.checkAccess(modes);
      return null;
    });
  }

  boolean isDirectory(AbstractPath path) {
    try {
      return this.accessFileReading(path, false, entry -> entry.isDirectory());
    } catch (IOException e) {
      // use for URI and URL construction, we do not want to throw in case of non-existing files
      return false;
    }
  }

  boolean exists(AbstractPath path, LinkOption... options) {
    try {
      return this.accessFileReadingIfExists(path, Options.isFollowSymLinks(options), entry -> true).orElse(Boolean.FALSE);
    } catch (IOException e) {
      return false;
    }
  }

  <A extends BasicFileAttributes> A readAttributes(AbstractPath path, Class<A> type, LinkOption... options) throws IOException {
    this.checker.check();
    return this.accessFileReading(path, Options.isFollowSymLinks(options), entry -> entry.readAttributes(type));
  }

  <A extends BasicFileAttributes> A readAttributesIfExists(AbstractPath path, Class<A> type, LinkOption... options) throws IOException {
    this.checker.check();
    Optional<A> result = this.accessFileReadingIfExists(path, Options.isFollowSymLinks(options), entry -> entry.readAttributes(type));
    if (result.isPresent()) {
      return result.get();
    } else {
      return null;
    }
  }

  <V extends FileAttributeView> V getLazyFileAttributeView(AbstractPath path, Class<V> type, LinkOption... options) {
    if (type != BasicFileAttributeView.class && !this.additionalViews.contains(type)) {
      // unsupported view, specification requires null
      return null;
    }
    InvocationHandler handler = new LazyFileAttributeView(path, type, options);
    Object proxy = Proxy.newProxyInstance(MemoryFileSystem.class.getClassLoader(), new Class<?>[]{type}, handler);
    return type.cast(proxy);
  }

  <V extends FileAttributeView> V getFileAttributeView(AbstractPath path, Class<V> type, LinkOption... options) throws IOException {
    return this.accessFileReading(path, Options.isFollowSymLinks(options), entry -> entry.getFileAttributeView(type));
  }

  Map<String, Object> readAttributes(AbstractPath path, String attributes, LinkOption... options) throws IOException {
    this.checker.check();
    return this.accessFileReading(path, Options.isFollowSymLinks(options), entry -> AttributeAccessors.readAttributes(entry, attributes));
  }

  void setAttribute(AbstractPath path, String attribute, Object value, LinkOption... options) throws IOException {
    this.checker.check();
    this.accessFileWriting(path, Options.isFollowSymLinks(options), entry -> {
      // TODO write lock?
      AttributeAccessors.setAttribute(entry, attribute, value);
      return null;
    });
  }

  private <R> R accessFileReading(AbstractPath path, boolean followSymLinks, MemoryEntryBlock<? extends R> callback) throws IOException {
    return this.accessFile(path, followSymLinks, LockType.READ, callback);
  }

  private <R> R accessFileWriting(AbstractPath path, boolean followSymLinks, MemoryEntryBlock<? extends R> callback) throws IOException {
    return this.accessFile(path, followSymLinks, LockType.WRITE, callback);
  }

  private <R> Optional<R> accessFileReadingIfExists(AbstractPath path, boolean followSymLinks, MemoryEntryBlock<? extends R> callback) throws IOException {
    return this.accessFileIfExists(path, followSymLinks, LockType.READ, callback);
  }

  private <R> Optional<R> accessFileWritingIfExists(AbstractPath path, boolean followSymLinks, MemoryEntryBlock<? extends R> callback) throws IOException {
    return this.accessFileIfExists(path, followSymLinks, LockType.WRITE, callback);
  }

  private <R> R accessFile(AbstractPath path, boolean followSymLinks, LockType lockType, MemoryEntryBlock<? extends R> callback) throws IOException {
    this.checker.check();
    AbstractPath absolutePath = (AbstractPath) path.toAbsolutePath().normalize();
    MemoryDirectory root = this.getRootDirectory(absolutePath);
    Set<MemorySymbolicLink> encounteredSymlinks;
    if (followSymLinks) {
      encounteredSymlinks = new HashSet<>(4);
    } else {
      encounteredSymlinks = Collections.emptySet();
    }
    return this.withLockDo(root, absolutePath, encounteredSymlinks, followSymLinks, lockType, callback);
  }

  private <R> Optional<R> accessFileIfExists(AbstractPath path, boolean followSymLinks, LockType lockType, MemoryEntryBlock<? extends R> callback) throws IOException {
    this.checker.check();
    AbstractPath absolutePath = (AbstractPath) path.toAbsolutePath().normalize();
    MemoryDirectory root = this.getRootDirectory(absolutePath);
    Set<MemorySymbolicLink> encounteredSymlinks;
    if (followSymLinks) {
      encounteredSymlinks = new HashSet<>(4);
    } else {
      encounteredSymlinks = Collections.emptySet();
    }
    return this.withLockDoIfExists(root, absolutePath, encounteredSymlinks, followSymLinks, lockType, callback);
  }


  private <R> R withWriteLockOnLastDo(MemoryDirectory root, AbstractPath path, boolean followSymLinks, MemoryDirectoryBlock<R> callback) throws IOException {
    Set<MemorySymbolicLink> encounteredSymlinks;
    if (followSymLinks) {
      encounteredSymlinks = new HashSet<>(4);
    } else {
      encounteredSymlinks = Collections.emptySet();
    }
    return this.withWriteLockOnLastDo(root, path, followSymLinks, encounteredSymlinks, callback);
  }

  private <R> R withWriteLockOnLastDo(MemoryDirectory root, AbstractPath path, boolean followSymLinks, Set<MemorySymbolicLink> encounteredSymlinks, final MemoryDirectoryBlock<R> callback) throws IOException {
    return this.withLockDo(root, path, encounteredSymlinks, followSymLinks, LockType.WRITE, entry -> {
      if (!(entry instanceof MemoryDirectory)) {
        throw new NotDirectoryException(path.toString());
      }
      return callback.value((MemoryDirectory) entry);
    });
  }

  private <R> R accessDirectoryWriting(AbstractPath path, boolean followSymLinks, MemoryDirectoryBlock<R> callback) throws IOException {
    return this.accessFileWriting(path, followSymLinks, entry -> {
      if (!(entry instanceof MemoryDirectory)) {
        throw new NotDirectoryException(path.toString());
      }
      return callback.value((MemoryDirectory) entry);
    });
  }

  private <R> Optional<R> accessDirectoryWritingIfExists(AbstractPath path, boolean followSymLinks, MemoryDirectoryBlock<R> callback) throws IOException {
    return this.accessFileWritingIfExists(path, followSymLinks, entry -> {
      if (!(entry instanceof MemoryDirectory)) {
        throw new NotDirectoryException(path.toString());
      }
      return callback.value((MemoryDirectory) entry);
    });
  }

  private <R> R withReadLockDo(MemoryDirectory root, AbstractPath path, boolean followSymLinks, MemoryEntryBlock<? extends R> callback) throws IOException {
    Set<MemorySymbolicLink> encounteredSymlinks;
    if (followSymLinks) {
      encounteredSymlinks = new HashSet<>(4);
    } else {
      encounteredSymlinks = Collections.emptySet();
    }
    return this.withLockDo(root, path, encounteredSymlinks, followSymLinks, LockType.READ, callback);
  }


  private <R> R withLockDo(MemoryDirectory root, AbstractPath path, Set<MemorySymbolicLink> encounteredLinks, boolean followSymLinks, LockType lockType, MemoryEntryBlock<? extends R> callback) throws IOException {
    if (path.isRoot()) {
      try (AutoRelease lock = root.lock(lockType)) {
        return callback.value(root);
      }
    } else if (path instanceof ElementPath) {
      R result = null;
      Path newLookUpPath = null;

      ElementPath elementPath = (ElementPath) path;
      List<String> nameElements = elementPath.getNameElements();
      int pathElementCount = nameElements.size();
      List<AutoRelease> locks = new ArrayList<>(pathElementCount + 1);
      try {
        locks.add(root.readLock());
        MemoryDirectory parent = root;
        for (int i = 0; i < pathElementCount; ++i) {
          String fileName = nameElements.get(i);
          String key = this.lookUpTransformer.transform(fileName);
          MemoryEntry current = parent.getEntryOrException(key, path);
          boolean isLast = i == pathElementCount - 1;
          if (isLast) {
            locks.add(current.lock(lockType));
          } else {
            locks.add(current.readLock());
          }

          if (followSymLinks && current instanceof MemorySymbolicLink) {
            MemorySymbolicLink link = (MemorySymbolicLink) current;
            if (!encounteredLinks.add(link)) {
              throw new FileSystemLoopException(path.toString());
            }
            newLookUpPath = this.resolveSymlink(path, link, nameElements, pathElementCount, i);
            break;
          }

          if (isLast) {
            result = callback.value(current);
          } else if (current instanceof MemoryDirectory) {
            parent = (MemoryDirectory) current;
          } else {
            throw new NotDirectoryException(path.toString());
          }

        }
      } finally {
        for (int i = locks.size() - 1; i >= 0; --i) {
          AutoRelease lock = locks.get(i);
          lock.close();
        }
      }
      if (newLookUpPath == null) {
        return result;
      } else {
        return this.withLockDo(root, (AbstractPath) newLookUpPath.normalize(), encounteredLinks, followSymLinks, lockType, callback);
      }

    } else {
      throw new IllegalArgumentException("unknown path type" + path);
    }
  }

  private <R> Optional<R> withLockDoIfExists(MemoryDirectory root, AbstractPath path, Set<MemorySymbolicLink> encounteredLinks, boolean followSymLinks, LockType lockType, MemoryEntryBlock<? extends R> callback) throws IOException {
    if (path.isRoot()) {
      try (AutoRelease lock = root.lock(lockType)) {
        return Optional.of(callback.value(root));
      }
    } else if (path instanceof ElementPath) {
      Optional<R> result = null;
      Path newLookUpPath = null;

      ElementPath elementPath = (ElementPath) path;
      List<String> nameElements = elementPath.getNameElements();
      int pathElementCount = nameElements.size();
      List<AutoRelease> locks = new ArrayList<>(pathElementCount + 1);
      try {
        locks.add(root.readLock());
        MemoryDirectory parent = root;
        for (int i = 0; i < pathElementCount; ++i) {
          String fileName = nameElements.get(i);
          String key = this.lookUpTransformer.transform(fileName);
          MemoryEntry current = parent.getEntry(key);
          if (current == null) {
            result = Optional.empty();
            break;
          }
          boolean isLast = i == pathElementCount - 1;
          if (isLast) {
            locks.add(current.lock(lockType));
          } else {
            locks.add(current.readLock());
          }

          if (followSymLinks && current instanceof MemorySymbolicLink) {
            MemorySymbolicLink link = (MemorySymbolicLink) current;
            if (!encounteredLinks.add(link)) {
              throw new FileSystemLoopException(path.toString());
            }
            newLookUpPath = this.resolveSymlink(path, link, nameElements, pathElementCount, i);
            break;
          }

          if (isLast) {
            result = Optional.of(callback.value(current));
          } else if (current instanceof MemoryDirectory) {
            parent = (MemoryDirectory) current;
          } else {
            throw new NotDirectoryException(path.toString());
          }

        }
      } finally {
        for (int i = locks.size() - 1; i >= 0; --i) {
          AutoRelease lock = locks.get(i);
          lock.close();
        }
      }
      if (newLookUpPath == null) {
        return result;
      } else {
        return this.withLockDoIfExists(root, (AbstractPath) newLookUpPath.normalize(), encounteredLinks, followSymLinks, lockType, callback);
      }

    } else {
      throw new IllegalArgumentException("unknown path type" + path);
    }
  }

  private Path resolveSymlink(AbstractPath path, MemorySymbolicLink link, List<String> nameElements, int pathElementCount, int currentNameElementIndex) {
    Path newLookUpPath;
    Path symLinkTarget = link.getTarget();
    if (symLinkTarget.isAbsolute()) {
      newLookUpPath = symLinkTarget;
      // we can't return and have to keep appending
    } else {
      // start an entire new lookup
      // start from the root
      newLookUpPath = path.getRoot();
      for (int j = 0; j < currentNameElementIndex; ++j) {
        // append everything we traversed so far
        newLookUpPath = newLookUpPath.resolve(nameElements.get(j));
      }
      // append the relative symlink
      newLookUpPath = newLookUpPath.resolve(symLinkTarget);
    }
    // append the elements not yet traversed
    for (int j = currentNameElementIndex + 1; j < pathElementCount; ++j) {
      newLookUpPath = newLookUpPath.resolve(nameElements.get(j));
    }
    return newLookUpPath;
  }

  private MemoryDirectory getRootDirectory(AbstractPath path) throws IOException {
    Path root = path.getRoot();
    MemoryDirectory directory = this.roots.get(root);
    if (directory == null) {
      throw new NoSuchFileException(path.toString(), null, "the root doesn't exist");
    }
    return directory;
  }

  void copyOrMove(AbstractPath source, AbstractPath target, TwoPathOperation operation, CopyOption... options) throws IOException {
    try (AutoRelease autoRelease = autoRelease(this.pathOrderingLock.writeLock())) {

      EndPointCopyContext sourceContext = this.buildEndpointCopyContext(source);
      EndPointCopyContext targetContext = this.buildEndpointCopyContext(target);

      int order = this.orderPaths(sourceContext, targetContext);
      CopyContext copyContext = buildCopyContext(sourceContext, targetContext, operation, options, order);

      AbstractPath firstParent = copyContext.first.parent;
      AbstractPath secondParent = copyContext.second.parent;
      if (firstParent == null && secondParent == null) {
        // both of the involved paths is a root
        // simply ignore
        return;
      }

      if (firstParent == null || secondParent == null) {
        // only one of the involved paths is a root
        throw new FileSystemException(toStringOrNull(firstParent), toStringOrNull(secondParent), "can't copy or move root directory");
      }

      MemoryDirectory firstRoot = this.getRootDirectory(copyContext.first.path);
      MemoryDirectory secondRoot = this.getRootDirectory(copyContext.second.path);

      this.withWriteLockOnLastDo(firstRoot, firstParent, copyContext.firstFollowSymLinks, firstDirectory -> {
        MemoryFileSystem.this.withWriteLockOnLastDo(secondRoot, secondParent, copyContext.secondFollowSymLinks, secondDirectory -> {
          handleTwoPathOperation(copyContext, firstDirectory, secondDirectory);
          return null;
        });
        return null;
      });

    }
  }

  static void copyOrMoveBetweenFileSystems(MemoryFileSystem sourceFileSystem, MemoryFileSystem targetFileSystem, AbstractPath source, AbstractPath target, TwoPathOperation operation, CopyOption... options) throws IOException {
    EndPointCopyContext sourceContext = sourceFileSystem.buildEndpointCopyContext(source);
    EndPointCopyContext targetContext = targetFileSystem.buildEndpointCopyContext(target);

    int order = orderFileSystems(sourceContext, targetContext);
    CopyContext copyContext = buildCopyContext(sourceContext, targetContext, operation, options, order);
    AbstractPath firstParent = copyContext.first.parent;
    AbstractPath secondParent = copyContext.second.parent;
    if (firstParent == null || secondParent == null) {
      throw new FileSystemException(toStringOrNull(firstParent), toStringOrNull(secondParent), "can't move ore copy the file system root");
    }

    MemoryDirectory firstRoot = sourceFileSystem.getRootDirectory(copyContext.first.path);
    MemoryDirectory secondRoot = targetFileSystem.getRootDirectory(copyContext.second.path);
    copyContext.first.path.getMemoryFileSystem().withWriteLockOnLastDo(firstRoot, firstParent, copyContext.firstFollowSymLinks, firstDirectory -> {
      copyContext.second.path.getMemoryFileSystem().withWriteLockOnLastDo(secondRoot, secondParent, copyContext.secondFollowSymLinks, secondDirectory -> {
        handleTwoPathOperation(copyContext, firstDirectory, secondDirectory);
        return null;
      });
      return null;
    });
  }

  private static String toStringOrNull(AbstractPath path) {
    return Objects.toString(path, null);
  }

  private int orderPaths(EndPointCopyContext source, EndPointCopyContext target) {
    int parentOrder;
    if (source.parent == null) {
      parentOrder = target.parent == null ? 0 : -1;
    } else if (target.parent == null) {
      parentOrder = 1;
    } else {
      parentOrder = source.parent.compareTo(target.parent);
    }
    if (parentOrder != 0) {
      return parentOrder;
    } else {
      if (source.elementName == null) {
        return target.elementName == null ? 0 : -1;
      } else if (target.elementName == null) {
        return 1;
      } else {
        return MemoryFileSystem.this.collator.compare(source.elementName, target.elementName);
      }
    }
  }

  private static int orderFileSystems(EndPointCopyContext source, EndPointCopyContext target) {
    MemoryFileSystem sourceFileSystem = source.path.getMemoryFileSystem();
    MemoryFileSystem targetFileSystem = target.path.getMemoryFileSystem();
    String sourceKey = sourceFileSystem.getKey();
    String targetKey = targetFileSystem.getKey();
    int comparison = sourceKey.compareTo(targetKey);
    if (comparison != 0) {
      return comparison;
    } else {
      throw new AssertionError("the two file system keys " + sourceKey + " and " + targetKey + " compare equal.");
    }
  }

  private EndPointCopyContext buildEndpointCopyContext(AbstractPath path) {
    AbstractPath absolutePath = (AbstractPath) path.toAbsolutePath().normalize();
    if (absolutePath.isRoot()) {
      return new EndPointCopyContext(absolutePath, null, null);
    } else {
      ElementPath elementPath = (ElementPath) absolutePath;
      AbstractPath parent = (AbstractPath) elementPath.getParent();
      String elementName = elementPath.getLastNameElement();
      return new EndPointCopyContext(elementPath, parent, elementName);
    }
  }

  static final class EndPointCopyContext {

    final AbstractPath path;
    final AbstractPath parent;
    final String elementName;

    EndPointCopyContext(AbstractPath path, AbstractPath parent, String elementName) {
      this.path = path;
      this.parent = parent;
      this.elementName = elementName;
    }

  }

  private static CopyContext buildCopyContext(EndPointCopyContext source, EndPointCopyContext target, TwoPathOperation operation, CopyOption[] options, int order) {
    boolean followSymLinks = Options.isFollowSymLinks(options);
    boolean replaceExisting = Options.isReplaceExisting(options);
    boolean copyAttributes = Options.isCopyAttributes(options);

    EndPointCopyContext first;
    EndPointCopyContext second;
    boolean firstFollowSymLinks;
    boolean secondFollowSymLinks;
    boolean inverted;
    if (order <= 0) {
      first = source;
      second = target;
      firstFollowSymLinks = followSymLinks;
      secondFollowSymLinks = false;
      inverted = false;
    } else {
      first = target;
      second = source;
      firstFollowSymLinks = false;
      secondFollowSymLinks = followSymLinks;
      inverted = true;
    }

    return new CopyContext(operation, source, target, first, second, firstFollowSymLinks, secondFollowSymLinks,
            inverted, replaceExisting, copyAttributes);
  }

  static final class CopyContext {

    final EndPointCopyContext source;
    final EndPointCopyContext target;
    final EndPointCopyContext first;
    final EndPointCopyContext second;
    final boolean firstFollowSymLinks;
    final boolean secondFollowSymLinks;
    private final boolean inverted;
    final boolean replaceExisting;
    final boolean copyAttributes;
    final TwoPathOperation operation;

    CopyContext(TwoPathOperation operation, EndPointCopyContext source, EndPointCopyContext target, EndPointCopyContext first, EndPointCopyContext second,
            boolean firstFollowSymLinks, boolean secondFollowSymLinks, boolean inverted, boolean replaceExisting, boolean copyAttributes) {
      this.operation = operation;
      this.source = source;
      this.target = target;
      this.first = first;
      this.second = second;
      this.firstFollowSymLinks = firstFollowSymLinks;
      this.secondFollowSymLinks = secondFollowSymLinks;
      this.inverted = inverted;
      this.replaceExisting = replaceExisting;
      this.copyAttributes = copyAttributes;
    }

    boolean isSourceFollowSymLinks() {
      if (this.inverted) {
        return this.secondFollowSymLinks;
      } else {
        return this.firstFollowSymLinks;
      }
    }

    MemoryDirectory getSourceParent(MemoryDirectory firstDirectory, MemoryDirectory secondDirectory) {
      if (!this.inverted) {
        return firstDirectory;
      } else {
        return secondDirectory;
      }
    }

    MemoryDirectory getTargetParent(MemoryDirectory firstDirectory, MemoryDirectory secondDirectory) {
      if (!this.inverted) {
        return secondDirectory;
      } else {
        return firstDirectory;
      }
    }

  }

  void delete(AbstractPath abstractPath) throws IOException {
    try (AutoRelease autoRelease = autoRelease(this.pathOrderingLock.readLock())) {
      AbstractPath absolutePath = (AbstractPath) abstractPath.toAbsolutePath().normalize();
      if (absolutePath.isRoot()) {
        throw new FileSystemException(abstractPath.toString(), null, "can not delete root");
      }
      ElementPath elementPath = (ElementPath) absolutePath;

      AbstractPath parent = (AbstractPath) elementPath.getParent();
      this.accessDirectoryWriting(parent, true, directory -> {
        String fileName = elementPath.getLastNameElement();
        String key = MemoryFileSystem.this.lookUpTransformer.transform(fileName);
        MemoryEntry child = directory.getEntryOrException(key, abstractPath);
        try (AutoRelease lock = child.writeLock()) {
          if (child instanceof MemoryDirectory) {
            MemoryDirectory childDirectory = (MemoryDirectory) child;
            childDirectory.checkEmpty(abstractPath);
          }
          if (child instanceof MemoryFile) {
            MemoryFile file = (MemoryFile) child;
            if (file.openCount() > 0) {
              throw new FileSystemException(abstractPath.toString(), null, "file still open");
            }
            file.markForDeletion();
          }
          directory.checkAccess(WRITE);
          directory.removeEntry(key);
        }
        return null;
      });
    }
  }

  boolean deleteIfExists(AbstractPath abstractPath) throws IOException {
    try (AutoRelease autoRelease = autoRelease(this.pathOrderingLock.readLock())) {
      AbstractPath absolutePath = (AbstractPath) abstractPath.toAbsolutePath().normalize();
      if (absolutePath.isRoot()) {
        throw new FileSystemException(abstractPath.toString(), null, "can not delete root");
      }
      ElementPath elementPath = (ElementPath) absolutePath;

      AbstractPath parent = (AbstractPath) elementPath.getParent();
      return this.accessDirectoryWritingIfExists(parent, true, directory -> {
        String fileName = elementPath.getLastNameElement();
        String key = MemoryFileSystem.this.lookUpTransformer.transform(fileName);
        MemoryEntry child = directory.getEntry(key);
        if (child != null) {
          try (AutoRelease lock = child.writeLock()) {
            if (child instanceof MemoryDirectory) {
              MemoryDirectory childDirectory = (MemoryDirectory) child;
              childDirectory.checkEmpty(abstractPath);
            }
            if (child instanceof MemoryFile) {
              MemoryFile file = (MemoryFile) child;
              if (file.openCount() > 0) {
                throw new FileSystemException(abstractPath.toString(), null, "file still open");
              }
              file.markForDeletion();
            }
            directory.checkAccess(WRITE);
            directory.removeEntry(key);
          }
          return true;
        } else {
          return false;
        }
      }).orElse(Boolean.FALSE);
    }
  }

  @Override
  public FileSystemProvider provider() {
    this.checker.check();
    return this.provider;
  }

  @Override
  @javax.annotation.PreDestroy
  @jakarta.annotation.PreDestroy
  public void close() {
    // avoid throws IOException
    // https://github.com/marschall/memoryfilesystem/issues/76
    if (this.checker.close()) {
      // closing twice is explicitly allowed by the contract
      this.checker.close();
      this.provider.close(this);
    }
  }

  @Override
  public boolean isOpen() {
    return this.checker.isOpen();
  }

  @Override
  public boolean isReadOnly() {
    this.checker.check();
    return this.store.isReadOnly();
  }

  @Override
  public String getSeparator() {
    this.checker.check();
    return this.separator;
  }


  @Override
  public Iterable<Path> getRootDirectories() {
    this.checker.check();
    // this is fine because the iterator does not support modification
    return (Iterable<Path>) ((Object) this.roots.keySet());
  }

  @Override
  public Iterable<FileStore> getFileStores() {
    this.checker.check();
    return this.stores;
  }

  @Override
  public Set<String> supportedFileAttributeViews() {
    this.checker.check();
    return this.supportedFileAttributeViews;
  }

  @Override
  public AbstractPath getPath(String first, String... more) {
    this.checker.check();
    // TODO check for maximum length
    return this.pathParser.parse(this.rootByKey, first, more);
  }

  AbstractPath getPathFromUri(String uri) {
    this.checker.check();
    // TODO check for maximum length
    return this.pathParser.parseUri(this.rootByKey, uri);
  }

  @Override
  public PathMatcher getPathMatcher(String syntaxAndPattern) {
    this.checker.check();
    int colonIndex = syntaxAndPattern.indexOf(':');
    if (colonIndex <= 0 || colonIndex == syntaxAndPattern.length() - 1) {
      throw new IllegalArgumentException("syntaxAndPattern must have form \"syntax:pattern\" but was \"" + syntaxAndPattern + "\"");
    }

    String syntax = syntaxAndPattern.substring(0, colonIndex);
    String pattern = syntaxAndPattern.substring(colonIndex + 1);
    if (syntax.equalsIgnoreCase(GlobPathMatcher.name())) {
      return this.pathParser.transpileGlob(pattern, this.lookUpTransformer.getRegexFlags());
    }
    if (syntax.equalsIgnoreCase(RegexPathMatcher.name())) {
      return this.pathParser.compileRegex(pattern, this.lookUpTransformer.getRegexFlags());
    }

    throw new UnsupportedOperationException("unsupported syntax \"" + syntax + "\"");
  }

  @Override
  public MemoryUserPrincipalLookupService getUserPrincipalLookupService() {
    this.checker.check();
    return this.userPrincipalLookupService;
  }

  @Override
  public WatchService newWatchService() {
    this.checker.check();
    // TODO make configurable
    if (true) {
      throw new UnsupportedOperationException();
    }
    return new MemoryFileSystemWatchService(this);
  }

  void register(MemoryWatchKey watchKey) {
    this.checker.check();
    AbsolutePath absolutePath = (AbsolutePath) watchKey.watchable().toAbsolutePath();
    List<MemoryWatchKey> keys = this.watchKeys.get(absolutePath);
    if (keys == null) {
      keys = new CopyOnWriteArrayList<>();
      List<MemoryWatchKey> previous = this.watchKeys.putIfAbsent(absolutePath, keys);
      if (previous != null) {
        keys = previous;
      }
    }
    keys.add(watchKey);
  }

  FileStore getFileStore() {
    return this.store;
  }

  Collator getCollator() {
    return this.collator;
  }

  @Override
  public Instant truncate(Instant instant) {
    if (instant == null || this.resolution == null) {
      return instant;
    }
    return instant.truncatedTo(this.resolution);
  }

  @Override
  public UserPrincipal getDefaultUser() {
    return this.getUserPrincipalLookupService().getDefaultUser();
  }

  boolean isHidden(AbstractPath abstractPath) throws IOException {
    if (this.supportedFileAttributeViews.contains(FileAttributeViews.POSIX)) {
      return this.accessFileReading(abstractPath, false, entry -> {
        // Posix seems to check only the file name
        String originalName = entry.getOriginalName();
        return !originalName.isEmpty() && originalName.charAt(0) == '.';
      });
    } else if (this.supportedFileAttributeViews.contains(FileAttributeViews.DOS)) {
      return this.readAttributes(abstractPath, DosFileAttributes.class).isHidden();
    } else {
      return false;
    }
  }

  private MemoryEntry copyEntry(Path absoluteTargetPath, MemoryEntry sourceEntry, String targetElementName) throws IOException {
    if (sourceEntry instanceof MemoryFile) {
      MemoryFile sourceFile = (MemoryFile) sourceEntry;
      try (AutoRelease lock = sourceFile.readLock()) {
        EntryCreationContext context = this.newEntryCreationContext(absoluteTargetPath, NO_FILE_ATTRIBUTES);
        return new MemoryFile(targetElementName, context, sourceFile);
      }
    } else {
      if (sourceEntry instanceof MemoryDirectory) {
        MemoryDirectory sourceDirectory = (MemoryDirectory) sourceEntry;
        try (AutoRelease lock = sourceDirectory.readLock()) {
          EntryCreationContext context = MemoryFileSystem.this.newEntryCreationContext(absoluteTargetPath, NO_FILE_ATTRIBUTES);
          return new MemoryDirectory(targetElementName, context);
        }
      } else if (sourceEntry instanceof MemorySymbolicLink) {
        MemorySymbolicLink sourceLink = (MemorySymbolicLink) sourceEntry;
        try (AutoRelease lock = sourceLink.readLock()) {
          EntryCreationContext context = MemoryFileSystem.this.newEntryCreationContext(absoluteTargetPath, NO_FILE_ATTRIBUTES);
          return new MemorySymbolicLink(targetElementName, sourceLink.getTarget(), context);
        }
      } else {
        throw new AssertionError("unknown entry type:" + sourceEntry);
      }
    }
  }

  Path readSymbolicLink(AbstractPath path) throws IOException {
    // look up the parent following symlinks
    // then look up the child not following symlinks
    AbstractPath parent = (AbstractPath) path.toAbsolutePath().getParent();
    return this.accessFileReading(parent, true, parentEntry -> {
      if (!(parentEntry instanceof MemoryDirectory)) {
        throw new FileSystemException(path.toString(), null, "parent is not a directory");
      }
      MemoryDirectory directory = (MemoryDirectory) parentEntry;
      return MemoryFileSystem.this.withReadLockDo(directory, (AbstractPath) path.getFileName(), false, entry -> {
        if (!(entry instanceof MemorySymbolicLink)) {
          throw new NotLinkException("file is not a symbolic link");
        }
        return ((MemorySymbolicLink) entry).getTarget();
      });

    });
  }

  static void handleTwoPathOperation(CopyContext copyContext, MemoryDirectory firstDirectory, MemoryDirectory secondDirectory) throws IOException {

    EndPointCopyContext sourceContext = copyContext.source;
    EndPointCopyContext targetContext = copyContext.target;
    MemoryDirectory sourceParent = copyContext.getSourceParent(firstDirectory, secondDirectory);
    MemoryDirectory targetParent = copyContext.getTargetParent(firstDirectory, secondDirectory);

    StringTransformer sourceTransformer = sourceContext.path.getMemoryFileSystem().lookUpTransformer;
    String sourceElementName = sourceTransformer.transform(sourceContext.elementName);
    MemoryEntry sourceEntry = sourceParent.getEntryOrException(sourceElementName, sourceContext.path);

    StringTransformer targetTransformer = targetContext.path.getMemoryFileSystem().lookUpTransformer;
    String targetElementName = targetTransformer.transform(targetContext.elementName);
    MemoryEntry targetEntry = targetParent.getEntry(targetElementName);

    if (sourceEntry == targetEntry) {
      // source and target are the same, do nothing
      // the way I read Files#copy this is the intention of the spec
      return;
    }

    if (sourceEntry == targetParent) {
      // copy or move "folder" -> "folder/sub"
      throw new FileSystemException(sourceContext.path.toString(), targetContext.path.toString(), "invalid argument");
    }

    // have to check permission first
    targetParent.checkAccess(WRITE);
    if (copyContext.operation.isMove()) {
      sourceParent.checkAccess(WRITE);
    }


    if (targetEntry != null) {
      if (!copyContext.replaceExisting) {
        throw new FileAlreadyExistsException(targetContext.path.toString());
      }

      if (targetEntry instanceof MemoryDirectory) {
        MemoryDirectory targetDirectory = (MemoryDirectory) targetEntry;
        try (AutoRelease lock = targetDirectory.readLock()) {
          targetDirectory.checkEmpty(targetContext.path);
        }
      }

      // TODO target should become symlink
      targetParent.removeEntry(targetElementName);
    }

    String newOriginalName = targetContext.path.getMemoryFileSystem().storeTransformer.transform(targetContext.elementName);
    if (copyContext.operation.isMove()) {
      sourceParent.removeEntry(sourceElementName);
      targetParent.addEntry(targetElementName, sourceEntry, copyContext.target.path);
      sourceEntry.setOriginalName(newOriginalName);
    } else {
      MemoryEntry toCopy = getCopySource(copyContext, sourceEntry);
      MemoryEntry copy = targetContext.path.getMemoryFileSystem().copyEntry(targetContext.path, toCopy, newOriginalName);
      if (copyContext.copyAttributes) {
        copy.initializeAttributes(toCopy);
      }
      targetParent.addEntry(targetElementName, copy, copyContext.target.path);
    }
  }

  private static MemoryEntry getCopySource(CopyContext copyContext, MemoryEntry sourceEntry) throws IOException {
    MemoryEntry toCopy;
    if (sourceEntry instanceof MemorySymbolicLink && copyContext.isSourceFollowSymLinks()) {
      AbstractPath linkTarget = ((MemorySymbolicLink) sourceEntry).getTarget();
      // TODO requires reentrant lock, should build return value object
      MemoryFileSystem sourceFileSystem = copyContext.source.path.getFileSystem();
      AbstractPath lookupPath;
      if (linkTarget.isAbsolute()) {
        lookupPath = linkTarget;
      } else {
        lookupPath = (AbstractPath) copyContext.source.parent.resolve(linkTarget);
      }
      toCopy = sourceFileSystem.getFile(lookupPath, NO_OPEN_OPTIONS, NO_FILE_ATTRIBUTES);
    } else {
      toCopy = sourceEntry;
    }
    return toCopy;
  }

  @Override
  public String toString() {
    return MemoryFileSystem.class.getSimpleName() + '[' + this.key + ']';
  }

  static class LazyFileAttributeView implements InvocationHandler {

    static final AtomicReferenceFieldUpdater<LazyFileAttributeView, FileAttributeView> ATTRIBUTE_VIEW_UPDATER =
            AtomicReferenceFieldUpdater.newUpdater(LazyFileAttributeView.class, FileAttributeView.class, "attributeView");

    private final AbstractPath path;
    private final LinkOption[] options;
    private final Class<? extends FileAttributeView> type;

    @SuppressWarnings("unused") // ATTRIBUTE_VIEW_UPDATER
    private volatile FileAttributeView attributeView;

    LazyFileAttributeView(AbstractPath path, Class<? extends FileAttributeView> type, LinkOption... options) {
      this.path = path;
      this.options = options;
      this.type = type;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      String methodName = method.getName();
      switch (methodName) {
        case "name":
          if (args != null && args.length > 0) {
            throw new AssertionError("#name() not expected to have any arguments");
          }
          return FileAttributeViews.mapAttributeView(this.type);
        case "toString":
          if (args != null && args.length > 0) {
            throw new AssertionError("#toString() not expected to have any arguments");
          }
          return this.type.toString();
        case "equals":
          if (args == null || args.length != 1) {
            throw new AssertionError("#equals() expected to exactly one argument");
          }
          return proxy == args[0];
        case "hashCode":
          if (args != null && args.length > 0) {
            throw new AssertionError("#hashCode() not expected to have any arguments");
          }
          return System.identityHashCode(proxy);
        default:
          try {
            return method.invoke(this.getView(), args);
          } catch (InvocationTargetException e) {
            throw e.getCause();
          }
      }
    }

    private FileAttributeView getView() throws IOException {
      FileAttributeView view = ATTRIBUTE_VIEW_UPDATER.get(this);
      if (view != null) {
        return view;
      } else {
        FileAttributeView newValue = this.path.getMemoryFileSystem().getFileAttributeView(this.path, this.type, this.options);
        boolean successful = ATTRIBUTE_VIEW_UPDATER.compareAndSet(this, null, newValue);
        if (successful) {
          return newValue;
        } else {
          return ATTRIBUTE_VIEW_UPDATER.get(this);
        }
      }
    }

  }

  @FunctionalInterface
  interface MemoryEntryBlock<R> {

    R value(MemoryEntry entry) throws IOException;

  }

  @FunctionalInterface
  interface MemoryDirectoryBlock<R> {

    R value(MemoryDirectory entry) throws IOException;

  }

  @FunctionalInterface
  interface MemoryEntryCreator {

    MemoryEntry create(String name) throws IOException;

  }

}
