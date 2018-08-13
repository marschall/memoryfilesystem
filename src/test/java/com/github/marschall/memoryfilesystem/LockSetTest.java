package com.github.marschall.memoryfilesystem;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class LockSetTest {

  private final MemoryFileLock toAdd;
  private final Collection<MemoryFileLock> alreadyPresent;
  private LockSet lockSet;
  private final boolean expectedSuccess;

  public LockSetTest(MemoryFileLock toAdd, Collection<MemoryFileLock> alreadyPresent, boolean expectedSuccess) {
    this.toAdd = toAdd;
    this.alreadyPresent = alreadyPresent;
    this.expectedSuccess = expectedSuccess;
  }


  @Parameters
  public static List<Object[]> data() {
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

  @Before
  public void setUp() throws IOException {
    this.lockSet = new LockSet();
    for (MemoryFileLock lock : this.alreadyPresent) {
      this.lockSet.lock(lock);
    }
  }

  @Test
  public void tryLock() {
    FileLock lock = this.lockSet.tryLock(this.toAdd);
    assertEquals("lock acquisition successful", this.expectedSuccess, lock != null);
    if (this.expectedSuccess) {
      assertSame(this.toAdd, lock);
    }
  }

  @Test
  public void lock() throws IOException {
    FileLock lock = null;
    try {
      lock = this.lockSet.lock(this.toAdd);
      if (!this.expectedSuccess) {
        fail("lock acquisition successful");
      }
    } catch (OverlappingFileLockException e) {
      if (this.expectedSuccess) {
        fail("lock acquisition failed");
      }
    }
    if (this.expectedSuccess) {
      assertSame(this.toAdd, lock);
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
