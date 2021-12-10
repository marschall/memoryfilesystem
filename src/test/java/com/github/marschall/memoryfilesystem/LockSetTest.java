package com.github.marschall.memoryfilesystem;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class LockSetTest {

  static List<Object[]> data() {
    return Arrays.asList(new Object[][] {
      { lock(0L, 1000L), singletonList(lock(1000L, 9000L)), true },
      { lock(10001L, 999L), singletonList(lock(1000L, 9001L)), true },
      { lock(1L, 1000L), singletonList(lock(1000L, 9000L)), false },
      { lock(10000L, 999L), singletonList(lock(1000L, 9001L)), false },
      { lock(2000L, 1000L), singletonList(lock(1000L, 9001L)), false },
      { lock(0L, 20000L), singletonList(lock(1000L, 9001L)), false },
      { lock(0L, 1000L), asList(lock(1000L, 1000L), lock(10000L, 1000L)), true },
      { lock(5000L, 1000L), asList(lock(1000L, 1000L), lock(10000L, 1000L)), true },
      { lock(20000L, 1000L), asList(lock(1000L, 1000L), lock(10000L, 1000L)), true },
      { lock(1500L, 1000L), asList(lock(1000L, 1000L), lock(10000L, 1000L)), false },
      { lock(1500L, 100L), asList(lock(1000L, 1000L), lock(10000L, 1000L)), false },
      { lock(10500L, 1000L), asList(lock(1000L, 1000L), lock(10000L, 1000L)), false },
      { lock(1500L, 9000L), asList(lock(1000L, 1000L), lock(10000L, 1000L)), false },
      { lock(0L, 20000L), asList(lock(1000L, 1000L), lock(10000L, 1000L)), false },
    });
  }

  private static MemoryFileLock lock(long position, long size) {
    AsynchronousFileChannel channel = new StubChannel();
    return new MemoryFileLock(channel, position, size, false);
  }

  private LockSet createLockSet(Collection<MemoryFileLock> alreadyPresent) throws IOException {
    LockSet lockSet = new LockSet();
    for (MemoryFileLock lock : alreadyPresent) {
      lockSet.lock(lock);
    }
    return lockSet;
  }

  @ParameterizedTest(autoCloseArguments = false) // don't call MemoryFileLock#close()
  @MethodSource("data")
  void tryLock(MemoryFileLock toAdd, Collection<MemoryFileLock> alreadyPresent, boolean expectedSuccess) throws IOException {
    FileLock lock = this.createLockSet(alreadyPresent).tryLock(toAdd);
    assertEquals(expectedSuccess, lock != null, "lock acquisition successful");
    if (expectedSuccess) {
      assertSame(toAdd, lock);
    }
  }

  @ParameterizedTest(autoCloseArguments = false) // don't call MemoryFileLock#close()
  @MethodSource("data")
  void lock(MemoryFileLock toAdd, Collection<MemoryFileLock> alreadyPresent, boolean expectedSuccess) throws IOException {
    FileLock lock = null;
    try {
      lock = this.createLockSet(alreadyPresent).lock(toAdd);
      if (!expectedSuccess) {
        fail("lock acquisition successful");
      }
    } catch (OverlappingFileLockException e) {
      if (expectedSuccess) {
        fail("lock acquisition failed");
      }
    }
    if (expectedSuccess) {
      assertSame(toAdd, lock);
    } else {
      assertNull(lock);
    }
  }

  static final class StubChannel extends AsynchronousFileChannel {

    @Override
    public void close() {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean isOpen() {
      throw new UnsupportedOperationException();
    }

    @Override
    public long size() {
      throw new UnsupportedOperationException();
    }

    @Override
    public AsynchronousFileChannel truncate(long size) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void force(boolean metaData) {
      throw new UnsupportedOperationException();
    }

    @Override
    public <A> void lock(long position, long size, boolean shared, A attachment, CompletionHandler<FileLock, ? super A> handler) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Future<FileLock> lock(long position, long size, boolean shared) {
      throw new UnsupportedOperationException();
    }

    @Override
    public FileLock tryLock(long position, long size, boolean shared) {
      throw new UnsupportedOperationException();
    }

    @Override
    public <A> void read(ByteBuffer dst, long position, A attachment, CompletionHandler<Integer, ? super A> handler) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Future<Integer> read(ByteBuffer dst, long position) {
      throw new UnsupportedOperationException();
    }

    @Override
    public <A> void write(ByteBuffer src, long position, A attachment, CompletionHandler<Integer, ? super A> handler) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Future<Integer> write(ByteBuffer src, long position) {
      throw new UnsupportedOperationException();
    }

  }

}
