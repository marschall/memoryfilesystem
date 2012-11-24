package com.github.marschall.memoryfilesystem;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

final class AsynchronousMemoryFileChannel extends AsynchronousFileChannel {

  // TODO what about exceptions in methods that return futures?

  private final FileChannel delegate;
  private final ExecutorService workExecutor;
  private final ExecutorService callbackExecutor;

  AsynchronousMemoryFileChannel(FileChannel delegate, ExecutorService workExecutor, ExecutorService callbackExecutor) {
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
          FileLock lock = AsynchronousMemoryFileChannel.this.delegate.lock(position, size, shared);
          AsynchronousMemoryFileChannel.this.completed(lock, attachment, handler);
        } catch (IOException e) {
          AsynchronousMemoryFileChannel.this.failed(e, attachment, handler);
        }

      }
    });

  }

  @Override
  public Future<FileLock> lock(final long position, final long size, final boolean shared) {
    return this.workExecutor.submit(new Callable<FileLock>() {

      @Override
      public FileLock call() throws Exception {
        return AsynchronousMemoryFileChannel.this.delegate.lock(position, size, shared);
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
          final int read = AsynchronousMemoryFileChannel.this.delegate.read(dst, position);
          AsynchronousMemoryFileChannel.this.completed(read, attachment, handler);
        } catch (IOException e) {
          AsynchronousMemoryFileChannel.this.failed(e, attachment, handler);
        }

      }
    });
  }

  @Override
  public Future<Integer> read(final ByteBuffer dst, final long position) {
    return this.workExecutor.submit(new Callable<Integer>() {

      @Override
      public Integer call() throws Exception {
        return AsynchronousMemoryFileChannel.this.delegate.read(dst, position);
      }
    });
  }

  @Override
  public <A> void write(final ByteBuffer src, final long position, final A attachment, final CompletionHandler<Integer, ? super A> handler) {
    this.workExecutor.submit(new Runnable() {

      @Override
      public void run() {
        try {
          final int written = AsynchronousMemoryFileChannel.this.delegate.write(src, position);
          AsynchronousMemoryFileChannel.this.completed(written, attachment, handler);
        } catch (IOException e) {
          AsynchronousMemoryFileChannel.this.failed(e, attachment, handler);
        }

      }
    });
  }

  @Override
  public Future<Integer> write(final ByteBuffer src, final long position) {
    return this.workExecutor.submit(new Callable<Integer>() {

      @Override
      public Integer call() throws Exception {
        return AsynchronousMemoryFileChannel.this.delegate.write(src, position);
      }
    });
  }

}
