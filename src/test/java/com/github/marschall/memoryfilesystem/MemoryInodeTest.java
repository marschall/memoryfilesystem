package com.github.marschall.memoryfilesystem;

import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.openjdk.jol.vm.VM;

public class MemoryInodeTest {

  @Test
  public void directBlock() {
    byte[] array = new byte[MemoryInode.BLOCK_SIZE];
    assertEquals(4096, VM.current().sizeOf(array));
  }

  @Test
  public void indirectBlock() {
    byte[][] array = new byte[MemoryInode.NUMBER_OF_BLOCKS][];
    assertThat(VM.current().sizeOf(array), lessThanOrEqualTo(16384L));
  }

}
