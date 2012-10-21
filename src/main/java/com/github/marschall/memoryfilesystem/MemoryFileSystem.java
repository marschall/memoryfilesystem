package com.github.marschall.memoryfilesystem;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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
          MemoryUserPrincipalLookupService userPrincipalLookupService, ClosedFileSystemChecker checker, StringTransformer pathTransformer,
          StringTransformer lookUpTransformer, Collator collator, Set<Class<? extends FileAttributeView>> additionalViews) {
    this.key = key;
    this.separator = separator;
    this.pathParser = pathParser;
    this.provider = provider;
    this.store = store;
    this.userPrincipalLookupService = userPrincipalLookupService;
    this.checker = checker;
    this.storeTransformer = pathTransformer;
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


  SeekableByteChannel newByteChannel(AbstractPath path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
    // TODO check options
    // TODO check attributes
    this.checker.check();
    options.contains(StandardOpenOption.APPEND);
    this.getRootDirectory(path);

    // TODO locks
    MemoryFile file = this.getFile(path, options, attrs);
    return file.newChannel(options);
  }

  private MemoryFile getFile(AbstractPath path,
          Set<? extends OpenOption> options, FileAttribute<?>[] attrs) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException();
  }

  DirectoryStream<Path> newDirectoryStream(AbstractPath abstractPath, Filter<? super Path> filter) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException();
  }


  void createDirectory(AbstractPath path, FileAttribute<?>... attrs) throws IOException {
    //TODO don't ignore attrs
    this.createFile(path, new MemoryEntryCreator() {

      @Override
      public MemoryEntry create(String name) {
        return new MemoryDirectory(name, MemoryFileSystem.this.additionalViews);
      }

    });
  }

  void createSymbolicLink(AbstractPath link, final AbstractPath target, FileAttribute<?>... attrs) throws IOException {
    //TODO don't ignore attrs
    this.createFile(link, new MemoryEntryCreator() {

      @Override
      public MemoryEntry create(String name) {
        return new MemorySymbolicLink(name, target, MemoryFileSystem.this.additionalViews);
      }

    });
  }

  private void createFile(AbstractPath path, final MemoryEntryCreator creator) throws IOException {
    this.checker.check();
    MemoryDirectory rootDirectory = this.getRootDirectory(path);

    final ElementPath absolutePath = (ElementPath) path.toAbsolutePath();
    final Path parent = absolutePath.getParent();

    this.withWriteLockOnLastDo(rootDirectory, (AbstractPath) parent, new MemoryEntryBlock<Void>() {

      @Override
      public Void value(MemoryEntry entry) throws IOException {
        if (!(entry instanceof MemoryDirectory)) {
          // TODO use FileSystemException?
          throw new IOException(parent + " is not a directory");
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
    abstractPath.normalize().toAbsolutePath();
    this.isFollowSymLinks(options);
    // TODO implement
    throw new UnsupportedOperationException();
  }

  private boolean isFollowSymLinks(LinkOption... options) {
    if (options == null) {
      return false;
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
    this.accessFile(path, new MemoryEntryBlock<Void>() {

      @Override
      public Void value(MemoryEntry entry) throws IOException {
        entry.checkAccess(modes);
        return null;
      }
    });
  }

  <A extends BasicFileAttributes> A readAttributes(AbstractPath path, final Class<A> type, LinkOption... options) throws IOException {
    this.checker.check();
    return this.accessFile(path, new MemoryEntryBlock<A>() {

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
    return this.accessFile(path, new MemoryEntryBlock<V>() {

      @Override
      public V value(MemoryEntry entry) throws IOException {
        return entry.getFileAttributeView(type);
      }
    });
  }

  Map<String, Object> readAttributes(AbstractPath path, final String attributes, LinkOption... options) throws IOException {
    this.checker.check();
    return this.accessFile(path, new MemoryEntryBlock<Map<String, Object>>() {

      @Override
      public Map<String, Object> value(MemoryEntry entry) throws IOException {
        return AttributeAccessors.readAttributes(entry, attributes);
      }
    });
  }

  void setAttribute(AbstractPath path, final String attribute, final Object value, LinkOption... options) throws IOException {
    this.checker.check();
    this.accessFile(path, new MemoryEntryBlock<Void>() {

      @Override
      public Void value(MemoryEntry entry) throws IOException {
        // TODO write lock?
        AttributeAccessors.setAttribute(entry, attribute, value);
        return null;
      }
    });
  }

  private <R> R accessFile(AbstractPath path, MemoryEntryBlock<? extends R> callback) throws IOException {
    this.checker.check();
    MemoryDirectory directory = this.getRootDirectory(path);
    Path absolutePath = path.toAbsolutePath();
    return this.withReadLockDo(directory, (AbstractPath) absolutePath, callback);
  }


  interface MemoryEntryBlock<R> {

    R value(MemoryEntry entry) throws IOException;

  }

  interface MemoryEntryCreator {

    MemoryEntry create(String name);

  }

  private <R> R withWriteLockOnLastDo(MemoryDirectory root, AbstractPath path, MemoryEntryBlock<R> callback) throws IOException {
    if (path instanceof Root) {
      try (AutoRelease lock = root.writeLock()) {
        return callback.value(root);
      }
    } else if (path instanceof ElementPath) {
      ElementPath elementPath = (ElementPath) path;
      try (AutoRelease lock = root.readLock()) {
        return this.withWriteLockOnLastDo(root, elementPath, 0, path.getNameCount(), callback);
      }
    } else {
      throw new IllegalArgumentException("unknown path type" + path);
    }

  }

  private <R> R withWriteLockOnLastDo(MemoryEntry parent, ElementPath path, int i, int length, MemoryEntryBlock<R> callback) throws IOException {
    if (!(parent instanceof MemoryDirectory)) {
      //TODO construct better error message
      // TODO use FileSystemException?
      throw new IOException("not a directory");
    }

    MemoryEntry entry = ((MemoryDirectory) parent).getEntry(path.getNameElement(i));
    if (entry == null) {
      //TODO construct better error message
      throw new NoSuchFileException("directory does not exist");
    }

    if (i == length - 1) {
      try (AutoRelease lock = entry.writeLock()) {
        return callback.value(entry);
      }
    } else {
      try (AutoRelease lock = entry.readLock()) {
        return this.withWriteLockOnLastDo(entry, path, i + 1, length, callback);
      }
    }
  }


  private <R> R withReadLockDo(MemoryDirectory root, AbstractPath path, MemoryEntryBlock<? extends R> callback) throws IOException {
    if (path instanceof Root) {
      try (AutoRelease lock = root.readLock()) {
        return callback.value(root);
      }
    } else if (path instanceof ElementPath) {
      ElementPath elementPath = (ElementPath) path;
      try (AutoRelease lock = root.readLock()) {
        return this.withReadLockDo(root, elementPath, 0, path.getNameCount(), callback);
      }
    } else {
      throw new IllegalArgumentException("unknown path type" + path);
    }
  }

  private <R> R withReadLockDo(MemoryEntry parent, ElementPath path, int i, int length, MemoryEntryBlock<? extends R> callback) throws IOException {
    if (!(parent instanceof MemoryDirectory)) {
      //TODO construct better error message
      // TODO use FileSystemException?
      throw new IOException("not a directory");
    }

    MemoryEntry entry = ((MemoryDirectory) parent).getEntry(path.getNameElement(i));
    if (entry == null) {
      //TODO construct better error message
      throw new NoSuchFileException("directory does not exist");
    }

    if (i == length - 1) {
      try (AutoRelease lock = entry.readLock()) {
        return callback.value(entry);
      }
    } else {
      try (AutoRelease lock = entry.readLock()) {
        return this.withReadLockDo(entry, path, i + 1, length, callback);
      }
    }
  }

  private MemoryDirectory getRootDirectory(AbstractPath path) throws IOException {
    MemoryDirectory directory = this.roots.get(path.getRoot());
    if (directory == null) {
      // TODO use FileSystemException?
      throw new IOException("the root of " + path + " does not exist");
    }
    return directory;
  }



  void delete(AbstractPath abstractPath) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException();
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
    return this.accessFile(abstractPath, new MemoryEntryBlock<Boolean>(){

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
          return method.invoke(this.getView(), args);
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

}
