package com.github.marschall.memoryfilesystem;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.file.attribute.BasicFileAttributes;

import org.junit.Before;
import org.junit.Test;

public class MemorySymbolicLinkTest {

  private MemorySymbolicLink memoryFile;

  @Before
  public void setUp() {
    memoryFile = new MemorySymbolicLink("", null);
  }

  @Test
  public void testCheckMethods() throws IOException {
    BasicFileAttributes attributes = memoryFile.getBasicFileAttributeView().readAttributes();
    
    assertFalse(attributes.isRegularFile());
    
    assertFalse(attributes.isDirectory());
    assertFalse(attributes.isOther());
    assertTrue(attributes.isSymbolicLink());
    
    assertEquals(-1L, attributes.size());
    
    assertNotNull(attributes.fileKey());
  }

}
