package com.github.marschall.memoryfilesystem;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

public class BlockFileChannel extends FileChannel {

  @Override
  public int read(ByteBuffer dst) throws IOException {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public int write(ByteBuffer src) throws IOException {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public long write(ByteBuffer[] srcs, int offset, int length)
          throws IOException {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public long position() throws IOException {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public FileChannel position(long newPosition) throws IOException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public long size() throws IOException {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public FileChannel truncate(long size) throws IOException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void force(boolean metaData) throws IOException {
    // TODO Auto-generated method stub

  }

  @Override
  public long transferTo(long position, long count, WritableByteChannel target) throws IOException {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public long transferFrom(ReadableByteChannel src, long position, long count) throws IOException {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public int read(ByteBuffer dst, long position) throws IOException {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public int write(ByteBuffer src, long position) throws IOException {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public MappedByteBuffer map(MapMode mode, long position, long size) throws IOException {
    throw new UnsupportedOperationException("memory file system does not support mmapped IO");
  }

  @Override
  public FileLock lock(long position, long size, boolean shared) throws IOException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public FileLock tryLock(long position, long size, boolean shared) throws IOException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  protected void implCloseChannel() throws IOException {
    // TODO Auto-generated method stub

  }

}
