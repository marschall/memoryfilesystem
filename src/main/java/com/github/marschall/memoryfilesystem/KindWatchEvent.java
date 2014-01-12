package com.github.marschall.memoryfilesystem;

import java.nio.file.Path;

final class KindWatchEvent extends PathWatchEvent {

  private final Kind<Path> kind;


  KindWatchEvent(Path path, Kind<Path> kind) {
    super(path);
    this.kind = kind;
  }

  @Override
  public Kind<Path> kind() {
    return this.kind;
  }

  @Override
  public int count() {
    return 1;
  }

}
