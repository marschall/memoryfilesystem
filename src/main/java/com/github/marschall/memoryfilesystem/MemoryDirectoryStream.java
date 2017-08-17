package com.github.marschall.memoryfilesystem;

import java.io.IOException;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

final class MemoryDirectoryStream implements DirectoryStream<Path> {

  //TODO ClosedDirectoryStreamException

  static final AtomicIntegerFieldUpdater<MemoryDirectoryStream> ITERATOR_CALLED_UPDATER =
          AtomicIntegerFieldUpdater.newUpdater(MemoryDirectoryStream.class, "iteratorCalled");

  private static final int CALLED = 1;
  private static final int NOT_CALLED = 0;

  @SuppressWarnings("unused") // ITERATOR_CALLED_UPDATER
  private volatile int iteratorCalled;

  private final MemoryDirectoryIterator iterator;

  MemoryDirectoryStream(Path basePath, Filter<? super Path> filter, List<String> elements) {
    Objects.requireNonNull(basePath, "basePath");
    Objects.requireNonNull(filter, "filter");
    Objects.requireNonNull(elements, "elements");
    this.iterator = new MemoryDirectoryIterator(basePath, filter, elements);
    ITERATOR_CALLED_UPDATER.set(this, NOT_CALLED);
  }

  @Override
  public void close() throws IOException {
    this.iterator.close();
  }

  @Override
  public Iterator<Path> iterator() {
    boolean success = ITERATOR_CALLED_UPDATER.compareAndSet(this, NOT_CALLED, CALLED);
    if (!success) {
      throw new IllegalStateException("#iterator() already called");
    }
    this.iterator.setNext();
    return this.iterator;
  }

  final class MemoryDirectoryIterator implements Iterator<Path> {

    private final Path basePath;
    private final Iterator<String> iterator;
    private Path next;
    private final Filter<? super Path> filter;

    MemoryDirectoryIterator(Path basePath, Filter<? super Path> filter, List<String> elements) {
      Objects.requireNonNull(basePath, "basePath");
      Objects.requireNonNull(filter, "filter");
      Objects.requireNonNull(elements, "elements");
      this.basePath = basePath;
      this.filter = filter;
      this.iterator = elements.iterator();
    }

    private void setNext() {
      while (this.iterator.hasNext()) {
        Path path = this.basePath.resolve(this.iterator.next());
        try {
          if (this.filter.accept(path)) {
            this.next = path;
            break;
          }
        } catch (IOException e) {
          throw new DirectoryIteratorException(e);
        }
      }
    }

    @Override
    public boolean hasNext() {
      return this.next != null;
    }

    @Override
    public Path next() {
      if (this.next == null) {
        throw new NoSuchElementException();
      }
      Path result = this.next;
      this.next = null;
      this.setNext();
      return result;
    }

    // has to run on Java 7
    //    @Override
    //    public void forEachRemaining(Consumer<? super Path> action) {
    //      if (this.next == null) {
    //        return;
    //      }
    //      try {
    //        try {
    //          if (this.filter.accept(this.next)) {
    //            action.accept(this.next);
    //          }
    //        } catch (IOException e) {
    //          throw new DirectoryIteratorException(e);
    //        }
    //        while (this.iterator.hasNext()) {
    //          Path path = this.basePath.resolve(this.iterator.next());
    //          try {
    //            if (this.filter.accept(path)) {
    //              action.accept(path);
    //            }
    //          } catch (IOException e) {
    //            throw new DirectoryIteratorException(e);
    //          }
    //        }
    //      } finally {
    //        this.next = null;
    //      }
    //    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }

    void close() {
      this.next = null;
    }

  }


}
