package com.github.marschall.memoryfilesystem;

import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;

final class ModificationWatchEvent extends PathWatchEvent {

  private final int count;


  ModificationWatchEvent(Path path, int count) {
    super(path);
    this.count = count;
  }

  @Override
  public Kind<Path> kind() {
    return StandardWatchEventKinds.ENTRY_MODIFY;
  }

  @Override
  public int count() {
    return this.count;
  }

}
