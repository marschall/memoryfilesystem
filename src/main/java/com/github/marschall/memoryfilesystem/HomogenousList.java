package com.github.marschall.memoryfilesystem;

import java.util.AbstractList;
import java.util.Collections;
import java.util.List;
import java.util.RandomAccess;

/**
 * A {@link List} that only contains a single element x-times.
 *
 * @param <E> the element type
 */
final class HomogenousList<E> extends AbstractList<E> implements RandomAccess {
  
  //TODO value is actually constant
  private final E element;
  
  private final int size;
  
  private HomogenousList(E element, int size) {
    this.element = element;
    this.size = size;
  }

  @Override
  public E get(int index) {
    if (index < 0) {
      throw new IndexOutOfBoundsException("index is not allowed to be negative but was " + index);
    }
    if (index >= this.size) {
      throw new IndexOutOfBoundsException("index is not allowed to be bigger than " + this.size + " but was " + index);
    }
    return this.element;
  }

  @Override
  public int size() {
    return this.size;
  }
  
  @Override
  public List<E> subList(int fromIndex, int toIndex) {
    if (fromIndex < 0) {
      throw new IllegalArgumentException("from index negative");
    }
    if (toIndex > this.size) {
      throw new IllegalArgumentException("to index too large");
    }
    if (fromIndex > toIndex) {
      throw new IllegalArgumentException("indices out of range");
    }
    
    if (fromIndex == 0 && toIndex == size) {
      return this;
    }
    
    return create(this.element, toIndex - fromIndex);
  }
  
  static <E> List<E> create(E element, int size) {
    if (size == 0) {
      return Collections.emptyList();
    } else if (size == 1) {
      return Collections.singletonList(element);
    } else {
      return new HomogenousList<E>(element, size);
    }
    
  }

}
