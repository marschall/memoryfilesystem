package com.github.marschall.memoryfilesystem;

import java.util.AbstractList;
import java.util.Collections;
import java.util.List;
import java.util.RandomAccess;

/**
 * A list made of two other lists.
 *
 * <p>An example usage for this is path relativization. Consider you have
 * a list <code>["..", ".."]</code> and a list <code>["a", "b"]</code>
 * and you want to create a new list <code>["..", "..", "a", "b"]</code>
 * without having to allocate a full array.</p>
 *
 * <p>This list tries to automatically "defragment" itself. When a sublist
 * is created in an index range that falls into only one list then a
 * sublist of only that list and not the whole composite list is
 * returned.</p>
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
    int size = this.size();
    if (fromIndex == 0 && toIndex == size) {
      return this;
    }
    if (toIndex == fromIndex) {
      return Collections.emptyList();
    }
    if (toIndex - fromIndex == 1) {
      return Collections.singletonList(this.get(fromIndex));
    }

    if (toIndex <= firstSize) {
      if (fromIndex == 0 && toIndex == firstSize) {
        return this.first;
      } else {
        return this.first.subList(fromIndex, toIndex);
      }
    } else if (fromIndex >= firstSize) {
      if (fromIndex == firstSize && toIndex == size) {
        return this.second;
      } else {
        return this.second.subList(fromIndex - firstSize, toIndex - firstSize);
      }
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
