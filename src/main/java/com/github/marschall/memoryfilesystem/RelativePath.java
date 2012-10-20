package com.github.marschall.memoryfilesystem;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchEvent.Modifier;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.text.CollationKey;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

final class RelativePath extends NonEmptyPath {
  

  RelativePath(MemoryFileSystem fileSystem, List<String> nameElements) {
    super(fileSystem, nameElements);
  }

  @Override
  public boolean isAbsolute() {
    return false;
  }
  
  @Override
  public String toString() {
    StringBuilder buffer = new StringBuilder();
    String separator = this.getFileSystem().getSeparator();
    boolean first = true;
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
  public Path getRoot() {
    return null;
  }

  @Override
  public Path getParent() {
    if (this.getNameElements().size() == 1) {
      return null;
    } else {
      List<String> subList = this.getNameElements().subList(0, this.getNameElements().size() - 1);
      return createRelative(getMemoryFileSystem(), subList);
    }
  }

  @Override
  public Path getName(int index) {
    int nameCount = this.getNameCount();
    if (index < 0) {
      throw new IllegalArgumentException("index must be positive but was " + index);
    }
    if (index >= nameCount) {
      throw new IllegalArgumentException("index must not be bigger than " + (nameCount - 1) +  " but was " + index);
    }
    
    if (nameCount == 1) {
      return this;
    } else {
      return createRelative(getMemoryFileSystem(), this.getNameElements().get(index));
    }
  }

  @Override
  public Path subpath(int beginIndex, int endIndex) {
    this.checkNameRange(beginIndex, endIndex);
    if (endIndex - beginIndex == this.getNameCount()) {
      return this;
    } else {
      return createRelative(getMemoryFileSystem(), this.getNameElements().subList(beginIndex, endIndex));
    }
  }
  
  @Override
  Path newInstance(MemoryFileSystem fileSystem, List<String> pathElements) {
    return createRelative(this.getMemoryFileSystem(), pathElements);
  }

  @Override
  public URI toUri() {
    return this.toAbsolutePath().toUri();
  }

  @Override
  public Path toAbsolutePath() {
    return this.getMemoryFileSystem().getDefaultPath().resolve(this);
  }

  @Override
  public WatchKey register(WatchService watcher, Kind<?>[] events, Modifier... modifiers) throws IOException {
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
    if (other.isAbsolute()) {
      return 1;
    }
    if (other.getNameCount() == 0) {
      return 1;
    }
    return this.compareNameElements(((RelativePath) other).getNameElements());
  }

  @Override
  boolean startsWith(AbstractPath other) {
    if (other.isAbsolute()) {
      return false;
    }
    if (other.isRoot()) {
      return false;
    }
    if (other instanceof ElementPath) {
      ElementPath otherPath = (ElementPath) other;
      int otherNameCount = otherPath.getNameCount();
      if (otherNameCount == 0) {
        // empty path
        return false;
      }
      if (otherNameCount > this.getNameCount()) {
        return false;
      }
      // otherNameCount smaller or equal to this.getNameCount()
      Collator collator = this.getMemoryFileSystem().getCollator();
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
      return false;
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
      return createRelative(this.getMemoryFileSystem(), resolvedElements);
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
      return createRelative(this.getMemoryFileSystem(), resolvedElements);
    } else {
      throw new IllegalArgumentException("can't resolve" + other);
    }
  }

  @Override
  Path relativize(AbstractPath other) {
    if (other.isAbsolute()) {
      // only support relativization against relative paths
      throw new IllegalArgumentException("can only relativize a relative path against a relative path");
    }
    if (other instanceof ElementPath) {
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
    if (!(obj instanceof RelativePath)) {
      return false;
    }
    ElementPath other = (ElementPath) obj;
    // compareTo take memory file system key into account to ensure 
    // a.compareTo(b) == 0 iff a.equals(b)
    return this.getMemoryFileSystem().getKey().equals(other.getMemoryFileSystem().getKey())
            && this.equalElementsAs(other.getNameElements());
  }
  
  @Override
  public int hashCode() {
    // TODO expensive, safe
    Collator collator = this.getMemoryFileSystem().getCollator();
    int result = 17;
    for (String each : this.getNameElements()) {
      CollationKey collationKey = collator.getCollationKey(each);
      result = 31 * result + Arrays.hashCode(collationKey.toByteArray());
    }
    return result;
  }

  protected List<String> handleDotDotNormalizationNotYetModified(List<String> nameElements,
          int nameElementsSize, int i) {
    // copy everything preceding the element before ".." unless it's ".."
    if (i > 0 && !nameElements.get(i - 1).equals("..")) {
      List<String> normalized = new ArrayList<>(nameElementsSize - 1);
      if (i > 1) {
        normalized.addAll(nameElements.subList(0, i - 1));
      }
      return normalized;
    } else {
      return nameElements;
    }
    
  }

  protected void handleDotDotNormalizationAlreadyModified(List<String> normalized) {
    int lastIndex = normalized.size() - 1;
    if (!normalized.get(lastIndex).equals("..")) {
      // "../.." has to be preserved
      normalized.remove(lastIndex);
    } else {
      // if there is already a ".." just add a ".."
      normalized.add("..");
    }
  }

  protected List<String> handleSingleDotDot(List<String> normalized) {
    return normalized;
  }

}
