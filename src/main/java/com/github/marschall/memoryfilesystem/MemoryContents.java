package com.github.marschall.memoryfilesystem;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

interface MemoryContents {


  long size();

  long read(ByteBuffer dst, long position, long maximum) throws IOException;

  int readShort(ByteBuffer dst, long position) throws IOException;

  int read(byte[] dst, long position, int off, int len) throws IOException;

  long transferFrom(ReadableByteChannel src, long position, long count) throws IOException;

  long transferTo(WritableByteChannel target, long position, long count) throws IOException;

  long write(ByteBuffer src, long position, long maximum);

  int writeShort(ByteBuffer src, long position);

  int write(byte[] src, long position, int off, int len);

  long writeAtEnd(ByteBuffer src, long maximum);

  int writeAtEnd(ByteBuffer src);

  int writeAtEnd(byte[] src, int off, int len);

  void truncate(long newSize);

  void unlock(MemoryFileLock lock);

  MemoryFileLock lock(MemoryFileLock lock) throws IOException;

  MemoryFileLock tryLock(MemoryFileLock lock) throws IOException;

  void accessed();

  void modified();

  void closedStream();

  void closedChannel();

}
