package com.github.marschall.memoryfilesystem;

import java.util.AbstractList;
import java.util.List;

/**
 * A {@link List} that only contains a single element x-times.
 *
 * @param <E> the element type
 */
final class HomogenousList<E> extends AbstractList<E> {
  
  //TODO value is actually constant
  private final E element;
  
  private final int size;
  
  HomogenousList(E element, int size) {
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
    return element;
  }

  @Override
  public int size() {
    return this.size;
  }

}
