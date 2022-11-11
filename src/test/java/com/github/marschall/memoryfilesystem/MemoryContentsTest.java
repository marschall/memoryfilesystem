package com.github.marschall.memoryfilesystem;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.nio.ByteBuffer;
import java.nio.channels.NonReadableChannelException;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class MemoryContentsTest {

  private static final byte[] SAMPLE_DATA = new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9};

  private static final int INITIAL_BLOCKS = 4;
  private static final String DISPLAY_NAME = "direct allocated: {0}";

  private ByteBuffer allocate(int capacity, boolean direct) {
    if (direct) {
      return ByteBuffer.allocateDirect(capacity);
    } else {
      return ByteBuffer.allocate(capacity);
    }
  }

  static List<Object[]> parameters() {
    return Arrays.asList(new Object[]{true}, new Object[]{false});
  }

  private MemoryFile createContents() {
    return new MemoryFile("", EntryCreationContextUtil.empty(), INITIAL_BLOCKS);
  }


  private ByteBuffer writeTestData(SeekableByteChannel channel, boolean direct) throws IOException {
    ByteBuffer src = this.allocate(SAMPLE_DATA.length, direct);
    byte[] data = SAMPLE_DATA;
    src.put(data);
    src.rewind();
    assertEquals(SAMPLE_DATA.length, channel.write(src));
    return src;
  }

  @Target(METHOD)
  @Retention(RUNTIME)
  @ParameterizedTest(name = DISPLAY_NAME)
  @MethodSource("parameters")
  @interface BufferTest {

  }

  @BufferTest
  void firstBlockEmpty(boolean direct) throws IOException {
    ByteBuffer src = this.allocate(SAMPLE_DATA.length, direct);
    byte[] data = SAMPLE_DATA;
    src.put(data);
    src.rewind();

    Path path = new MockPath();
    SeekableByteChannel channel = this.createContents().newChannel(true, true, false, path);
    assertEquals(0, channel.size());
    assertEquals(0, channel.position());
    assertEquals(SAMPLE_DATA.length, channel.write(src));
    assertEquals(SAMPLE_DATA.length, channel.size());
    assertEquals(SAMPLE_DATA.length, channel.position());
    channel.position(0L);

    ByteBuffer dst = this.allocate(SAMPLE_DATA.length, direct);
    assertEquals(SAMPLE_DATA.length, channel.read(dst));
    dst.rewind();
    byte[] extracted = new byte[SAMPLE_DATA.length];
    dst.get(extracted);
    assertArrayEquals(data, extracted);

    dst.rewind();
    assertEquals(-1, channel.read(dst));
  }


  @BufferTest
  void positition(boolean direct) throws IOException {
    Path path = new MockPath();
    SeekableByteChannel channel = this.createContents().newChannel(true, true, false, path);

    assertEquals(0L, channel.position());

    // set the position bigger than the limit
    channel.position(5L);
    assertEquals(5L, channel.position());
    assertEquals(0, channel.size());

    // make sure we're at the end
    ByteBuffer dst = this.allocate(1, direct);
    assertEquals(-1, channel.read(dst));

    this.writeTestData(channel, direct);
    assertEquals(5L + SAMPLE_DATA.length, channel.position());
    assertEquals(5L + SAMPLE_DATA.length, channel.size());

    channel.position(channel.position() - SAMPLE_DATA.length);
    byte[] readBack = this.readBackSampleData(channel, direct);
    assertArrayEquals(SAMPLE_DATA, readBack);
  }

  @BufferTest
  void readOnly(boolean direct) throws IOException {
    Path path = new MockPath();
    SeekableByteChannel channel = this.createContents().newChannel(true, true, false, path);

    ByteBuffer src = this.writeTestData(channel, direct);

    BlockChannel newChannel = this.createContents().newChannel(true, false, false, path);
    assertThrows(NonWritableChannelException.class, () -> newChannel.write(src), "channel should not be writable");
  }

  @BufferTest
  void writeOnly(boolean direct) throws IOException {
    Path path = new MockPath();
    SeekableByteChannel channel = this.createContents().newChannel(false, true, false, path);

    ByteBuffer src = this.writeTestData(channel, direct);
    src.rewind();

    channel.position(0L);
    assertThrows(NonReadableChannelException.class, () -> channel.read(src), "channel should not be readable");
  }

  @BufferTest
  void truncate(boolean direct) throws IOException {
    Path path = new MockPath();
    SeekableByteChannel channel = this.createContents().newChannel(true, true, false, path);
    ByteBuffer src = this.allocate(1, direct);
    for (byte data : SAMPLE_DATA) {
      src.rewind();
      src.put(data);
      src.rewind();
      channel.write(src);
    }

    src.rewind();
    src.put((byte) 1);
    for (int i = 0; i < MemoryInode.BLOCK_SIZE; i++) {
      src.rewind();
      channel.write(src);
    }

    long expectedSize = (long) MemoryInode.BLOCK_SIZE + SAMPLE_DATA.length;
    assertEquals(expectedSize, channel.size());

    // truncating a bigger value should make no difference
    assertSame(channel, channel.truncate(Long.MAX_VALUE));
    assertEquals(expectedSize, channel.size());

    assertSame(channel, channel.truncate(expectedSize + 1L));
    assertEquals(expectedSize, channel.size());

    // now really truncate
    assertSame(channel, channel.truncate(SAMPLE_DATA.length));
    assertEquals(SAMPLE_DATA.length, channel.size());
    assertEquals(SAMPLE_DATA.length, channel.position());

    channel.position(0);
    byte[] readBack = this.readBackSampleData(channel, direct);
    assertArrayEquals(SAMPLE_DATA, readBack);

    // should be at the end
    ByteBuffer dst = this.allocate(1, direct);
    assertEquals(-1, channel.read(dst));
  }

  @BufferTest
  void appendNonTruncatable(boolean direct) throws IOException {
    Path path = new MockPath();
    SeekableByteChannel channel = this.createContents().newAppendingChannel(true, false, path);

    ByteBuffer src = this.writeTestData(channel, direct);
    channel.write(src);
    assertThrows(IOException.class, () -> channel.truncate(5L), "channel should not allow truncation");
  }

  @BufferTest
  void appendReadable(boolean direct) throws IOException {
    Path path = new MockPath();
    SeekableByteChannel channel = this.createContents().newAppendingChannel(true, false, path);
    assertEquals(0L, channel.position());

    ByteBuffer src = this.allocate(1, direct);
    for (byte data : SAMPLE_DATA) {
      src.rewind();
      channel.position(0L);
      src.put(data);
      src.rewind();
      channel.write(src);

      assertEquals(data + 1, channel.size());
      assertEquals(data + 1, channel.position());
    }

    channel.position(0);
    byte[] readBack = this.readBackSampleData(channel, direct);
    assertArrayEquals(SAMPLE_DATA, readBack);
  }

  private byte[] readBackSampleData(SeekableByteChannel channel, boolean direct) throws IOException {
    ByteBuffer dst = this.allocate(SAMPLE_DATA.length, direct);
    assertEquals(SAMPLE_DATA.length, channel.read(dst));
    dst.rewind();
    byte[] readBack = new byte[SAMPLE_DATA.length];
    dst.get(readBack);
    return readBack;
  }

  @BufferTest
  void appendNotReadable(boolean direct) throws IOException {
    Path path = new MockPath();
    SeekableByteChannel channel = this.createContents().newAppendingChannel(false, false, path);

    ByteBuffer testData = this.writeTestData(channel, direct);
    channel.position(0L);

    testData.rewind();
    assertThrows(NonReadableChannelException.class, () -> channel.read(testData), "channel should not be readable");
  }

}
