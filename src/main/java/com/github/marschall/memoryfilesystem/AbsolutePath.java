package com.github.marschall.memoryfilesystem;


import static com.github.marschall.memoryfilesystem.MemoryFileSystemProvider.SCHEME;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
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
    // TODO estimate size
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
      return createAbsolute(this.getMemoryFileSystem(), this.root, subList);
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
    return createRelative(this.getMemoryFileSystem(), this.getNameElements().get(index));
  }


  @Override
  public Path subpath(int beginIndex, int endIndex) {
    this.checkNameRange(beginIndex, endIndex);
    if (endIndex - beginIndex == this.getNameCount()) {
      return createRelative(this.getMemoryFileSystem(), this.getNameElements());
    } else {
      return createRelative(this.getMemoryFileSystem(), this.getNameElements().subList(beginIndex, endIndex));
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
    return createAbsolute(fileSystem, this.root, pathElements);
  }


  @Override
  public URI toUri() {
    String fileSystemKey = this.getMemoryFileSystem().getKey();
    boolean isNamed = this.root.isNamed();
    List<String> nameElements = this.getNameElements();
    boolean isDirectory = this.getMemoryFileSystem().isDirectory(this);
    int sizeEstimate =
            fileSystemKey.length()
            + 3 // "://".length
            + (isNamed ? 3 :0) // c:/
            + nameElements.size() // n * '/'
            + totalLength(nameElements)
            + (isDirectory ? 1 : 0);
    StringBuilder schemeSpecificPart = new StringBuilder(sizeEstimate);
    schemeSpecificPart.append(fileSystemKey);
    schemeSpecificPart.append("://");
    if (isNamed) {
      schemeSpecificPart.append('/');
      schemeSpecificPart.append(this.root.getKey());
      schemeSpecificPart.append(':');
    }
    for (String element : nameElements) {
      schemeSpecificPart.append('/');
      schemeSpecificPart.append(element);
    }

    if (isDirectory) {
      // Java expects URIs and URLs for directories to end with a forward slash.
      // Otherwise, URLClassLoader and other mechanisms will treat it as if it were
      // a file rather than a directory.
      schemeSpecificPart.append('/');
    }

    try {
      return new URI(SCHEME, schemeSpecificPart.toString(), null);
    } catch (URISyntaxException e) {
      throw new AssertionError("could not create URI", e);
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
  int compareToNonRoot(ElementPath other) {
    if (!other.isAbsolute()) {
      return -1;
    }
    return this.compareNameElements(other.getNameElements());
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
  Path resolve(ElementPath other) {
    List<String> resolvedElements = CompositeList.create(this.getNameElements(), other.getNameElements());
    return createAbsolute(this.getMemoryFileSystem(), this.root, resolvedElements);
  }


  @Override
  Path resolveSibling(AbstractPath other) {
    if (other.isAbsolute()) {
      return other;
    }
    if (other instanceof ElementPath) {
      ElementPath otherPath = (ElementPath) other;
      List<String> resolvedElements = CompositeList.create(this.getNameElements().subList(0, this.getNameCount() - 1), otherPath.getNameElements());
      return createAbsolute(this.getMemoryFileSystem(), this.root, resolvedElements);
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
      return createRelative(this.getMemoryFileSystem(), ParentReferenceList.create(this.getNameCount()));
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
