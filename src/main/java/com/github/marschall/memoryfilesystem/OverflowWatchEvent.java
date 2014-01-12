package com.github.marschall.memoryfilesystem;

import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;

final class OverflowWatchEvent implements WatchEvent<Object> {

  static final WatchEvent<?> INSTANCE = new OverflowWatchEvent();

  @Override
  public Kind<Object> kind() {
    return StandardWatchEventKinds.OVERFLOW;
  }

  @Override
  public int count() {
    return 1;
  }

  @Override
  public Object context() {
    return null;
  }

}
