package com.github.marschall.memoryfilesystem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Map;

import org.junit.Test;

public class WindowsMemoryFileSystemTest {
  
  @Test
  public void windows() throws IOException {
    URI uri = URI.create("memory:uri");
    Map<String, ?> env = EnvironmentBuilder.newWindows().build();
    try (FileSystem fileSystem = FileSystems.newFileSystem(uri, env)) {
      Path c1 = fileSystem.getPath("C:\\");
      Path c2 = fileSystem.getPath("c:\\");
      assertEquals("C:\\", c1.toString());
      assertEquals(c1.hashCode(), c2.hashCode());
      assertTrue(c1.startsWith(c2));
      assertTrue(c1.startsWith("c:\\"));
      assertTrue(c1.equals(c2)); //FIXME
      assertEquals(c1, c2);
      
      c1 = fileSystem.getPath("C:\\TEMP");
      c2 = fileSystem.getPath("c:\\temp");
      assertEquals("C:\\TEMP", c1.toString());
      assertTrue(c1.startsWith(c2));
      assertTrue(c1.startsWith("c:\\"));
    }
  }
  
  @Test
  public void windowsQuirky() throws IOException {
    URI uri = URI.create("memory:uri");
    Map<String, ?> env = EnvironmentBuilder.newWindows().build();
    try (FileSystem fileSystem = FileSystems.newFileSystem(uri, env)) {
      Path c1 = fileSystem.getPath("C:\\");
      Path c2 = fileSystem.getPath("c:\\");
      assertEquals("c:\\", c2.toString());
      
      c1 = fileSystem.getPath("C:\\TEMP");
      c2 = fileSystem.getPath("c:\\temp");
      assertEquals("c:\\temp", c2.toString());
      assertEquals(c1.hashCode(), c2.hashCode());
    }
  }

}
