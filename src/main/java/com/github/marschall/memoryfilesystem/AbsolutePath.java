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
import java.text.CollationKey;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
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
    String fileSystemKey = this.getMemoryFileSystem().getKey();
    List<String> nameElements = this.getNameElements();
    int sizeEsitmate =
            fileSystemKey.length()
            + 3 // "://".length
            + nameElements.size() // n * '/'
            + totalLength(nameElements);
    StringBuilder schemeSpecificPart = new StringBuilder(sizeEsitmate);
    schemeSpecificPart.append(fileSystemKey);
    schemeSpecificPart.append("://");
    for (String element : nameElements) {
      schemeSpecificPart.append('/');
      schemeSpecificPart.append(element);
    }
    try {
      return new URI(SCHEME, schemeSpecificPart.toString(), null);
    } catch (URISyntaxException e) {
      throw new AssertionError("could not create URI");
    }
  }

  private static int totalLength(List<String> elements) {
    int totalLength = 0;
    for (String each : elements) {
      totalLength += each.length();
    }
    return totalLength;
  }

  @Override
  public Path toAbsolutePath() {
    return this;
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
  int compareToNonRoot(AbstractPath other) {
    if (!other.isAbsolute()) {
      return -1;
    }
    return this.compareNameElements(((AbsolutePath) other).getNameElements());
  }

  @Override
  boolean startsWith(AbstractPath other) {
    if (!other.isAbsolute()) {
      return false;
    }
    if (other.isRoot()) {
      // is other my root?
      return other.equals(this.getRoot());
    }
    
    // named roots under windows
    if (!this.getRoot().startsWith(other.getRoot())) {
      return false;
    }
    
    if (other instanceof ElementPath) {
      ElementPath otherPath = (ElementPath) other;
      int otherNameCount = otherPath.getNameCount();
      if (otherNameCount > this.getNameCount()) {
        return false;
      }
      Collator collator = this.getMemoryFileSystem().getCollator();
      // otherNameCount smaller or equal to this.getNameCount()
      for (int i = 0; i < otherNameCount; ++i) {
        String thisElement = this.getNameElement(i);
        String otherElement = otherPath.getNameElement(i);
        if (!collator.equals(thisElement, otherElement)) {
          return false;
        }
      }
      return true;
    } else {
      throw new IllegalArgumentException("can't check for #startsWith against " + other);
    }
  }


  @Override
  boolean endsWith(AbstractPath other) {
    if (other.isAbsolute()) {
      return this.equals(other);
    }
    if (other.isRoot()) {
      return false;
    }
    
    if (other instanceof ElementPath) {
      return this.endsWithRelativePath(other);
    } else {
      throw new IllegalArgumentException("can't check for #startsWith against " + other);
    }
  }

  @Override
  Path resolve(AbstractPath other) {
    if (other instanceof ElementPath) {
      ElementPath otherPath = (ElementPath) other;
      List<String> resolvedElements = CompositeList.create(this.getNameElements(), otherPath.getNameElements());
      return createAboslute(this.getMemoryFileSystem(), this.root, resolvedElements);
    } else {
      throw new IllegalArgumentException("can't resolve" + other);
    }
  }


  @Override
  Path resolveSibling(AbstractPath other) {
    if (other.isAbsolute()) {
      return other;
    }
    if (other instanceof ElementPath) {
      ElementPath otherPath = (ElementPath) other;
      List<String> resolvedElements = CompositeList.create(this.getNameElements().subList(0, getNameCount() - 1), otherPath.getNameElements());
      return createAboslute(this.getMemoryFileSystem(), this.root, resolvedElements);
    } else {
      throw new IllegalArgumentException("can't resolve" + other);
    }
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
    // takes file system key into account
    return this.root.equals(other.root)
            && this.equalElementsAs(other.getNameElements());
  }
  

  @Override
  public int hashCode() {
    // TODO expensive, safe
    Collator collator = this.getMemoryFileSystem().getCollator();
    int result = 17;
    result = 31 * result + this.root.hashCode();
    for (String each : this.getNameElements()) {
      CollationKey collationKey = collator.getCollationKey(each);
      result = 31 * result + Arrays.hashCode(collationKey.toByteArray());
    }
    return result;
  }

}
