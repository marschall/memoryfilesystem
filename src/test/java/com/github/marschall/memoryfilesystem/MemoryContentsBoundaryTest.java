package com.github.marschall.memoryfilesystem;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class MemoryContentsBoundaryTest {

  private static final String DISPLAY_NAME = "initialOffset: {0}, initialBlocks: {1}, twoWrite: {2}";

  public static List<Object[]> parameters() {
    int blockSize = MemoryInode.BLOCK_SIZE;
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

  private MemoryFile createContents(int initialBlocks) {
    return new MemoryFile("", EntryCreationContext.empty(), initialBlocks);
  }

  @ParameterizedTest(name = DISPLAY_NAME)
  @MethodSource("parameters")
  public void boundaryWrapping(int initialOffset, int initialBlocks, int toWrite) throws IOException {
    Path path = new MockPath();
    SeekableByteChannel channel = createContents(initialBlocks).newChannel(true, true, false, path);
    byte[] initial = new byte[initialOffset];
    channel.write(ByteBuffer.wrap(initial));


    byte[] data = new byte[toWrite];
    data[data.length - 1] = 9;
    data[data.length - 2] = 8;
    data[data.length - 3] = 7;
    data[2] = 3;
    data[1] = 2;
    data[0] = 1;
    ByteBuffer src = ByteBuffer.wrap(data);

    channel.write(src);
    assertEquals(toWrite + initialOffset, channel.size());
    assertEquals(toWrite + initialOffset, channel.position());

    // one element tests

    // read last element
    byte[] oneElement = new byte[1];
    ByteBuffer dst = ByteBuffer.wrap(oneElement);
    channel.position(channel.size() - 1L);
    assertEquals(1, channel.read(dst));
    assertArrayEquals(new byte[]{9}, oneElement);

    // read first element
    dst.rewind();
    long startOfWrite = channel.size() - toWrite;
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
    byte[] readBack = new byte[toWrite];
    dst = ByteBuffer.wrap(readBack);
    channel.position(channel.size() - toWrite);
    assertEquals(toWrite, channel.read(dst));
    assertEquals(1, readBack[0]);
    assertEquals(2, readBack[1]);
    assertEquals(3, readBack[2]);
    assertEquals(7, readBack[readBack.length - 3]);
    assertEquals(8, readBack[readBack.length - 2]);
    assertEquals(9, readBack[readBack.length - 1]);
  }

}
