package com.github.marschall.memoryfilesystem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.attribute.BasicFileAttributes;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MemoryDirectoryTest {

  private MemoryDirectory memoryDirectory;

  @BeforeEach
  void setUp() {
    this.memoryDirectory = new MemoryDirectory("");
  }


  @Test
  void testCheckMethods() throws IOException {
    BasicFileAttributes attributes = this.memoryDirectory.getBasicFileAttributeView().readAttributes();

    assertTrue(attributes.isDirectory());

    assertFalse(attributes.isRegularFile());
    assertFalse(attributes.isOther());
    assertFalse(attributes.isSymbolicLink());
    assertEquals(-1L, attributes.size());

    assertNotNull(attributes.fileKey());
  }

}
