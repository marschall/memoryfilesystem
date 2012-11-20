package com.github.marschall.memoryfilesystem;

import java.io.IOException;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicBoolean;

final class MemoryDirectoryStream implements DirectoryStream<Path>, Iterator<Path> {

  private final Path basePath;
  private final Iterator<String> iterator;
  private final AtomicBoolean iteratorCalled = new AtomicBoolean(false);
  private Path next;
  private final Filter<? super Path> filter;

  MemoryDirectoryStream(Path basePath, Filter<? super Path> filter, List<String> elements) {
    this.basePath = basePath;
    this.filter = filter;
    this.iterator = elements.iterator();
  }

  @Override
  public void close() throws IOException {
    this.next = null;
  }

  @Override
  public Iterator<Path> iterator() {
    boolean success = this.iteratorCalled.compareAndSet(false, true);
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
