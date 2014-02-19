package com.github.marschall.memoryfilesystem;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

import java.nio.file.OpenOption;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

final class DefaultOpenOptions extends AbstractSet<OpenOption> {

  static final Set<OpenOption> INSTANCE = new DefaultOpenOptions();

  private DefaultOpenOptions() {
    super();
  }

  @Override
  public Iterator<OpenOption> iterator() {
    return new DefaultOpenOptionsIterator();
  }

  @Override
  public boolean isEmpty() {
    return false;
  }

  @Override
  public boolean contains(Object o) {
    return o == CREATE || o == TRUNCATE_EXISTING || o == WRITE;
  }

  @Override
  public boolean remove(Object o) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int size() {
    return 3;
  }

  static final class DefaultOpenOptionsIterator implements Iterator<OpenOption> {

    private int index;

    DefaultOpenOptionsIterator() {
      this.index = 0;
    }

    @Override
    public boolean hasNext() {
      return this.index < 3;
    }

    @Override
    public OpenOption next() {
      this.index += 1;
      switch (this.index) {
        case 1:
          return CREATE;
        case 2:
          return TRUNCATE_EXISTING;
        case 3:
          return WRITE;
        default:
          throw new NoSuchElementException();
      }
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }

  }

}