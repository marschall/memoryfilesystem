package com.github.marschall.memoryfilesystem;


import static com.github.marschall.memoryfilesystem.MemoryFileSystemProvider.SCHEME;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchEvent.Modifier;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class AbsolutePath extends NonEmptyPath {
  
  private final Root root;

  AbsolutePath(MemoryFileSystem fileSystem, Root root, List<String> nameElements) {
    super(fileSystem, nameElements);
    this.root = root;
  }

  @Override
  public boolean isAbsolute() {
    return true;
  }

  @Override
  public Path getRoot() {
    return this.root;
  }
  
  @Override
  public String toString() {
    StringBuilder buffer = new StringBuilder();
    buffer.append(this.root);
    boolean first = true;
    String separator = this.getFileSystem().getSeparator();
    for (String element : this.getNameElements()) {
      if (!first) {
        buffer.append(separator);
      }
      buffer.append(element);
      first = false;
    }
    return buffer.toString();
  }

  @Override
  public Path getParent() {
    if (this.getNameElements().size() == 1) {
      return this.root;
    } else {
      List<String> subList = this.getNameElements().subList(0, this.getNameElements().size() - 1);
      return createAboslute(getMemoryFileSystem(), this.root, subList);
    }
  }


  @Override
  public Path getName(int index) {
    if (index < 0) {
      throw new IllegalArgumentException("index must be positive but was " + index);
    }
    if (index >= this.getNameCount()) {
      throw new IllegalArgumentException("index must not be bigger than " + (this.getNameCount() - 1) +  " but was " + index);
    }
    return createRelative(getMemoryFileSystem(), this.getNameElements().get(index));
  }


  @Override
  public Path subpath(int beginIndex, int endIndex) {
    this.checkNameRange(beginIndex, endIndex);
    if (endIndex - beginIndex == this.getNameCount()) {
      return createRelative(getMemoryFileSystem(), this.getNameElements());
    } else {
      return createRelative(getMemoryFileSystem(), this.getNameElements().subList(beginIndex, endIndex));
    }
  }
  
  @Override
  protected List<String> handleSingleDotDot(List<String> normalized) {
    return Collections.emptyList();
  }
  
  @Override
  protected void handleDotDotNormalizationAlreadyModified(List<String> normalized) {
    normalized.remove(normalized.size() - 1);
  }
  
  @Override
  protected List<String> handleDotDotNormalizationNotYetModified(List<String> nameElements, int nameElementsSize, int i) {
    // copy everything preceding the element before ".."
    List<String> normalized = new ArrayList<>(nameElementsSize - 1);
    if (i > 1) {
      normalized.addAll(nameElements.subList(0, i - 1));
    }
    return normalized;
  }
  
  @Override
  Path newInstance(MemoryFileSystem fileSystem, List<String> pathElements) {
    return createAboslute(fileSystem, this.root, pathElements);
  }


  @Override
  public URI toUri() {
    //TODO estimate size.
    StringBuilder schemeSpecificPart = new StringBuilder();
    schemeSpecificPart.append(this.getMemoryFileSystem().getKey());
    schemeSpecificPart.append("://");
    for (String element : this.getNameElements()) {
      schemeSpecificPart.append('/');
      schemeSpecificPart.append(element);
    }
    try {
      return new URI(SCHEME, schemeSpecificPart.toString(), null);
    } catch (URISyntaxException e) {
      throw new AssertionError("could not create URI");
    }
  }
  
  public static void main(String[] args) throws URISyntaxException {
    URI uri = new URI("memory", "authority", "/a/b", null, null);
    System.out.println(uri);
    
  }


  @Override
  public Path toAbsolutePath() {
    return this;
  }


  @Override
  public Path toRealPath(LinkOption... options) throws IOException {
    // TODO Auto-generated function stub
    throw new UnsupportedOperationException();
  }


  @Override
  public WatchKey register(WatchService watcher, Kind<?>[] events,
          Modifier... modifiers) throws IOException {
    // TODO report bug
    // TODO Auto-generated function stub
    throw new UnsupportedOperationException();
  }


  @Override
  public WatchKey register(WatchService watcher, Kind<?>... events) throws IOException {
    // TODO Auto-generated function stub
    throw new UnsupportedOperationException();
  }


  @Override
  public int compareTo(Path other) {
    // TODO Auto-generated function stub
    throw new UnsupportedOperationException();
  }


  @Override
  boolean startsWith(AbstractPath other) {
    // TODO Auto-generated function stub
    throw new UnsupportedOperationException();
  }


  @Override
  boolean endsWith(AbstractPath other) {
    // TODO Auto-generated function stub
    throw new UnsupportedOperationException();
  }


  @Override
  Path resolve(AbstractPath other) {
    if (other instanceof ElementPath) {
      // TODO root?
      ElementPath otherPath = (ElementPath) other;
      List<String> resolvedElements = CompositeList.create(this.getNameElements(), otherPath.getNameElements());
      return createAboslute(this.getMemoryFileSystem(), this.root, resolvedElements);
    } else {
      throw new IllegalArgumentException("can't resolve" + other);
    }
  }


  @Override
  Path resolveSibling(AbstractPath other) {
    // TODO Auto-generated function stub
    throw new UnsupportedOperationException();
  }


  @Override
  Path relativize(AbstractPath other) {
    if (!other.isAbsolute()) {
      // only support relativization against absolute paths
      throw new IllegalArgumentException("can only relativize an absolute path against an absolute path");
    }
    if (!other.getRoot().equals(this.root)) {
      // only support relativization against paths with same root
      throw new IllegalArgumentException("paths must have the same root");
    }
    //TODO normalize
    if (other.equals(this.root)) {
      // other is my root, easy 
      return createRelative(this.getMemoryFileSystem(), HomogenousList.create("..", this.getNameCount()));
    } else if (other instanceof ElementPath) {
      // normal case
      return this.buildRelativePathAgainst(other);
    } else {
      // unknown case
      throw new IllegalArgumentException("unsupported path argument");
    }
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof AbsolutePath)) {
      return false;
    }
    AbsolutePath other = (AbsolutePath) obj;
    return this.root.equals(other.root)
            && this.getNameElements().equals(other.getNameElements());
  }
  

  @Override
  public int hashCode() {
    // TODO disturb context
    return this.root.hashCode() ^ this.getNameElements().hashCode();
  }

}
