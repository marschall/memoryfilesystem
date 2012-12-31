package com.github.marschall.memoryfilesystem;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.channels.FileLock;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

final class AsynchronousBlockChannel extends AsynchronousFileChannel {

  private final BlockChannel delegate;
  private final ExecutorService workExecutor;
  private final ExecutorService callbackExecutor;

  AsynchronousBlockChannel(BlockChannel delegate, ExecutorService workExecutor, ExecutorService callbackExecutor) {
    this.delegate = delegate;
    this.workExecutor = workExecutor;
    this.callbackExecutor = callbackExecutor;
  }

  private <A> void failed(final Throwable exception, final A attachment, final CompletionHandler<?, ? super A> handler) {
    this.callbackExecutor.submit(new Runnable() {

      @Override
      public void run() {
        handler.failed(exception, attachment);

      }
    });
  }

  private <V, A> void completed(final V result, final A attachment, final CompletionHandler<V, ? super A> handler) {
    this.callbackExecutor.submit(new Runnable() {

      @Override
      public void run() {
        handler.completed(result, attachment);
      }
    });
  }

  @Override
  public void close() throws IOException {
    this.delegate.close();
  }

  @Override
  public boolean isOpen() {
    return this.delegate.isOpen();
  }

  @Override
  public long size() throws IOException {
    return this.delegate.size();
  }

  @Override
  public AsynchronousFileChannel truncate(long size) throws IOException {
    this.delegate.truncate(size);
    return this;
  }

  @Override
  public void force(boolean metaData) throws IOException {
    this.delegate.force(metaData);
  }


  @Override
  public <A> void lock(final long position, final long size, final boolean shared, final A attachment, final CompletionHandler<FileLock, ? super A> handler) {
    this.workExecutor.submit(new Runnable() {

      @Override
      public void run() {
        try {
          MemoryFileLock l = new MemoryFileLock(AsynchronousBlockChannel.this, position, size, shared);
          FileLock lock = AsynchronousBlockChannel.this.delegate.lock(l);
          AsynchronousBlockChannel.this.completed(lock, attachment, handler);
        } catch (IOException | RuntimeException e) {
          AsynchronousBlockChannel.this.failed(e, attachment, handler);
        }

      }
    });

  }

  @Override
  public Future<FileLock> lock(final long position, final long size, final boolean shared) {
    return this.workExecutor.submit(new Callable<FileLock>() {

      @Override
      public FileLock call() throws Exception {
        MemoryFileLock l = new MemoryFileLock(AsynchronousBlockChannel.this, position, size, shared);
        return AsynchronousBlockChannel.this.delegate.lock(l);
      }
    });
  }

  @Override
  public FileLock tryLock(long position, long size, boolean shared) throws IOException {
    return this.delegate.tryLock(position, size, shared);
  }

  @Override
  public <A> void read(final ByteBuffer dst, final long position, final A attachment, final CompletionHandler<Integer, ? super A> handler) {
    this.workExecutor.submit(new Runnable() {

      @Override
      public void run() {
        try {
          final int read = AsynchronousBlockChannel.this.delegate.read(dst, position);
          AsynchronousBlockChannel.this.completed(read, attachment, handler);
        } catch (IOException | RuntimeException e) {
          AsynchronousBlockChannel.this.failed(e, attachment, handler);
        }

      }
    });
  }

  @Override
  public Future<Integer> read(final ByteBuffer dst, final long position) {
    return this.workExecutor.submit(new Callable<Integer>() {

      @Override
      public Integer call() throws Exception {
        return AsynchronousBlockChannel.this.delegate.read(dst, position);
      }
    });
  }

  @Override
  public <A> void write(final ByteBuffer src, final long position, final A attachment, final CompletionHandler<Integer, ? super A> handler) {
    this.workExecutor.submit(new Runnable() {

      @Override
      public void run() {
        try {
          final int written = AsynchronousBlockChannel.this.delegate.write(src, position);
          AsynchronousBlockChannel.this.completed(written, attachment, handler);
        } catch (IOException | RuntimeException e) {
          AsynchronousBlockChannel.this.failed(e, attachment, handler);
        }

      }
    });
  }

  @Override
  public Future<Integer> write(final ByteBuffer src, final long position) {
    return this.workExecutor.submit(new Callable<Integer>() {

      @Override
      public Integer call() throws Exception {
        return AsynchronousBlockChannel.this.delegate.write(src, position);
      }
    });
  }

}
