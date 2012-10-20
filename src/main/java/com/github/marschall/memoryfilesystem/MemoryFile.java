package com.github.marschall.memoryfilesystem;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Set;

class MemoryFile extends MemoryEntry {

  private final MemoryContents contents;

  private final BasicFileAttributes attributes;

  private final BasicFileAttributeView basicFileAttributeView;

  MemoryFile(String originalName) {
    super(originalName);
    this.attributes = new MemoryFileAttributes();
    this.basicFileAttributeView = new MemoryFileAttributesView();
    this.contents = new MemoryContents(16);
  }

  @Override
  BasicFileAttributeView getBasicFileAttributeView() {
    return this.basicFileAttributeView;
  }

  @Override
  BasicFileAttributes getBasicFileAttributes() {
    return this.attributes;
  }


  class MemoryFileAttributesView extends MemoryEntryFileAttributesView {

    @Override
    public BasicFileAttributes readAttributes() throws IOException {
      return MemoryFile.this.attributes;
    }

  }

  final class MemoryFileAttributes extends MemoryEntryFileAttributes {

    @Override
    public boolean isRegularFile() {
      return true;
    }

    @Override
    public boolean isDirectory() {
      return false;
    }

    @Override
    public boolean isSymbolicLink() {
      return false;
    }

    @Override
    public boolean isOther() {
      return false;
    }

    @Override
    public long size() {
      return MemoryFile.this.contents.size();
    }

    @Override
    public Object fileKey() {
      // REVIEW think about it
      return MemoryFile.this;
    }

  }

  SeekableByteChannel newChannel(Set<? extends OpenOption> options) {
    // TODO check more options
    boolean append = options.contains(StandardOpenOption.APPEND);
    boolean readable = options.contains(StandardOpenOption.READ);
    if (append) {
      return this.contents.newAppendingChannel(readable);
    } else {
      boolean writable = options.contains(StandardOpenOption.WRITE);
      return this.contents.newChannel(readable, writable);
    }
  }


}
