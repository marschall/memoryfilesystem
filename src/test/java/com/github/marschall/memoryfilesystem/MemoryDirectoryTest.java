package com.github.marschall.memoryfilesystem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.attribute.BasicFileAttributes;

import org.junit.Before;
import org.junit.Test;

public class MemoryDirectoryTest {
  
  private MemoryDirectory memoryDirectory;

  @Before
  public void setUp() {
    memoryDirectory = new MemoryDirectory("");
  }


  @Test
  public void testCheckMethods() throws IOException {
    BasicFileAttributes attributes = memoryDirectory.getBasicFileAttributeView().readAttributes();
    
    assertTrue(attributes.isDirectory());
    
    assertFalse(attributes.isRegularFile());
    assertFalse(attributes.isOther());
    assertFalse(attributes.isSymbolicLink());
    assertEquals(-1L, attributes.size());
    
    assertNotNull(attributes.fileKey());
  }

}
