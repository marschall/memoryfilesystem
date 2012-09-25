package com.github.marschall.memoryfilesystem;

import java.util.AbstractList;
import java.util.List;
import java.util.RandomAccess;

/**
 * A list made of two other lists.
 *
 * @param <E> the element type
 */
final class CompositeList<E> extends AbstractList<E> implements RandomAccess {
  
  private final List<E> first;
  
  private final List<E> second;
  
  private CompositeList(List<E> first, List<E> second) {
    this.first = first;
    this.second = second;
  }

  @Override
  public E get(int index) {
    int firstSize = this.first.size();
    if (index < firstSize) {
      return this.first.get(index);
    } else {
      return this.second.get(index - firstSize);
    }
  }

  @Override
  public int size() {
    return this.first.size() + this.second.size();
  }
  
  @Override
  public List<E> subList(int fromIndex, int toIndex) {
    int firstSize = this.first.size();
    if (toIndex <= firstSize) {
      return this.first.subList(fromIndex, toIndex);
    } else if (fromIndex <= firstSize) {
      return this.second.subList(fromIndex - firstSize, toIndex - firstSize);
    } else {
      return super.subList(fromIndex, toIndex);
    }
  }
  
  static <E> List<E> create(List<E> first, List<E> second) {
    if (first.isEmpty()) {
      return second;
    } else if (second.isEmpty()) {
      return first;
    } else {
      return new CompositeList<>(first, second);
    }
  }

}
