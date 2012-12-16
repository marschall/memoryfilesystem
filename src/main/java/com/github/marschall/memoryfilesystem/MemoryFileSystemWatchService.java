package com.github.marschall.memoryfilesystem;

import java.io.IOException;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

final class MemoryFileSystemWatchService implements WatchService {

  private final ClosedWatchServiceChecker checker;

  private final BlockingQueue<MemoryWatchKey> readyKeys;

  MemoryFileSystemWatchService() {
    this.checker = new ClosedWatchServiceChecker();
    this.readyKeys = new LinkedBlockingQueue<>();
  }

  @Override
  public void close() throws IOException {
    this.checker.close();
    // TODO invalidate keys
    // TODO throw new UnsupportedOperationException for all poll/take
    throw new UnsupportedOperationException();
  }

  @Override
  public WatchKey poll() {
    this.checker.check();
    return this.readyKeys.poll();
  }

  @Override
  public WatchKey poll(long timeout, TimeUnit unit) throws InterruptedException {
    this.checker.check();
    return this.readyKeys.poll(timeout, unit);
  }

  @Override
  public WatchKey take() throws InterruptedException {
    this.checker.check();
    return this.readyKeys.take();
  }

}
