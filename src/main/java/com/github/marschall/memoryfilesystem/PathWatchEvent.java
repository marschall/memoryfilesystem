package com.github.marschall.memoryfilesystem;

import java.nio.file.Path;
import java.nio.file.WatchEvent;

abstract class PathWatchEvent implements WatchEvent<Path> {

  private final Path path;

  PathWatchEvent(Path path) {
    this.path = path;
  }

  @Override
  public Path context() {
    return this.path;
  }

}