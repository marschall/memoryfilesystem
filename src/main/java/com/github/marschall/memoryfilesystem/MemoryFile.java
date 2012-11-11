package com.github.marschall.memoryfilesystem;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttributeView;
import java.util.Collections;
import java.util.Set;

class MemoryFile extends MemoryEntry {

  private final MemoryContents contents;

  private final BasicFileAttributes attributes;

  private final BasicFileAttributeView basicFileAttributeView;

  MemoryFile(String originalName) {
    this(originalName, Collections.<Class<? extends FileAttributeView>>emptySet());
  }

  MemoryFile(String originalName, Set<Class<? extends FileAttributeView>> additionalViews) {
    super(originalName, additionalViews);
    this.attributes = new MemoryFileAttributes();
    this.basicFileAttributeView = new MemoryFileAttributesView();
    this.contents = new MemoryContents();
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

  InputStream newInputStream(Set<? extends OpenOption> options) {
    // TODO check more options
    // TODO DELETE_ON_CLOSE and NOFOLLOW_LINKS
    // TODO SYNC DSYNC
    return this.contents.newInputStream();
  }

  SeekableByteChannel newChannel(Set<? extends OpenOption> options) {
    // TODO check more options
    // TODO DELETE_ON_CLOSE and NOFOLLOW_LINKS
    // TODO SYNC DSYNC
    boolean append = options.contains(StandardOpenOption.APPEND);
    boolean readable = options.contains(StandardOpenOption.READ);
    if (append) {
      return this.contents.newAppendingChannel(readable);
    } else {
      boolean writable = options.contains(StandardOpenOption.WRITE);
      if (writable) {
        boolean truncate = options.contains(StandardOpenOption.TRUNCATE_EXISTING);
        if (truncate) {
          this.contents.truncate(0L);
        }
      }
      return this.contents.newChannel(readable, writable);
    }
  }


}
