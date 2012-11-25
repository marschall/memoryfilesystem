package com.github.marschall.memoryfilesystem;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.AccessMode;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemException;
import java.nio.file.FileSystemLoopException;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.StandardOpenOption;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.DosFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileOwnerAttributeView;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import javax.annotation.PreDestroy;

class MemoryFileSystem extends FileSystem {

  private final String key;

  private final String separator;

  private final MemoryFileSystemProvider provider;

  private final MemoryFileStore store;

  private final Iterable<FileStore> stores;

  private final ClosedFileSystemChecker checker;

  private volatile Map<Root, MemoryDirectory> roots;

  private volatile Map<String, Root> rootByKey;

  private volatile Path defaultPath;

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

  MemoryFileSystem(String key, String separator, PathParser pathParser, MemoryFileSystemProvider provider, MemoryFileStore store,
          MemoryUserPrincipalLookupService userPrincipalLookupService, ClosedFileSystemChecker checker, StringTransformer storeTransformer,
          StringTransformer lookUpTransformer, Collator collator, Set<Class<? extends FileAttributeView>> additionalViews) {
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
    this.stores = Collections.<FileStore>singletonList(store);
    this.emptyPath = new EmptyPath(this);
    this.supportedFileAttributeViews = this.buildSupportedFileAttributeViews(additionalViews);
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
        views.add(FileAttributeViews.mapAttributeView(viewClass));
      }
      return Collections.unmodifiableSet(views);
    }
  }

  String getKey() {
    return this.key;
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

  Path getDefaultPath() {
    return this.defaultPath;
  }


  BlockChannel newFileChannel(AbstractPath path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
    this.checker.check();
    MemoryFile file = this.getFile(path, options, attrs);
    return file.newChannel(options);
  }


  InputStream newInputStream(AbstractPath path, OpenOption... options) throws IOException {
    this.checker.check();
    Set<OpenOption> optionsSet;
    if (options == null || options.length == 0) {
      optionsSet = Collections.emptySet();
    } else {
      optionsSet = new HashSet<>(options.length);
      for (OpenOption option : options) {
        optionsSet.add(option);
      }
    }
    MemoryFile file = this.getFile(path, optionsSet);
    return file.newInputStream(optionsSet);
  }


  OutputStream newOutputStream(AbstractPath path, OpenOption... options) throws IOException {
    this.checker.check();
    Set<OpenOption> optionsSet;
    if (options == null || options.length == 0) {
      optionsSet = Collections.emptySet();
    } else {
      optionsSet = new HashSet<>(options.length);
      for (OpenOption option : options) {
        optionsSet.add(option);
      }
    }
    MemoryFile file = this.getFile(path, optionsSet);
    return file.newOutputStream(optionsSet);
  }

  private MemoryFile getFile(final AbstractPath path, final Set<? extends OpenOption> options, final FileAttribute<?>... attrs) throws IOException {
    final ElementPath absolutePath = (ElementPath) path.toAbsolutePath();
    MemoryDirectory rootDirectory = this.getRootDirectory(absolutePath);

    final Path parent = absolutePath.getParent();

    return this.withWriteLockOnLastDo(rootDirectory, (AbstractPath) parent, this.isFollowSymLinks(options), new MemoryEntryBlock<MemoryFile>() {

      @Override
      public MemoryFile value(MemoryEntry entry) throws IOException {
        if (!(entry instanceof MemoryDirectory)) {
          throw new NotDirectoryException(parent.toString());
        }
        boolean isCreateNew = options.contains(StandardOpenOption.CREATE_NEW);
        MemoryDirectory directory = (MemoryDirectory) entry;
        String fileName = absolutePath.getLastNameElement();
        String key = MemoryFileSystem.this.lookUpTransformer.transform(fileName);
        if (isCreateNew) {
          String name = MemoryFileSystem.this.storeTransformer.transform(fileName);
          MemoryFile file = new MemoryFile(name, MemoryFileSystem.this.additionalViews);
          AttributeAccessors.setAttributes(file, attrs);
          // will throw an exception if already present
          directory.addEntry(key, file);
          return file;
        } else {
          MemoryEntry storedEntry = directory.getEntry(key);
          if (storedEntry == null) {
            boolean isCreate = options.contains(StandardOpenOption.CREATE);
            if (isCreate) {
              String name = MemoryFileSystem.this.storeTransformer.transform(fileName);
              MemoryFile file = new MemoryFile(name, MemoryFileSystem.this.additionalViews);
              AttributeAccessors.setAttributes(file, attrs);
              directory.addEntry(key, file);
              return file;
            } else {
              throw new NoSuchFileException(path.toString());
            }
          }
          if (storedEntry instanceof MemoryFile) {
            return (MemoryFile) storedEntry;
          } else {
            throw new IOException("file is a directory");
          }

        }
      }
    });

  }

  DirectoryStream<Path> newDirectoryStream(final AbstractPath abstractPath, final Filter<? super Path> filter) throws IOException {
    final AbstractPath absolutePath = (AbstractPath) abstractPath.toAbsolutePath();
    MemoryDirectory root = this.getRootDirectory(absolutePath);
    return this.withReadLockDo(root, abstractPath, false, new MemoryEntryBlock<DirectoryStream<Path>>() {

      @Override
      public DirectoryStream<Path> value(MemoryEntry entry) throws IOException {
        if (!(entry instanceof MemoryDirectory)) {
          throw new NotDirectoryException(abstractPath.toString());
        }
        MemoryDirectory directory = (MemoryDirectory) entry;
        return directory.newDirectoryStream(absolutePath, filter);
      }
    });
  }


  void createDirectory(AbstractPath path, final FileAttribute<?>... attrs) throws IOException {
    this.createFile(path, new MemoryEntryCreator() {

      @Override
      public MemoryEntry create(String name) throws IOException {
        MemoryDirectory directory = new MemoryDirectory(name, MemoryFileSystem.this.additionalViews);
        AttributeAccessors.setAttributes(directory, attrs);
        return directory;
      }

    });
  }

  void createSymbolicLink(AbstractPath link, final AbstractPath target, final FileAttribute<?>... attrs) throws IOException {
    this.createFile(link, new MemoryEntryCreator() {

      @Override
      public MemoryEntry create(String name) throws IOException {
        MemorySymbolicLink symbolicLink = new MemorySymbolicLink(name, target, MemoryFileSystem.this.additionalViews);
        AttributeAccessors.setAttributes(symbolicLink, attrs);
        return symbolicLink;
      }

    });
  }

  private void createFile(final AbstractPath path, final MemoryEntryCreator creator) throws IOException {
    this.checker.check();
    final ElementPath absolutePath = (ElementPath) path.toAbsolutePath();
    MemoryDirectory rootDirectory = this.getRootDirectory(absolutePath);

    final Path parent = absolutePath.getParent();

    this.withWriteLockOnLastDo(rootDirectory, (AbstractPath) parent, true, new MemoryEntryBlock<Void>() {

      @Override
      public Void value(MemoryEntry entry) throws IOException {
        if (!(entry instanceof MemoryDirectory)) {
          throw new NotDirectoryException(parent.toString());
        }
        String name = MemoryFileSystem.this.storeTransformer.transform(absolutePath.getLastNameElement());
        MemoryEntry newEntry = creator.create(name);
        String key = MemoryFileSystem.this.lookUpTransformer.transform(newEntry.getOriginalName());
        ((MemoryDirectory) entry).addEntry(key, newEntry);
        return null;
      }
    });

  }

  Path toRealPath(AbstractPath abstractPath, LinkOption... options)throws IOException  {
    this.checker.check();
    AbstractPath absolutePath = (AbstractPath) abstractPath.normalize().toAbsolutePath();
    boolean followSymLinks = this.isFollowSymLinks(options);
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


  private Path toRealPath(MemoryDirectory root, AbstractPath path, Set<MemorySymbolicLink> encounteredLinks, boolean followSymLinks) throws IOException {
    if (path.isRoot()) {
      return path.getRoot();
    } else if (path instanceof ElementPath) {
      Path symLinkTarget = null;

      ElementPath elementPath = (ElementPath) path;
      List<String> nameElements = elementPath.getNameElements();
      List<String> realPath = new ArrayList<>(nameElements.size());
      List<AutoRelease> locks = new ArrayList<>(nameElements.size() + 1);
      try {
        locks.add(root.readLock());
        MemoryDirectory parent = root;
        for (int i = 0; i < nameElements.size(); ++i) {
          String fileName = nameElements.get(i);
          String key = this.lookUpTransformer.transform(fileName);
          MemoryEntry current = parent.getEntry(key);
          if (current == null) {
            throw new NoSuchFileException(path.toString());
          }
          locks.add(current.readLock());
          realPath.add(current.getOriginalName());

          if (followSymLinks && current instanceof MemorySymbolicLink) {
            MemorySymbolicLink link = (MemorySymbolicLink) current;
            if (!encounteredLinks.add(link)) {
              // TODO better error message
              throw new FileSystemLoopException(path.toString());
            }
            symLinkTarget = link.getTarget();
          }

          if (i == nameElements.size() - 1) {
            continue;
          } else if (current instanceof MemoryDirectory) {
            parent = (MemoryDirectory) current;
          } else {
            //TODO construct better error message
            throw new NotDirectoryException(path.toString());
          }

        }
      } finally {
        for (int i = locks.size() - 1; i >= 0; --i) {
          AutoRelease lock = locks.get(i);
          lock.close();
        }
      }
      if (symLinkTarget == null) {
        return AbsolutePath.createAboslute(this, (Root) path.getRoot(), realPath);
      } else {
        return this.toRealPath(root, (AbstractPath) symLinkTarget, encounteredLinks, followSymLinks);
      }

    } else {
      throw new IllegalArgumentException("unknown path type" + path);
    }
  }

  private boolean isFollowSymLinks(Set<? extends OpenOption> options) {
    if (options == null || options.isEmpty()) {
      return true;
    }

    for (OpenOption option : options) {
      if (option == LinkOption.NOFOLLOW_LINKS) {
        return false;
      }
    }
    return true;
  }

  private boolean isFollowSymLinks(LinkOption... options) {
    if (options == null) {
      return true;
    }

    for (LinkOption option : options) {
      if (option == LinkOption.NOFOLLOW_LINKS) {
        return false;
      }
    }
    return true;
  }


  void checkAccess(AbstractPath path, final AccessMode... modes) throws IOException {
    this.checker.check();
    // java.nio.file.spi.FileSystemProvider#checkAccess(Path, AccessMode...)
    // says we should follow symbolic links
    this.accessFile(path, true, new MemoryEntryBlock<Void>() {

      @Override
      public Void value(MemoryEntry entry) throws IOException {
        entry.checkAccess(modes);
        return null;
      }
    });
  }

  <A extends BasicFileAttributes> A readAttributes(AbstractPath path, final Class<A> type, LinkOption... options) throws IOException {
    this.checker.check();
    return this.accessFile(path, this.isFollowSymLinks(options), new MemoryEntryBlock<A>() {

      @Override
      public A value(MemoryEntry entry) throws IOException {
        return entry.readAttributes(type);
      }
    });
  }

  <V extends FileAttributeView> V getLazyFileAttributeView(AbstractPath path, final Class<V> type, LinkOption... options) {
    InvocationHandler handler = new LazyFileAttributeView<>(path, type, options);
    Object proxy = Proxy.newProxyInstance(MemoryFileSystem.class.getClassLoader(), new Class<?>[]{type}, handler);
    return type.cast(proxy);
  }

  <V extends FileAttributeView> V getFileAttributeView(AbstractPath path, final Class<V> type, LinkOption... options) throws IOException {
    return this.accessFile(path, this.isFollowSymLinks(options), new MemoryEntryBlock<V>() {

      @Override
      public V value(MemoryEntry entry) throws IOException {
        return entry.getFileAttributeView(type);
      }
    });
  }

  Map<String, Object> readAttributes(AbstractPath path, final String attributes, LinkOption... options) throws IOException {
    this.checker.check();
    return this.accessFile(path, this.isFollowSymLinks(options), new MemoryEntryBlock<Map<String, Object>>() {

      @Override
      public Map<String, Object> value(MemoryEntry entry) throws IOException {
        return AttributeAccessors.readAttributes(entry, attributes);
      }
    });
  }

  void setAttribute(AbstractPath path, final String attribute, final Object value, LinkOption... options) throws IOException {
    this.checker.check();
    this.accessFile(path, this.isFollowSymLinks(options), new MemoryEntryBlock<Void>() {

      @Override
      public Void value(MemoryEntry entry) throws IOException {
        // TODO write lock?
        AttributeAccessors.setAttribute(entry, attribute, value);
        return null;
      }
    });
  }

  private <R> R accessFile(AbstractPath path, boolean followSymLinks, MemoryEntryBlock<? extends R> callback) throws IOException {
    this.checker.check();
    AbstractPath absolutePath = (AbstractPath) path.toAbsolutePath();
    MemoryDirectory directory = this.getRootDirectory(absolutePath);
    return this.withReadLockDo(directory, absolutePath, followSymLinks, callback);
  }


  private <R> R withWriteLockOnLastDo(MemoryDirectory root, AbstractPath path,  boolean followSymLinks, MemoryEntryBlock<R> callback) throws IOException {
    Set<MemorySymbolicLink> encounteredSymlinks;
    if (followSymLinks) {
      encounteredSymlinks = new HashSet<>(4);
    } else {
      encounteredSymlinks = Collections.emptySet();
    }
    return this.withLockDo(root, path, encounteredSymlinks, followSymLinks, LockType.WRITE, callback);

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
    if (path instanceof Root) {
      try (AutoRelease lock = root.readLock()) {
        return callback.value(root);
      }
    } else if (path instanceof ElementPath) {
      R result = null;
      Path symLinkTarget = null;

      ElementPath elementPath = (ElementPath) path;
      List<String> nameElements = elementPath.getNameElements();
      List<AutoRelease> locks = new ArrayList<>(nameElements.size() + 1);
      try {
        locks.add(root.readLock());
        MemoryDirectory parent = root;
        for (int i = 0; i < nameElements.size(); ++i) {
          String fileName = nameElements.get(i);
          String key = this.lookUpTransformer.transform(fileName);
          MemoryEntry current = parent.getEntry(key);
          if (current == null) {
            throw new NoSuchFileException(path.toString());
          }
          boolean isLast = i == nameElements.size() - 1;
          if (isLast && lockType == LockType.WRITE) {
            locks.add(current.writeLock());
          } else {
            locks.add(current.readLock());
          }

          if (followSymLinks && current instanceof MemorySymbolicLink) {
            MemorySymbolicLink link = (MemorySymbolicLink) current;
            if (!encounteredLinks.add(link)) {
              // TODO better error message
              throw new FileSystemLoopException(path.toString());
            }
            symLinkTarget = link.getTarget();
          }

          if (isLast) {
            result = callback.value(current);
          } else if (current instanceof MemoryDirectory) {
            parent = (MemoryDirectory) current;
          } else {
            //TODO construct better error message
            throw new NotDirectoryException(path.toString());
          }

        }
      } finally {
        for (int i = locks.size() - 1; i >= 0; --i) {
          AutoRelease lock = locks.get(i);
          lock.close();
        }
      }
      if (symLinkTarget == null) {
        return result;
      } else {
        return this.withLockDo(root, (AbstractPath) symLinkTarget, encounteredLinks, followSymLinks, lockType, callback);
      }

    } else {
      throw new IllegalArgumentException("unknown path type" + path);
    }
  }

  private MemoryDirectory getRootDirectory(AbstractPath path) throws IOException {
    Path root = path.getRoot();
    MemoryDirectory directory = this.roots.get(root);
    if (directory == null) {
      throw new NoSuchFileException(path.toString(), null, "the root doesn't exist");
    }
    return directory;
  }



  void delete(final AbstractPath abstractPath) throws IOException {
    final ElementPath absolutePath = (ElementPath) abstractPath.toAbsolutePath();
    MemoryDirectory rootDirectory = this.getRootDirectory(absolutePath);

    final Path parent = absolutePath.getParent();

    this.withWriteLockOnLastDo(rootDirectory, (AbstractPath) parent, true, new MemoryEntryBlock<Void>() {

      @Override
      public Void value(MemoryEntry entry) throws IOException {
        if (!(entry instanceof MemoryDirectory)) {
          throw new NotDirectoryException(parent.toString());
        }
        MemoryDirectory directory = (MemoryDirectory) entry;
        String fileName = absolutePath.getLastNameElement();
        String key = MemoryFileSystem.this.lookUpTransformer.transform(fileName);
        MemoryEntry child = directory.getEntry(key);
        if (child == null) {
          throw new NoSuchFileException(abstractPath.toString());
        }
        try (AutoRelease lock = child.writeLock()) {
          if (child instanceof MemoryDirectory) {
            MemoryDirectory childDirectory = (MemoryDirectory) child;
            if (!childDirectory.isEmpty()) {
              throw new DirectoryNotEmptyException(abstractPath.toString());
            }
          }
          if (child instanceof MemoryFile) {
            MemoryFile file = (MemoryFile) child;
            if (file.openCount() > 0) {
              throw new FileSystemException(abstractPath.toString(), null, "file still open");
            }
          }
          directory.removeEntry(key);
        }
        return null;
      }
    });
  }


  @Override
  public FileSystemProvider provider() {
    this.checker.check();
    return this.provider;
  }


  @Override
  @PreDestroy // closing twice is explicitly allowed by the contract
  public void close() throws IOException {
    this.checker.close();
    this.provider.close(this);
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
  public Path getPath(String first, String... more) {
    this.checker.check();
    // TODO check for maximum length
    // TODO check for valid characters
    return this.pathParser.parse(this.rootByKey, first, more);
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
      Path patternPath = this.getPath(pattern);
      return new GlobPathMatcher(patternPath);
    }
    if (syntax.equalsIgnoreCase(RegexPathMatcher.name())) {
      Pattern regex = Pattern.compile(pattern);
      return new RegexPathMatcher(regex);
    }

    throw new UnsupportedOperationException("unsupported syntax \"" + syntax + "\"");
  }


  @Override
  public UserPrincipalLookupService getUserPrincipalLookupService() {
    this.checker.check();
    return this.userPrincipalLookupService;
  }


  @Override
  public WatchService newWatchService() throws IOException {
    this.checker.check();
    // TODO Auto-generated method stub
    // TODO make configurable
    throw new UnsupportedOperationException();
  }


  FileStore getFileStore() {
    return this.store;
  }

  Collator getCollator() {
    return this.collator;
  }

  boolean isHidden(AbstractPath abstractPath) throws IOException {
    // Posix seems to check only the file name
    // TODO write test
    return this.accessFile(abstractPath, false, new MemoryEntryBlock<Boolean>(){

      @Override
      public Boolean value(MemoryEntry entry) throws IOException {
        Set<String> supportedFileAttributeViews = MemoryFileSystem.this.supportedFileAttributeViews();
        if (supportedFileAttributeViews.contains(FileAttributeViews.POSIX)) {
          String originalName = entry.getOriginalName();
          return !originalName.isEmpty() && originalName.charAt(0) == '.';
        } else if (supportedFileAttributeViews.contains(FileAttributeViews.DOS)) {
          return entry.readAttributes(DosFileAttributes.class).isHidden();
        } else {
          return false;
        }
      }

    });
  }

  class LazyFileAttributeView<V extends FileAttributeView> implements InvocationHandler {

    private final AbstractPath path;
    private final LinkOption[] options;
    private final Class<V> type;
    private final AtomicReference<V> attributeView;

    LazyFileAttributeView(AbstractPath path, Class<V> type, LinkOption... options) {
      this.path = path;
      this.options = options;
      this.type = type;
      this.attributeView = new AtomicReference<>();
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

    private V getView() throws IOException {
      V v = this.attributeView.get();
      if (v != null) {
        return v;
      } else {
        V newValue = MemoryFileSystem.this.getFileAttributeView(this.path, this.type, this.options);
        boolean successful = this.attributeView.compareAndSet(null, newValue);
        if (successful) {
          return newValue;
        } else {
          return this.attributeView.get();
        }
      }
    }

  }



  interface MemoryEntryBlock<R> {

    R value(MemoryEntry entry) throws IOException;

  }

  interface MemoryEntryCreator {

    MemoryEntry create(String name) throws IOException;

  }

  enum LockType {

    READ,
    WRITE;

  }


}
