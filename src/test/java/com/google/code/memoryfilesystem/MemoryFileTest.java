package com.google.code.memoryfilesystem;

import java.io.IOException;
import java.nio.file.attribute.BasicFileAttributes;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MemoryFileTest {
  
  private MemoryFile memoryFile;

  @Before
  public void setUp() {
    memoryFile = new MemoryFile();
  }

  @Test
  public void testCheckMethods() throws IOException {
    BasicFileAttributes attributes = memoryFile.getBasicFileAttributeView().readAttributes();
    
    assertTrue(attributes.isRegularFile());
    
    assertFalse(attributes.isDirectory());
    assertFalse(attributes.isOther());
    assertFalse(attributes.isSymbolicLink());
    
    assertEquals(0L, attributes.size());
    
    assertNotNull(attributes.fileKey());
  }

}
