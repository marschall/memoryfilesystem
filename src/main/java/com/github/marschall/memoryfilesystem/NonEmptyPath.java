package com.github.marschall.memoryfilesystem;

import static java.lang.Math.min;

import java.io.IOException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

abstract class NonEmptyPath extends ElementPath {

  private final List<String> nameElements;

  NonEmptyPath(MemoryFileSystem fileSystem, List<String> nameElements) {
    super(fileSystem);
    this.nameElements = nameElements;
  }

  @Override
  List<String> getNameElements() {
    return this.nameElements;
  }

  @Override
  String getNameElement(int index) {
    return this.nameElements.get(index);
  }

  @Override
  String getLastNameElement() {
    return this.nameElements.get(this.nameElements.size() - 1);
  }

  int compareNameElements(List<String> otherElements) {
    int thisSize = this.nameElements.size();
    int otherSize = otherElements.size();
    Collator collator = this.getMemoryFileSystem().getCollator();
    for (int i = 0; i < thisSize; i++) {
      if (i == otherSize) {
        // bail out before accessing
        return 1;
      }
      String thisElement = this.nameElements.get(i);
      String otherElement = otherElements.get(i);
      int comparison = collator.compare(thisElement, otherElement);
      if (comparison != 0) {
        return comparison;
      }
    }
    if (otherSize > thisSize) {
      return -1;
    } else {
      return 0;
    }
  }

  boolean equalElementsAs(List<String> otherElements) {
    int thisSize = this.nameElements.size();
    if (thisSize != otherElements.size()) {
      return false;
    }
    Collator collator = this.getMemoryFileSystem().getCollator();
    for (int i = 0; i < thisSize; i++) {
      String thisElement = this.nameElements.get(i);
      String otherElement = otherElements.get(i);
      if (!collator.equals(thisElement, otherElement)) {
        return false;
      }
    }

    return true;
  }


  @Override
  public Path getFileName() {
    String lastElement = this.nameElements.get(this.nameElements.size() - 1);
    return createRelative(this.getMemoryFileSystem(), lastElement);
  }

  @Override
  public Path toRealPath(LinkOption... options) throws IOException {
    return this.getMemoryFileSystem().toRealPath(this, options);
  }

  @Override
  boolean isRoot() {
    return false;
  }


  @Override
  public int getNameCount() {
    return this.nameElements.size();
  }


  @Override
  public boolean startsWith(String other) {
    Path path = this.getMemoryFileSystem().getPath(other);
    return this.startsWith(path);
  }

  void checkNameRange(int beginIndex, int endIndex) {
    int nameCount = this.getNameCount();
    if (beginIndex < 0) {
      throw new IllegalArgumentException("beginIndex must not be negative but was " + beginIndex);
    }
    if (beginIndex >= nameCount) {
      throw new IllegalArgumentException("beginIndex must be smaller than " + nameCount + " but was " + beginIndex);
    }
    if (beginIndex >= endIndex) {
      throw new IllegalArgumentException("beginIndex must be smaller than " + beginIndex + " but was " + beginIndex);
    }
    if (endIndex > nameCount) {
      throw new IllegalArgumentException("endIndex must be smaller than or equal to " + nameCount + " but was " + endIndex);
    }
  }


  @Override
  public boolean endsWith(String other) {
    Path path = this.getMemoryFileSystem().getPath(other);
    return this.endsWith(path);
  }


  @Override
  public Iterator<Path> iterator() {
    return new ElementIterator(this.getMemoryFileSystem(), this.nameElements.iterator());
  }

  protected int firstDifferenceIndex(List<?> l1, List<?> l2) {
    int endIndex = min(l1.size(), l2.size());
    for (int i = 0; i < endIndex; ++i) {
      if (!l1.get(i).equals(l2.get(i))) {
        return i;
      }
    }
    return endIndex;
  }

  Path buildRelativePathAgainst(AbstractPath other) {
    ElementPath otherPath = (ElementPath) other;
    int firstDifferenceIndex = this.firstDifferenceIndex(this.getNameElements(), otherPath.getNameElements());
    List<String> first = Collections.emptyList();
    if (firstDifferenceIndex < this.getNameCount()) {
      first = ParentReferenceList.create(this.getNameCount() - firstDifferenceIndex);
    }
    List<String> second = Collections.emptyList();
    if (firstDifferenceIndex < other.getNameCount()) {
      second = otherPath.getNameElements().subList(firstDifferenceIndex, otherPath.getNameCount());
    }
    List<String> relativeElements = CompositeList.create(first, second);
    return createRelative(this.getMemoryFileSystem(), relativeElements);
  }

  abstract List<String> handleDotDotNormalizationNotYetModified(List<String> nameElements, int nameElementsSize, int i);

  abstract void handleDotDotNormalizationAlreadyModified(List<String> normalized);

  abstract List<String> handleSingleDotDot(List<String> normalized);

  @Override
  public Path normalize() {
    List<String> nameElements = this.getNameElements();
    int nameElementsSize = nameElements.size();
    List<String> normalized = nameElements;
    boolean modified = false;

    for (int i = 0; i < nameElementsSize; ++i) {
      String each = nameElements.get(i);

      if (each.equals(".")) {
        if (!modified) {
          if (nameElementsSize == 1) {
            // path is just "."
            normalized = Collections.emptyList();
            modified = true;
            break;
          }
          if (nameElementsSize == 2) {
            // path is either "/a/." or "/./a"
            String element = i == 0 ? nameElements.get(1) : nameElements.get(0);
            normalized = Collections.singletonList(element);
            modified = true;
            break;
          }

          // copy everything preceding a
          normalized = new ArrayList<>(nameElementsSize - 1);
          if (i > 0) {
            normalized.addAll(nameElements.subList(0, i));
          }
          modified = true;
        }

        // ignore
        continue;
      }

      if (each.equals("..")) {
        if (modified) {
          // just remove the last entry if possible
          if (!normalized.isEmpty()) {
            this.handleDotDotNormalizationAlreadyModified(normalized);
          }
        } else {
          if (nameElementsSize == 1) {
            // path is just "/.."
            normalized = this.handleSingleDotDot(normalized);
            modified = normalized != nameElements;
            break;
          } else {
            normalized = this.handleDotDotNormalizationNotYetModified(nameElements, nameElementsSize, i);
            modified = normalized != nameElements;
          }
        }
        continue;
      }

      if (modified) {
        normalized.add(each);
      }

    }
    if (modified) {
      return this.newInstance(this.getMemoryFileSystem(), normalized);
    } else {
      return this;
    }
  }

  abstract Path newInstance(MemoryFileSystem fileSystem, List<String> pathElements);

  protected boolean endsWithRelativePath(AbstractPath other) {
    ElementPath otherPath = (ElementPath) other;
    int otherNameCount = otherPath.getNameCount();
    int thisNameCount = this.getNameCount();
    if (otherNameCount == 0) {
      // empty path
      return false;
    }

    if (otherNameCount > thisNameCount) {
      return false;
    }
    // otherNameCount smaller or equal to this.getNameCount()
    int offset = thisNameCount - otherNameCount;
    Collator collator = this.getMemoryFileSystem().getCollator();
    for (int i = 0; i < otherNameCount; ++i) {
      String thisElement = this.getNameElement(i + offset);
      String otherElement = otherPath.getNameElement(i);
      if (!collator.equals(thisElement, otherElement)) {
        return false;
      }
    }
    return true;
  }

  static final class ElementIterator implements Iterator<Path> {

    private final MemoryFileSystem fileSystem;
    private final Iterator<String> nameIterator;

    ElementIterator(MemoryFileSystem fileSystem, Iterator<String> nameIterator) {
      this.fileSystem = fileSystem;
      this.nameIterator = nameIterator;
    }

    @Override
    public boolean hasNext() {
      return this.nameIterator.hasNext();
    }

    @Override
    public Path next() {
      return createRelative(this.fileSystem, this.nameIterator.next());
    }

    @Override
    public void forEachRemaining(Consumer<? super Path> action) {
      while (this.nameIterator.hasNext()) {
        String nextName = this.nameIterator.next();
        AbstractPath nextPath = createRelative(this.fileSystem, nextName);
        action.accept(nextPath);
      }
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException("can't remove from a path iterator");
    }

  }

}
