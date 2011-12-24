package com.google.code.memoryfilesystem;

import java.nio.file.attribute.BasicFileAttributeView;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class MemoryEntryTest {
  
  private final MemoryEntry memoryEntry;
  

  public MemoryEntryTest(MemoryEntry memoryEntry) {
    this.memoryEntry = memoryEntry;
  }
  
  @Parameters
  public static List<Object[]> data() {
    return Arrays.asList(new Object[][] {
        { new MemoryDirectory() },
        { new MemoryFile() }
    });
  }


  @Test
  public void basicViewName() {
    BasicFileAttributeView view = memoryEntry.getBasicFileAttributeView();
    assertEquals("basic", view.name());
  }

}
