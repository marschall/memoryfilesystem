package com.github.marschall.memoryfilesystem;

import java.io.IOException;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

final class MemoryFileSystemWatchService implements WatchService {

  private final ClosedWatchServiceChecker checker;

  private final BlockingQueue<MemoryWatchKey> readyKeys;

  private final List<MemoryWatchKey> keys;

  private final MemoryFileSystem memoryFileSystem;

  MemoryFileSystemWatchService(MemoryFileSystem memoryFileSystem) {
    this.memoryFileSystem = memoryFileSystem;
    this.checker = new ClosedWatchServiceChecker();
    this.readyKeys = new LinkedBlockingQueue<>();
    this.keys = new CopyOnWriteArrayList<>();
  }

  @Override
  public void close() throws IOException {
    if (this.checker.close()) {
      for (MemoryWatchKey key : this.keys) {
        key.cancel();
      }
    }
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

  void queue(MemoryWatchKey key) {
    this.readyKeys.add(key);
  }

  MemoryFileSystem getMemoryFileSystem() {
    return this.memoryFileSystem;
  }

}
