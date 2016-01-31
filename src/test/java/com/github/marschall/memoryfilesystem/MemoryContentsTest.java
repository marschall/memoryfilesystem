package com.github.marschall.memoryfilesystem;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.NonReadableChannelException;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class MemoryContentsTest {

  private static final byte[] SAMPLE_DATA = new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9};

  private static final int INITIAL_BLOCKS = 4;

  private final boolean direct;

  private MemoryFile contents;

  public MemoryContentsTest(boolean direct) {
    this.direct = direct;
  }

  private ByteBuffer allocate(int capacity) {
    if (this.direct) {
      return ByteBuffer.allocateDirect(capacity);
    } else {
      return ByteBuffer.allocate(capacity);
    }
  }

  @Parameters(name = "direct allocated: {0}")
  public static List<Object[]> parameters() {
    return Arrays.asList(new Object[]{true}, new Object[]{false});
  }

  @Before
  public void setUp() {
    this.contents = new MemoryFile("", EntryCreationContext.empty(), INITIAL_BLOCKS);
  }


  private ByteBuffer writeTestData(SeekableByteChannel channel) throws IOException {
    ByteBuffer src = this.allocate(SAMPLE_DATA.length);
    byte[] data = SAMPLE_DATA;
    src.put(data);
    src.rewind();
    assertEquals(SAMPLE_DATA.length, channel.write(src));
    return src;
  }

  @Test
  public void firstBlockEmpty() throws IOException {
    ByteBuffer src = this.allocate(SAMPLE_DATA.length);
    byte[] data = SAMPLE_DATA;
    src.put(data);
    src.rewind();

    Path path = EasyMock.createNiceMock(Path.class);
    SeekableByteChannel channel = this.contents.newChannel(true, true, false, path);
    assertEquals(0, channel.size());
    assertEquals(0, channel.position());
    assertEquals(SAMPLE_DATA.length, channel.write(src));
    assertEquals(SAMPLE_DATA.length, channel.size());
    assertEquals(SAMPLE_DATA.length, channel.position());
    channel.position(0L);

    ByteBuffer dst = this.allocate(SAMPLE_DATA.length);
    assertEquals(SAMPLE_DATA.length, channel.read(dst));
    dst.rewind();
    byte[] extracted = new byte[SAMPLE_DATA.length];
    dst.get(extracted);
    assertArrayEquals(data, extracted);

    dst.rewind();
    assertEquals(-1, channel.read(dst));
  }


  @Test
  public void positition() throws IOException {
    Path path = EasyMock.createNiceMock(Path.class);
    SeekableByteChannel channel = this.contents.newChannel(true, true, false, path);

    assertEquals(0L, channel.position());

    // set the position bigger than the limit
    channel.position(5L);
    assertEquals(5L, channel.position());
    assertEquals(0, channel.size());

    // make sure we're at the end
    ByteBuffer dst = this.allocate(1);
    assertEquals(-1, channel.read(dst));

    this.writeTestData(channel);
    assertEquals(5L + SAMPLE_DATA.length, channel.position());
    assertEquals(5L + SAMPLE_DATA.length, channel.size());

    channel.position(channel.position() - SAMPLE_DATA.length);
    byte[] readBack = this.readBackSampleData(channel);
    assertArrayEquals(SAMPLE_DATA, readBack);
  }

  @Test
  public void readOnly() throws IOException {
    Path path = EasyMock.createNiceMock(Path.class);
    SeekableByteChannel channel = this.contents.newChannel(true, true, false, path);

    ByteBuffer src = this.writeTestData(channel);

    channel = this.contents.newChannel(true, false, false, path);
    try {
      channel.write(src);
      fail("channel should not be writable");
    } catch (NonWritableChannelException e) {
      // should reach here
      assertTrue(true);
    }
  }

  @Test
  public void writeOnly() throws IOException {
    Path path = EasyMock.createNiceMock(Path.class);
    SeekableByteChannel channel = this.contents.newChannel(false, true, false, path);

    ByteBuffer src = this.writeTestData(channel);
    src.rewind();

    channel.position(0L);
    try {
      channel.read(src);
      fail("channel should not be readable");
    } catch (NonReadableChannelException e) {
      // should reach here
      assertTrue(true);
    }
  }

  @Test
  public void truncate() throws IOException {
    Path path = EasyMock.createNiceMock(Path.class);
    SeekableByteChannel channel = this.contents.newChannel(true, true, false, path);
    ByteBuffer src = this.allocate(1);
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
    byte[] readBack = this.readBackSampleData(channel);
    assertArrayEquals(SAMPLE_DATA, readBack);

    // should be at the end
    ByteBuffer dst = this.allocate(1);
    assertEquals(-1, channel.read(dst));
  }

  @Test
  public void appendNonTruncatable() throws IOException {
    Path path = EasyMock.createNiceMock(Path.class);
    SeekableByteChannel channel = this.contents.newAppendingChannel(true, false, path);

    ByteBuffer src = this.writeTestData(channel);
    channel.write(src);
    try {
      channel.truncate(5L);
      fail("channel should not allow truncation");
    } catch (IOException e) {
      // should reach here
      assertTrue(true);
    }
  }

  @Test
  public void appendReadable() throws IOException {
    Path path = EasyMock.createNiceMock(Path.class);
    SeekableByteChannel channel = this.contents.newAppendingChannel(true, false, path);
    assertEquals(0L, channel.position());

    ByteBuffer src = this.allocate(1);
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
    byte[] readBack = this.readBackSampleData(channel);
    assertArrayEquals(SAMPLE_DATA, readBack);
  }

  private byte[] readBackSampleData(SeekableByteChannel channel) throws IOException {
    ByteBuffer dst = this.allocate(SAMPLE_DATA.length);
    assertEquals(SAMPLE_DATA.length, channel.read(dst));
    dst.rewind();
    byte[] readBack = new byte[SAMPLE_DATA.length];
    dst.get(readBack);
    return readBack;
  }

  @Test
  public void appendNotReadable() throws IOException {
    Path path = EasyMock.createNiceMock(Path.class);
    SeekableByteChannel channel = this.contents.newAppendingChannel(false, false, path);

    ByteBuffer testData = this.writeTestData(channel);
    channel.position(0L);

    testData.rewind();
    try {
      channel.read(testData);
      fail("channel should not be readable");
    } catch (NonReadableChannelException e) {
      // should reach here
      assertTrue(true);
    }
  }

}
