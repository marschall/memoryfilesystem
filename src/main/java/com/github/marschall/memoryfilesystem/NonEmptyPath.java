package com.github.marschall.memoryfilesystem;

import static java.lang.Math.min;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

abstract class NonEmptyPath extends ElementPath {
  
  private final List<String> nameElements;

  NonEmptyPath(MemoryFileSystem fileSystem, List<String> nameElements) {
    super(fileSystem);
    this.nameElements = nameElements;
  }
  
  List<String> getNameElements() {
    return this.nameElements;
  }
  
  String getNameElement(int index) {
    return this.nameElements.get(index);
  }
  
  String getLastNameElement() {
    return this.nameElements.get(this.nameElements.size() - 1);
  }


  @Override
  public Path getFileName() {
    String lastElement = nameElements.get(nameElements.size() - 1);
    List<String> elements = Collections.singletonList(lastElement);
    return AbsolutePath.createRealative(getMemoryFileSystem(), elements);
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


  @Override
  public boolean endsWith(String other) {
    Path path = this.getMemoryFileSystem().getPath(other);
    return this.endsWith(path);
  }


  @Override
  public Iterator<Path> iterator() {
    return new ElementIterator(getMemoryFileSystem(), this.nameElements.iterator());
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
    int firstDifferenceIndex = firstDifferenceIndex(this.getNameElements(), otherPath.getNameElements());
    List<String> first = Collections.emptyList();
    if (firstDifferenceIndex < this.getNameCount()) {
      first = HomogenousList.create("..", this.getNameCount() - firstDifferenceIndex);
    }
    List<String> second = Collections.emptyList();
    if (firstDifferenceIndex < other.getNameCount()) {
      second = otherPath.getNameElements().subList(firstDifferenceIndex, otherPath.getNameCount());
    }
    List<String> relativeElements = CompositeList.create(first, second);
    return AbsolutePath.createRealative(this.getMemoryFileSystem(), relativeElements);
  }

  abstract List<String> handleDotDotNormalizationNotYetModified(List<String> nameElements, int nameElementsSize, int i);

  abstract void handleDotDotNormalizationAlreadyModified(List<String> normalized);

  abstract List<String> handleSingleDotDot(List<String> normalized);

  List<String> normalizeElements() {
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
            // be break so no need to set modified
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
    return normalized;
  }

  static final class ElementIterator implements Iterator<Path> {
    
    private final MemoryFileSystem fileSystem;
    private final Iterator<String> nameIterator;
    
    ElementIterator(MemoryFileSystem fileSystem, Iterator<String> nameIterator) {
      this.fileSystem = fileSystem;
      this.nameIterator = nameIterator;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasNext() {
      return this.nameIterator.hasNext();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path next() {
      List<String> elements = Collections.singletonList(this.nameIterator.next());
      return AbsolutePath.createRealative(fileSystem, elements);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void remove() {
      throw new UnsupportedOperationException("can't remove from a path iterator");
    }
    
  } 

}
