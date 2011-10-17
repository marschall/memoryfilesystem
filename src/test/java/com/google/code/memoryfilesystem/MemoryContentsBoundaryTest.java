package com.google.code.memoryfilesystem;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertArrayEquals;

import static org.junit.Assert.assertEquals;


@RunWith(Parameterized.class)
public class MemoryContentsBoundaryTest {

  private final int twoWrite;

  private final int initialBlocks;

  private MemoryContents contents;

  public MemoryContentsBoundaryTest(int initialBlocks, int twoWrite) {
    this.twoWrite = twoWrite;
    this.initialBlocks = initialBlocks;
  }

  @Parameters
  public static List<Object[]> parameters() {
    List<Object[]> parameters = new ArrayList<>();
    for (int initialBlockSize = 0; initialBlockSize < 10; initialBlockSize++) {
      for (int multiplier = 1; multiplier < 20; multiplier++) {
        int offset = MemoryContents.BLOCK_SIZE * multiplier;
        parameters.add(new Object[]{initialBlockSize, offset - 1});
        parameters.add(new Object[]{initialBlockSize, offset});
        parameters.add(new Object[]{initialBlockSize, offset + 1});
      }
    }
    return parameters;
  }

  @Before
  public void setUp() {
    this.contents = new MemoryContents(this.initialBlocks);
  }

  @Test
  public void write() throws IOException {
    byte[] data = new byte[this.twoWrite];
    data[data.length - 1] = 9;
    data[data.length - 2] = 8;
    data[data.length - 3] = 7;
    ByteBuffer src = ByteBuffer.wrap(data);

    SeekableByteChannel channel = this.contents.newChannel(true, true);
    channel.write(src);
    assertEquals(twoWrite, channel.size());
    assertEquals(twoWrite, channel.position());

    byte[] lastOne = new byte[1];
    ByteBuffer dst = ByteBuffer.wrap(lastOne);
    channel.position(channel.size() - 1L);
    assertEquals(1, channel.read(dst));
    assertArrayEquals(new byte[]{9}, lastOne);

    byte[] lastTwo = new byte[2];
    dst = ByteBuffer.wrap(lastTwo);
    channel.position(channel.size() - 2L);
    assertEquals(2, channel.read(dst));
    assertArrayEquals(new byte[]{8, 9}, lastTwo);

    byte[] lastThree = new byte[3];
    dst = ByteBuffer.wrap(lastThree);
    channel.position(channel.size() - 3L);
    assertEquals(3, channel.read(dst));
    assertArrayEquals(new byte[]{7, 8, 9}, lastThree);
  }

}
