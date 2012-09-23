package com.google.code.memoryfilesystem;

import static com.google.code.memoryfilesystem.MemoryFileSystemProperties.BASIC_FILE_ATTRIBUTE_VIEW_NAME;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.StandardOpenOption;
import java.nio.file.WatchService;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

class MemoryFileSystem extends FileSystem {

  private final String separator;

  private final MemoryFileSystemProvider provider;

  private final MemoryFileStore store;

  private final Iterable<FileStore> stores;

  private final ClosedFileSystemChecker checker;

  private volatile Map<Root, MemoryDirectory> roots;

  private final MemoryUserPrincipalLookupService userPrincipalLookupService;

  private final PathParser pathParser;

  MemoryFileSystem(String separator, PathParser pathParser, MemoryFileSystemProvider provider, MemoryFileStore store,
      MemoryUserPrincipalLookupService userPrincipalLookupService, ClosedFileSystemChecker checker) {
    this.separator = separator;
    this.pathParser = pathParser;
    this.provider = provider;
    this.store = store;
    this.userPrincipalLookupService = userPrincipalLookupService;
    this.checker = checker;
    this.stores = Collections.<FileStore>singletonList(store);
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
  }


  SeekableByteChannel newByteChannel(AbstractPath path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
    // TODO check options
    // TODO check attributes
    this.checker.check();
    boolean isAppend = options.contains(StandardOpenOption.APPEND);
    MemoryDirectory directory = this.getRootDirectory(path);
    
    throw new UnsupportedOperationException();
  }
  
  DirectoryStream<Path> newDirectoryStream(AbstractPath abstractPath, Filter<? super Path> filter) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException();
  }



  void createDirectory(AbstractPath path, FileAttribute<?>[] attrs) throws IOException {
    this.checker.check();
    MemoryDirectory directory = this.getRootDirectory(path);
    
    
    Path parent = path.getParent();
    if (!(parent instanceof ElementPath)) {
      throw new IOException("operation not supported on roots");
    }
    
    final ElementPath elementParent = (ElementPath) parent;
    
    this.withWriteLockOnLastDo(directory, elementParent, new MemoryDirectoryBlock() {
      
      @Override
      public void value(MemoryDirectory directory) throws IOException {
        MemoryDirectory newDirectory = new MemoryDirectory();
        String name = elementParent.getNameElements().get(elementParent.getNameCount());
        directory.addEntry(name, newDirectory);
      }
    });
  }
  
  private void withWriteLockOnLastDo(MemoryDirectory root, ElementPath path, MemoryDirectoryBlock callback) throws IOException {
    ElementPath elementPath = (ElementPath) path;
    try (AutoRelease lock = root.writeLock()) {
      withWriteLockOnLastDo(root, elementPath, 1, path.getNameCount(), callback);
    }
  }
  
  interface MemoryDirectoryBlock {
    
    void value(MemoryDirectory directory) throws IOException;
    
  }
  

  private void withWriteLockOnLastDo(MemoryDirectory parent, ElementPath path, int i, int length, MemoryDirectoryBlock callback) throws IOException {
    MemoryEntry entry = parent.getEntry(path.getNameElements().get(i));
    if (entry == null) {
      //TODO construct better error message
      throw new IOException("directory does not exist");
    }
    
    if (!(entry instanceof MemoryDirectory)) {
      //TODO construct better error message
      throw new IOException("not a directory");
    }
    MemoryDirectory directory = (MemoryDirectory) entry;
    if (i == length - 1) {
      try (AutoRelease lock = directory.writeLock()) {
        callback.value(directory);
      }
    } else {
      try (AutoRelease lock = directory.readLock()) {
        this.withWriteLockOnLastDo(directory, path, i + 1, length, callback);
      }
    }
  }
  
  private MemoryDirectory getRootDirectory(AbstractPath path) throws IOException {
    AbstractPath absolutePath = (AbstractPath) path.toAbsolutePath();
    MemoryDirectory directory = this.roots.get(path.getRoot());
    if (directory == null) {
      throw new IOException("the root of " + path + " does not exist");
    }
    return directory;
  }



  void delete(AbstractPath abstractPath, Path path) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public FileSystemProvider provider() {
    this.checker.check();
    return this.provider;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void close() throws IOException {
    this.checker.close();
    this.provider.close(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isOpen() {
    return this.checker.isOpen();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isReadOnly() {
    this.checker.check();
    return this.store.isReadOnly();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getSeparator() {
    this.checker.check();
    return this.separator;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Iterable<Path> getRootDirectories() {
    this.checker.check();
    // this is fine because the iterator does not support modification
    return (Iterable<Path>) ((Object) this.roots.keySet());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Iterable<FileStore> getFileStores() {
    this.checker.check();
    return this.stores;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<String> supportedFileAttributeViews() {
    this.checker.check();
    return Collections.singleton(BASIC_FILE_ATTRIBUTE_VIEW_NAME);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Path getPath(String first, String... more) {
    this.checker.check();
    // TODO check for maximum length
    // TODO check for valid characters
    return this.pathParser.parse(this.roots.keySet(), first, more);
  }
  
  /**
   * {@inheritDoc}
   */
  @Override
  public PathMatcher getPathMatcher(String syntaxAndPattern) {
    this.checker.check();
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public UserPrincipalLookupService getUserPrincipalLookupService() {
    this.checker.check();
    return this.userPrincipalLookupService;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public WatchService newWatchService() throws IOException {
    this.checker.check();
    // TODO Auto-generated method stub
    // TODO make configurable
    throw new UnsupportedOperationException();
  }

  String getKey() {
    return this.store.getKey();
  }


  FileStore getFileStore() {
    return this.store;
  }

}
