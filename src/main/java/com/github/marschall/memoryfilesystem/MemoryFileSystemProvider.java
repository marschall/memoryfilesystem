package com.github.marschall.memoryfilesystem;

import java.io.IOException;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.text.Collator;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Creates memory file systems instance.
 * 
 * <p>This class should not be used directly. Instead
 * {@link java.nio.file.FileSystems#newFileSystem(URI, Map)}
 * should be used.</p>
 */
public final class MemoryFileSystemProvider extends FileSystemProvider {

  private final ConcurrentMap<String, MemoryFileSystem> fileSystems;

  public MemoryFileSystemProvider() {
    this.fileSystems = new ConcurrentHashMap<>();
  }


  static final String SCHEME = "memory";


  @Override
  public String getScheme() {
    return SCHEME;
  }


  @Override
  public FileSystem newFileSystem(URI uri, Map<String, ?> env) throws IOException {
    this.valideUri(uri);
    String key = this.getFileSystemKey(uri);
    EnvironmentParser parser = new EnvironmentParser(env);
    MemoryFileSystem fileSystem = createNewFileSystem(key, parser);
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

  private MemoryFileSystem createNewFileSystem(String key, EnvironmentParser parser) {
    ClosedFileSystemChecker checker = new ClosedFileSystemChecker();
    String separator = parser.getSeparator();
    StringTransformer pathTransformer = parser.getPathTransformer();
    Collator collator = parser.getCollator();
    MemoryFileStore memoryStore = new MemoryFileStore(key, checker);
    MemoryUserPrincipalLookupService userPrincipalLookupService = this.createUserPrincipalLookupService(parser, checker);
    PathParser pathParser = this.buildPathParser(parser);
    MemoryFileSystem fileSystem = new MemoryFileSystem(separator, pathParser, this, memoryStore,
            userPrincipalLookupService, checker, pathTransformer, collator);
    fileSystem.setRootDirectories(this.buildRootsDirectories(parser, fileSystem));
    String defaultDirectory = parser.getDefaultDirectory();
    fileSystem.setCurrentWorkingDirectory(defaultDirectory);
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
      return new SingleEmptyRootPathParser(separator);
    } else {
      return new MultipleNamedRootsPathParser(separator, parser.getPathTransformer());
    }
  }

  private Map<Root, MemoryDirectory> buildRootsDirectories(EnvironmentParser parser, MemoryFileSystem fileSystem) {
    if (parser.isSingleEmptyRoot()) {
      Root root = new EmptyRoot(fileSystem);
      MemoryDirectory directory = new MemoryDirectory();
      return Collections.singletonMap(root, directory);
    } else {
      List<String> roots = parser.getRoots();
      Map<Root, MemoryDirectory> paths = new LinkedHashMap<>(roots.size());
      for (String root : roots) {
        paths.put(new NamedRoot(fileSystem, root), new MemoryDirectory());
      }
      return Collections.unmodifiableMap(paths);
    }
  }

  private String getFileSystemKey(URI uri) {
    String schemeSpecificPart = uri.getSchemeSpecificPart();
    int colonIndex = schemeSpecificPart.indexOf("://");
    if (colonIndex == -1) {
      return schemeSpecificPart;
    } else {
      return schemeSpecificPart.substring(0, colonIndex);
    }
  }
  
  private String getFileSystemPath(URI uri) {
    //REVIEW check for getPath() first()?
    String schemeSpecificPart = uri.getSchemeSpecificPart();
    int colonIndex = schemeSpecificPart.indexOf("://");
    if (colonIndex == -1) {
      return uri.getPath();
    } else {
      return schemeSpecificPart.substring(colonIndex + "://".length());
    }
  }


  @Override
  public Path getPath(URI uri) {
    String key = getFileSystemKey(uri);
    MemoryFileSystem fileSystem = this.fileSystems.get(key);
    if (fileSystem == null) {
      throw new FileSystemNotFoundException("memory file system \"" + key + "\" not found");
    }
    return fileSystem.getPath(getFileSystemPath(uri));
  }


  @Override
  public SeekableByteChannel newByteChannel(Path path,
      Set<? extends OpenOption> options, FileAttribute<?>... attrs)
          throws IOException {

    AbstractPath abstractPath = this.castPath(path);
    MemoryFileSystem memoryFileSystem = abstractPath.getMemoryFileSystem();
    return memoryFileSystem.newByteChannel(abstractPath, options, attrs);
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
  public void delete(Path path) throws IOException {
    AbstractPath abstractPath = this.castPath(path);
    MemoryFileSystem memoryFileSystem = abstractPath.getMemoryFileSystem();
    memoryFileSystem.delete(abstractPath, path);
  }


  @Override
  public void copy(Path source, Path target, CopyOption... options)
      throws IOException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException();
  }


  @Override
  public void move(Path source, Path target, CopyOption... options)
      throws IOException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException();
  }


  @Override
  public boolean isSameFile(Path path, Path path2) throws IOException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException();
  }


  @Override
  public boolean isHidden(Path path) throws IOException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException();
  }


  @Override
  public FileStore getFileStore(Path path) throws IOException {
    return this.castPath(path).getMemoryFileSystem().getFileStore();
  }

  private AbstractPath castPath(Path path) {
    if (!(path instanceof AbstractPath)) {
      throw new IllegalArgumentException("can only handle paths of this file system provider");
    }
    return (AbstractPath) path;
  }


  @Override
  public void checkAccess(Path path, AccessMode... modes) throws IOException {
    AbstractPath abstractPath = this.castPath(path);
    MemoryFileSystem memoryFileSystem = abstractPath.getMemoryFileSystem();
    memoryFileSystem.checkAccess(abstractPath, modes);
  }


  @Override
  public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type, LinkOption... options) {
    // TODO Auto-generated method stub
    FileSystem fileSystem = path.getFileSystem();
    throw new UnsupportedOperationException();
  }


  @Override
  public <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type, LinkOption... options) throws IOException {
    AbstractPath abstractPath = this.castPath(path);
    MemoryFileSystem memoryFileSystem = abstractPath.getMemoryFileSystem();
    return memoryFileSystem.readAttributes(abstractPath, type, options);
  }


  @Override
  public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException();
  }


  @Override
  public void setAttribute(Path path, String attribute, Object value, LinkOption... options) throws IOException {
    // TODO Auto-generated method stub
    int colonIndex = attribute.indexOf(':');
    String viewName;
    if (colonIndex == -1) {
      viewName = "basic";
    } else {
      viewName = attribute.substring(0, colonIndex);
    }
    // TODO check bounds
    String attributeName = attribute.substring(colonIndex + 1, attribute.length());
    throw new UnsupportedOperationException();
  }

  void close(MemoryFileSystem fileSystem) {
    String key = fileSystem.getKey();
    this.fileSystems.remove(key);
  }

}
