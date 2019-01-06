package com.github.marschall.memoryfilesystem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.attribute.BasicFileAttributes;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MemoryFileTest {

  private MemoryFile memoryFile;

  @BeforeEach
  void setUp() {
    this.memoryFile = new MemoryFile("", EntryCreationContext.empty());
  }

  @Test
  void testCheckMethods() throws IOException {
    BasicFileAttributes attributes = this.memoryFile.getBasicFileAttributeView().readAttributes();

    assertTrue(attributes.isRegularFile());

    assertFalse(attributes.isDirectory());
    assertFalse(attributes.isOther());
    assertFalse(attributes.isSymbolicLink());

    assertEquals(0L, attributes.size());

    assertNotNull(attributes.fileKey());
  }

}
