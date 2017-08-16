package com.github.marschall.memoryfilesystem;

import java.io.IOException;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

final class MemoryDirectoryStream implements DirectoryStream<Path>, Iterator<Path> {

  //TODO ClosedDirectoryStreamException

  static final AtomicIntegerFieldUpdater<MemoryDirectoryStream> ITERATOR_CALLED_UPDATER =
          AtomicIntegerFieldUpdater.newUpdater(MemoryDirectoryStream.class, "iteratorCalled");

  private static final int CALLED = 1;
  private static final int NOT_CALLED = 0;

  @SuppressWarnings("unused") // ITERATOR_CALLED_UPDATER
  private volatile int iteratorCalled;

  private final Path basePath;
  private final Iterator<String> iterator;
  private Path next;
  private final Filter<? super Path> filter;

  MemoryDirectoryStream(Path basePath, Filter<? super Path> filter, List<String> elements) {
    this.basePath = basePath;
    this.filter = filter;
    this.iterator = elements.iterator();
    ITERATOR_CALLED_UPDATER.set(this, NOT_CALLED);
  }

  @Override
  public void close() throws IOException {
    this.next = null;
  }

  @Override
  public Iterator<Path> iterator() {
    boolean success = ITERATOR_CALLED_UPDATER.compareAndSet(this, NOT_CALLED, CALLED);
    if (!success) {
      throw new IllegalStateException("#iterator() already called");
    }
    this.setNext();
    return this;
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

  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }

}
