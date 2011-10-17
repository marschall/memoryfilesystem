package com.google.code.memoryfilesystem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class MemoryContentsTest {

  private final boolean direct;

  private MemoryContents contents;

  public MemoryContentsTest(boolean direct) {
    this.direct = direct;
  }

  private ByteBuffer allocate(int capacity) {
    if (direct) {
      return ByteBuffer.allocateDirect(capacity);
    } else {
      return ByteBuffer.allocate(capacity);
    }
  }

  @Parameters
  public static List<Object[]> parameters() {
    return Arrays.asList(new Object[]{true}, new Object[]{false});
  }

  @Before
  public void setUp() {
    this.contents = new MemoryContents(10);

  }

  @Test
  public void firstBlockEmpty() throws IOException {
    ByteBuffer src = allocate(10);
    byte[] data = new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
    src.put(data);
    src.rewind();
    SeekableByteChannel channel = this.contents.newChannel(true, true);
    assertEquals(0, channel.size());
    assertEquals(0, channel.position());
    assertEquals(10, channel.write(src));
    assertEquals(10, channel.size());
    assertEquals(10, channel.position());
    channel.position(0L);

    ByteBuffer dst = allocate(10);
    assertEquals(10, channel.read(dst));
    dst.rewind();
    byte[] extracted = new byte[10];
    dst.get(extracted);
    assertArrayEquals(data, extracted);

    dst.rewind();
    assertEquals(-1, channel.read(dst));
  }

  @Test
  public void writeBlockSizeMinusOne() throws IOException {
  }

  @Test
  public void writeBlockSize() throws IOException {
  }

  @Test
  public void writeBlockSizePlusOne() throws IOException {
  }

}
