package com.github.marschall.memoryfilesystem;

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

import com.github.marschall.memoryfilesystem.MemoryContents;

import static org.junit.Assert.assertArrayEquals;

import static org.junit.Assert.assertEquals;


@RunWith(Parameterized.class)
public class MemoryContentsBoundaryTest {
  
  private final int initialOffset;

  private final int initialBlocks;
  
  private final int toWrite;

  private MemoryContents contents;

  public MemoryContentsBoundaryTest(int initialOffset, int initialBlocks, int twoWrite) {
    this.initialOffset = initialOffset;
    this.toWrite = twoWrite;
    this.initialBlocks = initialBlocks;
  }

  @Parameters
  public static List<Object[]> parameters() {
    int blockSize = MemoryContents.BLOCK_SIZE;
    List<Object[]> parameters = new ArrayList<>();
    for (int initialOffset : new int[]{0, blockSize - 2, blockSize - 1, blockSize, blockSize + 1, blockSize + 2}) {
      for (int initialBlockSize = 0; initialBlockSize < 10; initialBlockSize++) {
        for (int multiplier = 1; multiplier < 5; multiplier++) {
          int offset = blockSize * multiplier;
          parameters.add(new Object[]{initialOffset, initialBlockSize, offset - 2});
          parameters.add(new Object[]{initialOffset, initialBlockSize, offset - 1});
          parameters.add(new Object[]{initialOffset, initialBlockSize, offset});
          parameters.add(new Object[]{initialOffset, initialBlockSize, offset + 1});
          parameters.add(new Object[]{initialOffset, initialBlockSize, offset + 2});
        }
      }
    }
    return parameters;
  }

  @Before
  public void setUp() {
    this.contents = new MemoryContents(this.initialBlocks);
  }

  @Test
  public void boundaryWrapping() throws IOException {
    SeekableByteChannel channel = this.contents.newChannel(true, true);
    byte[] initial = new byte[this.initialOffset];
    channel.write(ByteBuffer.wrap(initial));
    
    
    byte[] data = new byte[this.toWrite];
    data[data.length - 1] = 9;
    data[data.length - 2] = 8;
    data[data.length - 3] = 7;
    data[2] = 3;
    data[1] = 2;
    data[0] = 1;
    ByteBuffer src = ByteBuffer.wrap(data);

    channel.write(src);
    assertEquals(toWrite + this.initialOffset, channel.size());
    assertEquals(toWrite + this.initialOffset, channel.position());

    // one element tests
    
    // read last element
    byte[] oneElement = new byte[1];
    ByteBuffer dst = ByteBuffer.wrap(oneElement);
    channel.position(channel.size() - 1L);
    assertEquals(1, channel.read(dst));
    assertArrayEquals(new byte[]{9}, oneElement);
    
    // read first element
    dst.rewind();
    long startOfWrite = channel.size() - (long) toWrite;
    channel.position(startOfWrite);
    assertEquals(1, channel.read(dst));
    assertArrayEquals(new byte[]{1}, oneElement);
    
    // two element tests

    // read last two elements
    byte[] twoElements = new byte[2];
    dst = ByteBuffer.wrap(twoElements);
    channel.position(channel.size() - 2L);
    assertEquals(2, channel.read(dst));
    assertArrayEquals(new byte[]{8, 9}, twoElements);
    
    // read first two elements
    dst.rewind();
    channel.position(startOfWrite);
    assertEquals(2, channel.read(dst));
    assertArrayEquals(new byte[]{1, 2}, twoElements);

    
    // read last three elements
    byte[] threeElements = new byte[3];
    dst = ByteBuffer.wrap(threeElements);
    channel.position(channel.size() - 3L);
    assertEquals(3, channel.read(dst));
    assertArrayEquals(new byte[]{7, 8, 9}, threeElements);
    
    // read first two elements
    dst.rewind();
    channel.position(startOfWrite);
    assertEquals(3, channel.read(dst));
    assertArrayEquals(new byte[]{1, 2, 3}, threeElements);
    
    // read the full data back
    byte[] readBack = new byte[this.toWrite];
    dst = ByteBuffer.wrap(readBack);
    channel.position(channel.size() - (long) this.toWrite);
    assertEquals(this.toWrite, channel.read(dst));
    assertEquals(1, readBack[0]);
    assertEquals(2, readBack[1]);
    assertEquals(3, readBack[2]);
    assertEquals(7, readBack[readBack.length - 3]);
    assertEquals(8, readBack[readBack.length - 2]);
    assertEquals(9, readBack[readBack.length - 1]);
  }

}
