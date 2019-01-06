package com.github.marschall.memoryfilesystem;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.openjdk.jol.vm.VM;

class MemoryInodeTest {

  @Test
  void directBlock() {
    byte[] array = new byte[MemoryInode.BLOCK_SIZE];
    assertEquals(4096, VM.current().sizeOf(array));
  }

  @Test
  void indirectBlock() {
    byte[][] array = new byte[MemoryInode.NUMBER_OF_BLOCKS][];
    assertThat(VM.current().sizeOf(array), lessThanOrEqualTo(16384L));
  }

}
