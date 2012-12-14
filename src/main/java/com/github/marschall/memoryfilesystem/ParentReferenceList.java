package com.github.marschall.memoryfilesystem;

import java.util.AbstractList;
import java.util.Collections;
import java.util.List;
import java.util.RandomAccess;

/**
 * A {@link List} that only contains <code>".."</code> a given number
 * of times.
 */
final class ParentReferenceList extends AbstractList<String> implements RandomAccess {

  private final int size;

  private ParentReferenceList(int size) {
    this.size = size;
  }

  @Override
  public String get(int index) {
    if (index < 0) {
      throw new IndexOutOfBoundsException("index is not allowed to be negative but was " + index);
    }
    if (index >= this.size) {
      throw new IndexOutOfBoundsException("index is not allowed to be bigger than " + this.size + " but was " + index);
    }
    return "..";
  }

  @Override
  public int size() {
    return this.size;
  }

  @Override
  public boolean contains(Object o) {
    return "..".equals(o);
  }

  @Override
  public List<String> subList(int fromIndex, int toIndex) {
    if (fromIndex < 0) {
      throw new IllegalArgumentException("from index negative");
    }
    if (toIndex > this.size) {
      throw new IllegalArgumentException("to index too large");
    }
    if (fromIndex > toIndex) {
      throw new IllegalArgumentException("indices out of range");
    }

    if (fromIndex == 0 && toIndex == this.size) {
      return this;
    }

    return create(toIndex - fromIndex);
  }

  static List<String> create(int size) {
    if (size == 0) {
      return Collections.emptyList();
    } else if (size == 1) {
      return Collections.singletonList("..");
    } else {
      return new ParentReferenceList(size);
    }

  }

}
