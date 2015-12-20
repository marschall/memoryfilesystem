package com.github.marschall.memoryfilesystem;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.ProviderMismatchException;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.spi.FileSystemProvider;
import java.text.Collator;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Creates memory file systems instances.
 *
 * <p>This class should not be used directly. Instead
 * {@link java.nio.file.FileSystems#newFileSystem(URI, Map)}
 * should be used to create instances.</p>
 */
public final class MemoryFileSystemProvider extends FileSystemProvider {

  static final String SCHEME = "memory";

  private final ConcurrentMap<String, MemoryFileSystem> fileSystems;

  private final ExecutorService workExecutor;

  private final ExecutorService callbackExecutor;

  public MemoryFileSystemProvider() {
    this.fileSystems = new ConcurrentHashMap<>();
    this.workExecutor = Executors.newFixedThreadPool(1, new NamedDaemonThreadFactory("memory-file-system-worker"));
    this.callbackExecutor = Executors.newFixedThreadPool(1, new NamedDaemonThreadFactory("memory-file-system-callback"));
  }

  @Override
  public String getScheme() {
    return SCHEME;
  }

  @Override
  public FileSystem newFileSystem(URI uri, Map<String, ?> env) throws IOException {
    this.valideUri(uri);
    String key = this.getFileSystemKey(uri);
    EnvironmentParser parser = new EnvironmentParser(env);
    MemoryFileSystem fileSystem = this.createNewFileSystem(key, parser);
    MemoryFileSystem previous = this.fileSystems.putIfAbsent(key, fileSystem);
    if (previous != null) {
      String message = "File system " + uri.getScheme() + ':' + key + " already exists";
      throw new FileSystemAlreadyExistsException(message);
    } else {
      return fileSystem;
    }
  }

  private void valideUri(URI uri) {
    String schemeSpecificPart = uri.getSchemeSpecificPart();
    if (schemeSpecificPart.isEmpty()) {
      throw new IllegalArgumentException("scheme specific part must not be empty");
    }
    String host = uri.getHost();
    if (host != null) {
      throw new IllegalArgumentException("host must not be set");
    }
    String authority = uri.getAuthority();
    if (authority != null) {
      throw new IllegalArgumentException("authority must not be set");
    }
    String userInfo = uri.getUserInfo();
    if (userInfo != null) {
      throw new IllegalArgumentException("userInfo must not be set");
    }
    int port = uri.getPort();
    if (port != -1) {
      throw new IllegalArgumentException("port must not be set");
    }
    String path = uri.getPath();
    if (path != null) {
      throw new IllegalArgumentException("path must not be set");
    }
    String query = uri.getQuery();
    if (query != null) {
      throw new IllegalArgumentException("query must not be set");
    }
    String fragment = uri.getFragment();
    if (fragment != null) {
      throw new IllegalArgumentException("fragment must not be set");
    }
  }

  private MemoryFileSystem createNewFileSystem(String key, EnvironmentParser parser) throws IOException {
    ClosedFileSystemChecker checker = new ClosedFileSystemChecker();
    String separator = parser.getSeparator();
    StringTransformer storeTransformer = parser.getStoreTransformer();
    StringTransformer lookUpTransformer = parser.getLookUpTransformer();
    Collator collator = parser.getCollator();
    MemoryFileStore memoryStore = new MemoryFileStore(key, checker);
    Set<Class<? extends FileAttributeView>> additionalViews = parser.getAdditionalViews();
    MemoryUserPrincipalLookupService userPrincipalLookupService = this.createUserPrincipalLookupService(parser, checker);
    PathParser pathParser = this.buildPathParser(parser);
    Set<PosixFilePermission> umask = parser.getUmask();
    if (!additionalViews.contains(PosixFileAttributeView.class)) {
      umask = Collections.emptySet();
    }

    MemoryFileSystem fileSystem = new MemoryFileSystem(key, separator, pathParser, this, memoryStore,
            userPrincipalLookupService, checker, storeTransformer, lookUpTransformer, collator, additionalViews, umask);
    fileSystem.setRootDirectories(this.buildRootsDirectories(parser,  fileSystem, additionalViews, umask));
    String defaultDirectory = parser.getDefaultDirectory();
    fileSystem.setCurrentWorkingDirectory(defaultDirectory);
    AbstractPath defaultPath = fileSystem.getDefaultPath();
    if (!defaultPath.isRoot()) {
      // TODO configure owner and permissions
      Files.createDirectories(defaultPath);
    }
    return fileSystem;
  }

  private MemoryUserPrincipalLookupService createUserPrincipalLookupService(EnvironmentParser parser,
          ClosedFileSystemChecker checker) {
    List<String> userNames = parser.getUserNames();
    List<String> groupNames = parser.getGroupNames();
    StringTransformer nameTransfomer = parser.getPrincipalNameTransfomer();
    return new MemoryUserPrincipalLookupService(userNames, groupNames, nameTransfomer, checker);
  }


  @Override
  public FileSystem getFileSystem(URI uri) {
    String key = this.getFileSystemKey(uri);
    FileSystem fileSystem = this.fileSystems.get(key);
    if (fileSystem == null) {
      String message = "File system " + uri.getScheme() + ':' + key + " does not exist";
      throw new FileSystemNotFoundException(message);
    }
    return fileSystem;
  }

  private PathParser buildPathParser(EnvironmentParser parser) {
    String separator = parser.getSeparator();
    if (parser.isSingleEmptyRoot()) {
      return new SingleEmptyRootPathParser(separator, parser.getForbiddenCharacters());
    } else {
      return new MultipleNamedRootsPathParser(separator, parser.getStoreTransformer(), parser.getForbiddenCharacters());
    }
  }

  private Map<Root, MemoryDirectory> buildRootsDirectories(EnvironmentParser parser,
          MemoryFileSystem fileSystem, Set<Class<? extends FileAttributeView>> additionalViews,
          Set<PosixFilePermission> perms) throws IOException {
    final FileAttribute<?>[] attributes
    = new FileAttribute<?>[]{ PosixFilePermissions.asFileAttribute(perms) };
    if (parser.isSingleEmptyRoot()) {
      Root root = new EmptyRoot(fileSystem);
      MemoryDirectory directory = new MemoryDirectory("", fileSystem.newEntryCreationContext(root, attributes));
      directory.initializeRoot();
      return Collections.singletonMap(root, directory);
    } else {
      List<String> roots = parser.getRoots();
      Map<Root, MemoryDirectory> paths = new LinkedHashMap<>(roots.size());
      for (String root : roots) {
        NamedRoot namedRoot = new NamedRoot(fileSystem, root);
        MemoryDirectory rootDirectory = new MemoryDirectory(namedRoot.getKey(), fileSystem.newEntryCreationContext(namedRoot, attributes));
        rootDirectory.initializeRoot();
        paths.put(namedRoot, rootDirectory);
      }
      return Collections.unmodifiableMap(paths);
    }
  }

  private String getFileSystemKey(URI uri) {
    String scheme = uri.getScheme();
    if (!this.getScheme().equals(scheme)) {
      throw new IllegalArgumentException("Requested unsupported scheme " + scheme + "only scheme: " + this.getScheme() + " is supported");
    }
    String schemeSpecificPart = uri.getSchemeSpecificPart();
    int colonIndex = schemeSpecificPart.indexOf(":/");
    if (colonIndex == -1) {
      return schemeSpecificPart;
    } else {
      return schemeSpecificPart.substring(0, colonIndex);
    }
  }

  private String getFileSystemPath(URI uri) {
    //REVIEW check for getPath() first()?
    String schemeSpecificPart = uri.getSchemeSpecificPart();
    int colonIndex = schemeSpecificPart.indexOf(":/");
    if (colonIndex == -1) {
      return uri.getPath();
    } else {
      return schemeSpecificPart.substring(colonIndex + ":/".length());
    }
  }


  @Override
  public Path getPath(URI uri) {
    String key = this.getFileSystemKey(uri);
    MemoryFileSystem fileSystem = this.fileSystems.get(key);
    if (fileSystem == null) {
      throw new FileSystemNotFoundException("memory file system \"" + key + "\" not found");
    }
    return fileSystem.getPathFromUri(this.getFileSystemPath(uri));
  }


  @Override
  public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
    return this.newFileChannel(path, options, attrs);
  }

  @Override
  public BlockChannel newFileChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
    this.checkSupported(options);
    AbstractPath abstractPath = this.castPath(path);
    MemoryFileSystem memoryFileSystem = abstractPath.getMemoryFileSystem();
    return memoryFileSystem.newFileChannel(abstractPath, options, attrs);
  }

  @Override
  public AsynchronousFileChannel newAsynchronousFileChannel(Path path, Set<? extends OpenOption> options, ExecutorService executor, FileAttribute<?>... attrs) throws IOException {
    BlockChannel fileChannel = this.newFileChannel(path, options, attrs);
    return new AsynchronousBlockChannel(fileChannel,
            executor != null ? executor : this.workExecutor, executor != null ? executor : this.callbackExecutor);
  }

  @Override
  public InputStream newInputStream(Path path, OpenOption... options) throws IOException {
    this.checkSupported(options);
    AbstractPath abstractPath = this.castPath(path);
    MemoryFileSystem memoryFileSystem = abstractPath.getMemoryFileSystem();
    return memoryFileSystem.newInputStream(abstractPath, options);
  }

  @Override
  public OutputStream newOutputStream(Path path, OpenOption... options) throws IOException {
    this.checkSupported(options);
    AbstractPath abstractPath = this.castPath(path);
    MemoryFileSystem memoryFileSystem = abstractPath.getMemoryFileSystem();
    return memoryFileSystem.newOutputStream(abstractPath, options);
  }


  @Override
  public DirectoryStream<Path> newDirectoryStream(Path dir, Filter<? super Path> filter) throws IOException {
    AbstractPath abstractPath = this.castPath(dir);
    MemoryFileSystem memoryFileSystem = abstractPath.getMemoryFileSystem();
    return memoryFileSystem.newDirectoryStream(abstractPath, filter);
  }


  @Override
  public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
    AbstractPath abstractPath = this.castPath(dir);
    MemoryFileSystem memoryFileSystem = abstractPath.getMemoryFileSystem();
    memoryFileSystem.createDirectory(abstractPath, attrs);
  }

  @Override
  public void createSymbolicLink(Path link, Path target, FileAttribute<?>... attrs) throws IOException {
    AbstractPath linkPath = this.castPath(link);
    AbstractPath targetPath = this.castPath(target);
    MemoryFileSystem memoryFileSystem = linkPath.getMemoryFileSystem();
    if (memoryFileSystem != targetPath.getMemoryFileSystem()) {
      throw new IllegalArgumentException("link and target must be on same file system");
    }
    memoryFileSystem.createSymbolicLink(linkPath, targetPath, attrs);
  }

  @Override
  public Path readSymbolicLink(Path link) throws IOException {
    AbstractPath linkPath = this.castPath(link);
    return linkPath.getMemoryFileSystem().readSymbolicLink(linkPath);
  }

  @Override
  public void delete(Path path) throws IOException {
    AbstractPath abstractPath = this.castPath(path);
    MemoryFileSystem memoryFileSystem = abstractPath.getMemoryFileSystem();
    memoryFileSystem.delete(abstractPath);
  }

  @Override
  public void copy(Path source, Path target, CopyOption... options) throws IOException {
    this.copyOrMove(source, target, TwoPathOperation.COPY, options);
  }

  @Override
  public void move(Path source, Path target, CopyOption... options) throws IOException {
    this.copyOrMove(source, target, TwoPathOperation.MOVE, options);
  }

  private void copyOrMove(Path source, Path target, TwoPathOperation operation, CopyOption... options) throws IOException {
    this.checkSupported(options);
    AbstractPath sourcePath = this.castPath(source);
    AbstractPath targetPath = this.castPath(target);
    MemoryFileSystem sourceFileSystem = sourcePath.getMemoryFileSystem();
    MemoryFileSystem targetFileSystem = targetPath.getMemoryFileSystem();
    if (sourceFileSystem == targetFileSystem) {
      sourceFileSystem.copyOrMove(sourcePath, targetPath, operation, options);
    } else {
      MemoryFileSystem.copyOrMoveBetweenFileSystems(sourceFileSystem, targetFileSystem, sourcePath, targetPath, operation, options);
    }
  }



  @Override
  public boolean isSameFile(Path path, Path path2) throws IOException {
    FileSystemProvider provider = provider(path);
    if (provider != this) {
      return Files.isSameFile(path2, path);
    }
    FileSystemProvider provider2 = provider(path2);
    if (provider2 != this) {
      return false;
    }

    if (path.getFileSystem() != path2.getFileSystem()) {
      return false;
    }

    if (path.equals(path2)) {
      return true;
    }

    // isn't atomic but that's fine I guess
    return path.toRealPath().equals(path2.toRealPath());
  }

  private static FileSystemProvider provider(Path path) {
    return path.getFileSystem().provider();
  }

  @Override
  public boolean isHidden(Path path) throws IOException {
    AbstractPath abstractPath = this.castPath(path);
    return abstractPath.getMemoryFileSystem().isHidden(abstractPath);
  }


  @Override
  public FileStore getFileStore(Path path) throws IOException {
    return this.castPath(path).getMemoryFileSystem().getFileStore();
  }

  private AbstractPath castPath(Path path) {
    if (!(path instanceof AbstractPath)) {
      throw new ProviderMismatchException("expected a path of provider " + SCHEME + " but got " + path.getFileSystem().provider().getScheme());
    }
    return (AbstractPath) path;
  }


  @Override
  public void checkAccess(Path path, AccessMode... modes) throws IOException {
    this.checkSupported(modes);
    AbstractPath abstractPath = this.castPath(path);
    MemoryFileSystem memoryFileSystem = abstractPath.getMemoryFileSystem();
    memoryFileSystem.checkAccess(abstractPath, modes);
  }


  @Override
  public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type, LinkOption... options) {
    AbstractPath abstractPath = this.castPath(path);
    MemoryFileSystem memoryFileSystem = abstractPath.getMemoryFileSystem();
    return memoryFileSystem.getLazyFileAttributeView(abstractPath, type, options);
  }


  @Override
  public <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type, LinkOption... options) throws IOException {
    AbstractPath abstractPath = this.castPath(path);
    MemoryFileSystem memoryFileSystem = abstractPath.getMemoryFileSystem();
    return memoryFileSystem.readAttributes(abstractPath, type, options);
  }


  @Override
  public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
    AbstractPath abstractPath = this.castPath(path);
    MemoryFileSystem memoryFileSystem = abstractPath.getMemoryFileSystem();
    return memoryFileSystem.readAttributes(abstractPath, attributes, options);
  }


  @Override
  public void setAttribute(Path path, String attribute, Object value, LinkOption... options) throws IOException {
    AbstractPath abstractPath = this.castPath(path);
    MemoryFileSystem memoryFileSystem = abstractPath.getMemoryFileSystem();
    memoryFileSystem.setAttribute(abstractPath, attribute, value, options);
  }

  void close(MemoryFileSystem fileSystem) {
    String key = fileSystem.getKey();
    this.fileSystems.remove(key);
  }

  void close() {
    this.workExecutor.shutdownNow();
    this.callbackExecutor.shutdownNow();
  }

  private void checkSupported(CopyOption... options)  {
    if (options == null) {
      return;
    }
    for (CopyOption copyOption : options) {
      if (copyOption != StandardCopyOption.ATOMIC_MOVE
              && copyOption != StandardCopyOption.COPY_ATTRIBUTES
              && copyOption != StandardCopyOption.REPLACE_EXISTING
              && copyOption != LinkOption.NOFOLLOW_LINKS) {
        throw new UnsupportedOperationException("copy option: " + copyOption + " not supported");
      }
    }
  }

  private void checkSupported(OpenOption... options)  {
    // TODO implement
  }

  private void checkSupported(Set<? extends OpenOption> options)  {
    // TODO implement
  }

  private void checkSupported(AccessMode... modes)  {
    if (modes == null || modes.length == 0) {
      return;
    }
    for (AccessMode mode : modes) {
      if (!(mode == AccessMode.READ || mode == AccessMode.WRITE || mode == AccessMode.EXECUTE)) {
        throw new UnsupportedOperationException("mode " + mode + " not supported");
      }
    }
  }


  static final class NamedDaemonThreadFactory implements ThreadFactory {

    private final String name;

    NamedDaemonThreadFactory(String name) {
      this.name = name;
    }

    @Override
    public Thread newThread(Runnable r) {
      Thread thread = new Thread(r, this.name);
      thread.setDaemon(true);
      return thread;
    }

  }

}
