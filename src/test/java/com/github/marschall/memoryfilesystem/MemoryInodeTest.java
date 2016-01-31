package com.github.marschall.memoryfilesystem;

import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.openjdk.jol.util.VMSupport;

public class MemoryInodeTest {

  @Test
  public void directBlock() {
    byte[] array = new byte[MemoryInode.BLOCK_SIZE];
    assertEquals(4096, VMSupport.sizeOf(array));
  }

  @Test
  public void indirectBlock() {
    byte[][] array = new byte[MemoryInode.BLOCK_SIZE][];
    assertThat(VMSupport.sizeOf(array), lessThanOrEqualTo(16384));
  }

}
