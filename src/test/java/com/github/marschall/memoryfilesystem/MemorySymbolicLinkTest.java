package com.github.marschall.memoryfilesystem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.attribute.BasicFileAttributes;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MemorySymbolicLinkTest {

  private MemorySymbolicLink memoryFile;

  @BeforeEach
  public void setUp() {
    this.memoryFile = new MemorySymbolicLink("", null);
  }

  @Test
  public void testCheckMethods() throws IOException {
    BasicFileAttributes attributes = this.memoryFile.getBasicFileAttributeView().readAttributes();

    assertFalse(attributes.isRegularFile());

    assertFalse(attributes.isDirectory());
    assertFalse(attributes.isOther());
    assertTrue(attributes.isSymbolicLink());

    assertEquals(-1L, attributes.size());

    assertNotNull(attributes.fileKey());
  }

}
