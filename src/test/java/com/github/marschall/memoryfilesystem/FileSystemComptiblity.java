package com.github.marschall.memoryfilesystem;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.NonReadableChannelException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.WRITE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class FileSystemComptiblity {

  @Test
  public void empty() {
    Path path = Paths.get("");
    System.out.println(path.toUri());
  }

  @Test
  @Ignore("only on Windows")
  public void windows() throws IOException {
    Path c1 = Paths.get("C:\\");
    Path c2 = Paths.get("c:\\");
    assertEquals(c1, c2);
    assertEquals("C:\\", c1.toString());
    assertEquals("c:\\", c2.toString());
    assertTrue(c1.startsWith(c2));
    assertTrue(c1.startsWith("c:\\"));
    
    //TODO
    
    c1.toRealPath();
  }
  
  @Test
  public void positionAfterTruncate() throws IOException {
    Path tempFile = Files.createTempFile("prefix", "suffix");
    try {
      ByteBuffer src = ByteBuffer.wrap(new byte[]{1, 2, 3, 4, 5});
      try (SeekableByteChannel channel = Files.newByteChannel(tempFile, READ, WRITE)) {
        channel.write(src);
        assertEquals(5L, channel.position());
        assertEquals(5L, channel.size());
        channel.truncate(2L);
        assertEquals(2L, channel.position());
        assertEquals(2L, channel.size());
      }
    } finally {
      Files.delete(tempFile);
    }
  }

  @Test
  public void writeOnly() throws IOException {
    Path path = Files.createTempFile("task-list", ".png");
    try (SeekableByteChannel channel = Files.newByteChannel(path, WRITE)) {
      channel.position(100);
      ByteBuffer buffer = ByteBuffer.allocate(100);
      try {
        channel.read(buffer);
        fail("should not be readable");
      } catch (NonReadableChannelException e) {
        assertTrue(true);
      }
    } finally {
      Files.delete(path);
    }
  }

  @Test
  public void position() throws IOException {
    Path path = Files.createTempFile("sample", ".txt");
    try (SeekableByteChannel channel = Files.newByteChannel(path, WRITE)) {
      assertEquals(0L, channel.position());
      
      channel.position(5L);
      assertEquals(5L, channel.position());
      assertEquals(0, channel.size());
      
      ByteBuffer src = ByteBuffer.wrap(new byte[]{1, 2, 3, 4, 5});
      assertEquals(5, channel.write(src));
      
      assertEquals(10L, channel.position());
      assertEquals(10L, channel.size());
    } finally {
      Files.delete(path);
    }
    
  }
  @Test
  public void append() throws IOException {
    Path path = Files.createTempFile("sample", ".txt");
    try (SeekableByteChannel channel = Files.newByteChannel(path, APPEND)) {
      //			channel.position(channel.size());
      System.out.println(channel.position());
      ByteBuffer src = ByteBuffer.wrap(new byte[]{1, 2, 3, 4, 5});
      channel.position(0L);
      channel.write(src);
      System.out.println(channel.position());
      //			channel.truncate(channel.size() - 1L);
      //			channel.truncate(1L);
    } finally {
      Files.delete(path);
    }
  }
  
  @Test
  public void toUri() {
    FileSystem fileSystem = FileSystems.getDefault();
    Path path = fileSystem.getPath("Users");
    URI uri = path.toUri();
  }
  
  @Test
  public void iterator() {
    FileSystem fileSystem = FileSystems.getDefault();
    Path path = fileSystem.getPath("/Users/marschall/Documents");
    Iterator<Path> iterator = path.iterator();
    while (iterator.hasNext()) {
      Path next = (Path) iterator.next();
      assertFalse(next.isAbsolute());
    }
  }
  
  @Test
  @Ignore("mac os")
  public void getPath() {
    FileSystem fileSystem = FileSystems.getDefault();
    Path path = fileSystem.getPath("/Users/marschall/Documents");
    assertTrue(Files.exists(path));
    
    path = fileSystem.getPath("/", "Users/marschall/Documents");
    assertTrue(Files.exists(path));
    
    path = fileSystem.getPath("/", "/Users/marschall/Documents");
    assertTrue(Files.exists(path));
    
    path = fileSystem.getPath("", "/Users/marschall/Documents");
    assertTrue(Files.exists(path));
    
    path = fileSystem.getPath("/Users", "/marschall/Documents");
    assertTrue(Files.exists(path));
    
    path = fileSystem.getPath("/Users", "marschall/Documents");
    assertTrue(Files.exists(path));
    
    path = fileSystem.getPath("/", "Users/marschall/Documents");
    assertTrue(Files.exists(path));
  }
  
  @Test
  public void aboluteGetParent() {
    
    FileSystem fileSystem = FileSystems.getDefault();
    Path usrBin = fileSystem.getPath("/usr/bin");
    Path usr = fileSystem.getPath("/usr");
    
    assertEquals(usr, usrBin.getParent());
    Path root = fileSystem.getRootDirectories().iterator().next();
    assertEquals(root, usr.getParent());
  }
  
  @Test
  public void relativeGetParent() {
    
    FileSystem fileSystem = FileSystems.getDefault();
    Path usrBin = fileSystem.getPath("usr/bin");
    Path usr = fileSystem.getPath("usr");
    
    assertEquals(usr, usrBin.getParent());
    assertNull(usr.getParent());
  }
  
  @Test
  public void getFileName() {
    
    FileSystem fileSystem = FileSystems.getDefault();
    Path usrBin = fileSystem.getPath("/usr/bin");
    Path bin = fileSystem.getPath("bin");
    
    assertTrue(Files.isDirectory(usrBin));
    assertFalse(Files.isRegularFile(usrBin));
    
    Path fileName = usrBin.getFileName();
    assertEquals(fileName, bin);
    assertFalse(fileName.isAbsolute());
  }
  
  @Test
  public void emptyPath() {
    FileSystem fileSystem = FileSystems.getDefault();
    Path path = fileSystem.getPath("");
    assertFalse(path.isAbsolute());
    assertNull(path.getRoot());
  }
  
  @Test
  public void relativePath() {
    FileSystem fileSystem = FileSystems.getDefault();
    Path path = fileSystem.getPath("Documents");
    assertFalse(path.isAbsolute());
    assertNull(path.getRoot());
  }
  
  @Test
  public void absolutePath() {
    FileSystem fileSystem = FileSystems.getDefault();
    Path path = fileSystem.getPath("/Documents");
    assertTrue(path.isAbsolute());
    assertNotNull(path.getRoot());
  }
  
  @Test(expected = IllegalArgumentException.class)
  public void slash() {
    FileSystem fileSystem = FileSystems.getDefault();
    Path path = fileSystem.getPath("/");
    path.subpath(0, 1);
  }

  @Test
  public void root() {
    FileSystem fileSystem = FileSystems.getDefault();
    for (Path root : fileSystem.getRootDirectories()) {
      assertTrue(root.isAbsolute());
      assertEquals(root, root.getRoot());
      assertNull(root.getFileName());
      assertNull(root.getParent());
      assertEquals(root, root.normalize());
      assertEquals(root, root.toAbsolutePath());


      assertEquals(0, root.getNameCount());
      assertFalse(root.iterator().hasNext());
      for (int i = -1; i < 2; ++i) {
        try {
          root.getName(i);
          fail("root should not support #getName(int)");
        } catch (IllegalArgumentException e) {
          // should reach here
        }
      }
    }
  }


  @Test
  public void test() {
    FileSystem defaultFileSystem = FileSystems.getDefault();
    Iterable<Path> rootDirectories = defaultFileSystem.getRootDirectories();
    Path root = rootDirectories.iterator().next();
    assertTrue(root.endsWith(root));
    try {
      root.endsWith((String) null);
      fail("path#endsWith(null) should not work");
    } catch (NullPointerException e) {
      // should reach here
    }
    try {
      root.startsWith((String) null);
      fail("path#startsWith(null) should not work");
    } catch (NullPointerException e) {
      // should reach here
    }
    assertTrue(root.startsWith(root));
    assertFalse(root.startsWith(""));
    assertTrue(root.startsWith("/"));
    assertTrue(root.endsWith("/"));
    assertFalse(root.endsWith(""));
  }

}
