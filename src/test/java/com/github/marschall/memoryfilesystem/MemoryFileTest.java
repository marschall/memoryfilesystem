package com.github.marschall.memoryfilesystem;

import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.attribute.BasicFileAttributes;

import org.junit.Before;
import org.junit.Test;
import org.openjdk.jol.util.VMSupport;

public class MemoryFileTest {

  private MemoryFile memoryFile;

  @Before
  public void setUp() {
    this.memoryFile = new MemoryFile("", EntryCreationContext.empty());
  }

  @Test
  public void directBlock() {
    byte[] array = new byte[MemoryFile.BLOCK_SIZE];
    assertEquals(4096, VMSupport.sizeOf(array));
  }

  @Test
  public void indirectBlock() {
    byte[][] array = new byte[MemoryFile.BLOCK_SIZE][];
    assertThat(VMSupport.sizeOf(array), lessThanOrEqualTo(16384));
  }

  @Test
  public void testCheckMethods() throws IOException {
    BasicFileAttributes attributes = this.memoryFile.getBasicFileAttributeView().readAttributes();

    assertTrue(attributes.isRegularFile());

    assertFalse(attributes.isDirectory());
    assertFalse(attributes.isOther());
    assertFalse(attributes.isSymbolicLink());

    assertEquals(0L, attributes.size());

    assertNotNull(attributes.fileKey());
  }

}
