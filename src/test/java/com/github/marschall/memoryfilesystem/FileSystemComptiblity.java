package com.github.marschall.memoryfilesystem;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.WRITE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.Iterator;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Ignore;
import org.junit.Test;

public class FileSystemComptiblity {

  @Test
  public void empty() {
    Path path = Paths.get("");
    System.out.println(path.toUri());
  }
  
  @Test
  public void macOsNormalization() throws IOException {
    String aUmlaut = "\u00C4";
    Path aPath = Paths.get(aUmlaut);
    String normalized = Normalizer.normalize(aUmlaut, Form.NFD);
    Path nPath = Paths.get(normalized);
    
    Path createdFile = null;
    try { 
      createdFile = Files.createFile(aPath);
      assertEquals(1, createdFile.getFileName().toString().length());
      assertEquals(1, createdFile.toAbsolutePath().getFileName().toString().length());
      assertEquals(2, createdFile.toRealPath().getFileName().toString().length());
      
      assertTrue(Files.exists(aPath));
      assertTrue(Files.exists(nPath));
      assertTrue(Files.isSameFile(aPath, nPath));
      assertTrue(Files.isSameFile(nPath, aPath));
      assertThat(aPath, not(equalTo(nPath)));
    } finally {
      if (createdFile != null) {
        Files.delete(createdFile);
      }
    }
    
  }
  
  @Test
  public void macOsComparison() throws IOException {
    Path aLower = Paths.get("a");
    Path aUpper = Paths.get("A");
    assertThat(aLower, not(equalTo(aUpper)));
    Path createdFile = null;
    try { 
      createdFile = Files.createFile(aLower);
      assertTrue(Files.exists(aLower));
      assertTrue(Files.exists(aUpper));
      assertTrue(Files.isSameFile(aLower, aUpper));
    } finally {
      if (createdFile != null) {
        Files.delete(createdFile);
      }
    }
  }
  
  @Test
  public void macOsPaths() {
    String aUmlaut = "\u00C4";
    String normalized = Normalizer.normalize(aUmlaut, Form.NFD);
    assertEquals(1, aUmlaut.length());
    assertEquals(2, normalized.length());
    Path aPath = Paths.get("/" + aUmlaut);
    Path nPath = Paths.get("/" + normalized);
    assertEquals(1, aPath.getName(0).toString().length());
    assertThat(aPath, not(equalTo(nPath)));
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
  public void startsWith() {
    FileSystem fileSystem = FileSystems.getDefault();
    
    assertTrue(fileSystem.getPath("a").startsWith(fileSystem.getPath("a")));
    assertFalse(fileSystem.getPath("a").startsWith(fileSystem.getPath("/a")));
    assertFalse(fileSystem.getPath("a").startsWith(fileSystem.getPath("/")));
    assertFalse(fileSystem.getPath("a").startsWith(fileSystem.getPath("")));
    
    assertTrue(fileSystem.getPath("/a").startsWith(fileSystem.getPath("/a")));
    assertFalse(fileSystem.getPath("/a").startsWith(fileSystem.getPath("a")));
    assertTrue(fileSystem.getPath("/a").startsWith(fileSystem.getPath("/")));
    assertFalse(fileSystem.getPath("/a").startsWith(fileSystem.getPath("")));
    
    assertTrue(fileSystem.getPath("/").startsWith(fileSystem.getPath("/")));
    assertFalse(fileSystem.getPath("/").startsWith(fileSystem.getPath("")));
    assertFalse(fileSystem.getPath("/").startsWith(fileSystem.getPath("a")));
    assertFalse(fileSystem.getPath("/").startsWith(fileSystem.getPath("/a")));
    assertFalse(fileSystem.getPath("/").startsWith(fileSystem.getPath("a/b")));
    assertFalse(fileSystem.getPath("/").startsWith(fileSystem.getPath("/a/b")));
    
    assertFalse(fileSystem.getPath("").startsWith(fileSystem.getPath("/")));
    assertTrue(fileSystem.getPath("").startsWith(fileSystem.getPath("")));
    assertFalse(fileSystem.getPath("").startsWith(fileSystem.getPath("a")));
    assertFalse(fileSystem.getPath("").startsWith(fileSystem.getPath("/a")));
    assertFalse(fileSystem.getPath("").startsWith(fileSystem.getPath("a/b")));
    assertFalse(fileSystem.getPath("").startsWith(fileSystem.getPath("/a/b")));
  }
  
  @Test
  public void endsWith() {
    FileSystem fileSystem = FileSystems.getDefault();
    
    assertTrue(fileSystem.getPath("a").endsWith(fileSystem.getPath("a")));
    assertFalse(fileSystem.getPath("a").endsWith(fileSystem.getPath("a/b")));
    assertFalse(fileSystem.getPath("a").endsWith(fileSystem.getPath("/a")));
    assertFalse(fileSystem.getPath("a").endsWith(fileSystem.getPath("/")));
    assertFalse(fileSystem.getPath("a").endsWith(fileSystem.getPath("")));
    assertFalse(fileSystem.getPath("a/b").endsWith(fileSystem.getPath("a")));
    assertTrue(fileSystem.getPath("a/b").endsWith(fileSystem.getPath("b")));
    assertTrue(fileSystem.getPath("a/b").endsWith(fileSystem.getPath("a/b")));
    assertFalse(fileSystem.getPath("a/b").endsWith(fileSystem.getPath("/a")));
    assertFalse(fileSystem.getPath("a/b").endsWith(fileSystem.getPath("/")));
    assertFalse(fileSystem.getPath("a/b").endsWith(fileSystem.getPath("")));
    
    assertTrue(fileSystem.getPath("/a").endsWith(fileSystem.getPath("/a")));
    assertFalse(fileSystem.getPath("/a").endsWith(fileSystem.getPath("/a/b")));
    assertTrue(fileSystem.getPath("/a").endsWith(fileSystem.getPath("a")));
    assertFalse(fileSystem.getPath("/a").endsWith(fileSystem.getPath("/")));
    assertFalse(fileSystem.getPath("/a").endsWith(fileSystem.getPath("")));
    assertFalse(fileSystem.getPath("/a/b").endsWith(fileSystem.getPath("/a")));
    assertTrue(fileSystem.getPath("/a/b").endsWith(fileSystem.getPath("/a/b")));
    assertFalse(fileSystem.getPath("/a/b").endsWith(fileSystem.getPath("/b")));
    assertTrue(fileSystem.getPath("/a/b").endsWith(fileSystem.getPath("b")));
    assertFalse(fileSystem.getPath("/a/b").endsWith(fileSystem.getPath("/a/b/c")));
    assertFalse(fileSystem.getPath("/a/b").endsWith(fileSystem.getPath("a")));
    assertFalse(fileSystem.getPath("/a/b").endsWith(fileSystem.getPath("/")));
    assertFalse(fileSystem.getPath("/a/b").endsWith(fileSystem.getPath("")));
    
    assertTrue(fileSystem.getPath("/").endsWith(fileSystem.getPath("/")));
    assertFalse(fileSystem.getPath("/").endsWith(fileSystem.getPath("")));
    assertFalse(fileSystem.getPath("/").endsWith(fileSystem.getPath("a")));
    assertFalse(fileSystem.getPath("/").endsWith(fileSystem.getPath("/a")));
    assertFalse(fileSystem.getPath("/").endsWith(fileSystem.getPath("a/b")));
    assertFalse(fileSystem.getPath("/").endsWith(fileSystem.getPath("/a/b")));
    
    assertFalse(fileSystem.getPath("").endsWith(fileSystem.getPath("/")));
    assertTrue(fileSystem.getPath("").endsWith(fileSystem.getPath("")));
    assertFalse(fileSystem.getPath("").endsWith(fileSystem.getPath("a")));
    assertFalse(fileSystem.getPath("").endsWith(fileSystem.getPath("/a")));
    assertFalse(fileSystem.getPath("").endsWith(fileSystem.getPath("a/b")));
    assertFalse(fileSystem.getPath("").endsWith(fileSystem.getPath("/a/b")));
  }
  
  @Test
  public void resolve() {
    FileSystem fileSystem = FileSystems.getDefault();
    
    assertEquals(fileSystem.getPath("/"), fileSystem.getPath("/a").resolve(fileSystem.getPath("/")));
  }
  
  @Test
  public void resolveSibling() {
    FileSystem fileSystem = FileSystems.getDefault();
    
    assertEquals(fileSystem.getPath("a"), fileSystem.getPath("/").resolveSibling(fileSystem.getPath("a")));
    assertEquals(fileSystem.getPath("b"), fileSystem.getPath("a").resolveSibling(fileSystem.getPath("b")));
    assertEquals(fileSystem.getPath(""), fileSystem.getPath("/").resolveSibling(fileSystem.getPath("")));
    
    assertEquals(fileSystem.getPath("/c/d"), fileSystem.getPath("/a/b").resolveSibling(fileSystem.getPath("/c/d")));
    assertEquals(fileSystem.getPath("/c/d"), fileSystem.getPath("a/b").resolveSibling(fileSystem.getPath("/c/d")));
    
    assertEquals(fileSystem.getPath(""), fileSystem.getPath("a").resolveSibling(fileSystem.getPath("")));
    assertEquals(fileSystem.getPath("/a"), fileSystem.getPath("/a/b").resolveSibling(fileSystem.getPath("")));
    assertEquals(fileSystem.getPath("a"), fileSystem.getPath("a/b").resolveSibling(fileSystem.getPath("")));
    assertEquals(fileSystem.getPath("a/b"), fileSystem.getPath("").resolveSibling(fileSystem.getPath("a/b")));
    assertEquals(fileSystem.getPath("/a/b"), fileSystem.getPath("").resolveSibling(fileSystem.getPath("/a/b")));
    
    assertEquals(fileSystem.getPath("/"), fileSystem.getPath("a/b").resolveSibling(fileSystem.getPath("/")));
    assertEquals(fileSystem.getPath("/"), fileSystem.getPath("/a/b").resolveSibling(fileSystem.getPath("/")));
    assertEquals(fileSystem.getPath("/"), fileSystem.getPath("").resolveSibling(fileSystem.getPath("/")));
    
    assertEquals(fileSystem.getPath("/"), fileSystem.getPath("/a").resolveSibling(fileSystem.getPath("")));
    assertEquals(fileSystem.getPath(""), fileSystem.getPath("a").resolveSibling(fileSystem.getPath("")));
  }
  
  @Test
  public void aboluteGetParent() {
    FileSystem fileSystem = FileSystems.getDefault();
    
    Path usrBin = fileSystem.getPath("/usr/bin");
    Path usr = fileSystem.getPath("/usr");
    
    assertEquals(usr, usrBin.getParent());
    Path root = fileSystem.getRootDirectories().iterator().next();
    assertEquals(root, usr.getParent());
    
    assertEquals(fileSystem.getPath("usr/bin/a"), fileSystem.getPath("usr/bin").resolve(fileSystem.getPath("a")));
    
    assertEquals(fileSystem.getPath("/"), fileSystem.getPath("/../../..").normalize());
    assertEquals(fileSystem.getPath("/a/b"), fileSystem.getPath("/../a/b").normalize());
    assertEquals(fileSystem.getPath("../../.."), fileSystem.getPath("../../..").normalize());
    assertEquals(fileSystem.getPath("../../.."), fileSystem.getPath(".././../..").normalize());
    assertEquals(fileSystem.getPath("../../a/b/c"), fileSystem.getPath("../../a/b/c").normalize());
    assertEquals(fileSystem.getPath("a"), fileSystem.getPath("a/b/..").normalize());
    assertEquals(fileSystem.getPath(""), fileSystem.getPath("a/..").normalize());
    assertEquals(fileSystem.getPath(".."), fileSystem.getPath("..").normalize());
//    System.out.println(fileSystem.getPath("a").subpath(0, 0)); // throws excepption
    assertEquals(fileSystem.getPath("a"), fileSystem.getPath("/a/b").getName(0));
    assertEquals(fileSystem.getPath("b"), fileSystem.getPath("/a/b").getName(1));
    assertEquals(fileSystem.getPath("a"), fileSystem.getPath("a/b").getName(0));
    assertEquals(fileSystem.getPath("b"), fileSystem.getPath("a/b").getName(1));
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
