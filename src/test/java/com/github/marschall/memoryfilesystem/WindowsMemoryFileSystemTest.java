package com.github.marschall.memoryfilesystem;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;
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
    try (FileSystem fileSystem = MemoryFileSystemBuilder.newWindows().build("uri")) {
      Path c1 = fileSystem.getPath("C:\\");
      Path c2 = fileSystem.getPath("c:\\");
      assertEquals("C:\\", c1.toString());
      assertEquals(c1.hashCode(), c2.hashCode());
      assertTrue(c1.startsWith(c2));
      assertTrue(c1.startsWith("c:\\"));
      assertEquals(c1, c2);
      
      c1 = fileSystem.getPath("C:\\TEMP");
      c2 = fileSystem.getPath("c:\\temp");
      assertEquals("C:\\TEMP", c1.toString());
      assertTrue(c1.startsWith(c2));
      assertTrue(c1.startsWith("c:\\"));
    }
  }
  
  @Test
  public void windowsDiffrentFileSystems() throws IOException {
    URI uri1 = URI.create("memory:uri1");
    URI uri2 = URI.create("memory:uri2");
    Map<String, ?> env = MemoryFileSystemBuilder.newWindows().buildEnvironment();
    try (
            FileSystem fileSystem1 = FileSystems.newFileSystem(uri1, env);
            FileSystem fileSystem2 = FileSystems.newFileSystem(uri2, env)) {
      Path c1 = fileSystem1.getPath("C:\\");
      Path c2 = fileSystem2.getPath("C:\\\\");
      
      assertThat(c1, equalTo(c1));
      assertThat(c2, equalTo(c2));
      
      // different file systems
      assertThat(c1, not(equalTo(c2)));
      assertThat(c2, not(equalTo(c1)));
    }
  }
  
  @Test
  public void windowsQuirky() throws IOException {
    try (FileSystem fileSystem = MemoryFileSystemBuilder.newWindows().build("uri")) {
      Path c1 = fileSystem.getPath("C:\\");
      Path c2 = fileSystem.getPath("c:\\");
      assertEquals("c:\\", c2.toString());

      c1 = fileSystem.getPath("C:\\TEMP");
      c2 = fileSystem.getPath("c:\\temp");
      assertEquals("c:\\temp", c2.toString());
      assertEquals(c1.hashCode(), c2.hashCode());
    }
  }
  
  @Test
  public void pathOrdering() throws IOException {
    try (FileSystem fileSystem = MemoryFileSystemBuilder.newWindows().build("uri")) {
      Path lowerA = fileSystem.getPath("a");
      Path upperA = fileSystem.getPath("A");
      
      assertEquals(0, lowerA.compareTo(lowerA));
      assertEquals(0, upperA.compareTo(lowerA));
      assertEquals(0, lowerA.compareTo(upperA));
      assertEquals(0, upperA.compareTo(upperA));
      
      assertEquals(lowerA, lowerA);
      assertEquals(lowerA, upperA);
      assertEquals(upperA, lowerA);
      assertEquals(upperA, upperA);
      
      Path c = fileSystem.getPath("C:\\");
      Path d = fileSystem.getPath("D:\\");
      assertThat(c, lessThan(d));
      assertThat(d, greaterThan(c));
    }
  }

}
