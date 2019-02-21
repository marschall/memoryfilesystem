package com.github.marschall.memoryfilesystem;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.channels.FileLock;
import java.nio.channels.NonWritableChannelException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

final class AsynchronousDirectoryFileChannel extends AsynchronousFileChannel {

  private final DirectoryChannel delegate;
  private final ExecutorService callbackExecutor;

  AsynchronousDirectoryFileChannel(DirectoryChannel delegate, ExecutorService callbackExecutor) {
    this.delegate = delegate;
    this.callbackExecutor = callbackExecutor;
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
    this.delegate.closedCheck();
    this.delegate.truncate(size);
    return this;
  }

  @Override
  public void force(boolean metaData) throws IOException {
    this.delegate.closedCheck();
    this.delegate.force(metaData);
  }

  @Override
  public <A> void lock(long position, long size, boolean shared, A attachment,
          CompletionHandler<FileLock, ? super A> handler) {
    if (!shared) {
      throw new NonWritableChannelException();
    }
    throw new IllegalArgumentException("directory file size is empty");

  }

  @Override
  public Future<FileLock> lock(long position, long size, boolean shared) {
    if (!shared) {
      throw new NonWritableChannelException();
    }
    throw new IllegalArgumentException("directory file size is empty");
  }

  @Override
  public FileLock tryLock(long position, long size, boolean shared)
          throws IOException {

    this.delegate.closedCheck();
    if (!shared) {
      throw new NonWritableChannelException();
    }
    throw new IllegalArgumentException("directory file size is empty");
  }

  @Override
  public <A> void read(ByteBuffer dst, long position, A attachment, CompletionHandler<Integer, ? super A> handler) {
    this.callbackExecutor.submit(() -> {
      handler.failed(new IOException("Is a directory"), attachment);
    });
  }

  @Override
  public Future<Integer> read(ByteBuffer dst, long position) {
    return new CanNotRead();
  }

  @Override
  public <A> void write(ByteBuffer src, long position, A attachment,
          CompletionHandler<Integer, ? super A> handler) {
    throw new NonWritableChannelException();
  }

  @Override
  public Future<Integer> write(ByteBuffer src, long position) {
    throw new NonWritableChannelException();
  }

  static final class CanNotRead implements Future<Integer> {

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
      return false;
    }

    @Override
    public boolean isCancelled() {
      return false;
    }

    @Override
    public boolean isDone() {
      return true;
    }

    private ExecutionException isDirectory() {
      return new ExecutionException(new IOException("Is a directory"));
    }

    @Override
    public Integer get() throws InterruptedException, ExecutionException {
      throw this.isDirectory();
    }

    @Override
    public Integer get(long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
      throw this.isDirectory();
    }

  }

}
