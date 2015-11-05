package com.github.marschall.memoryfilesystem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Regression test for <a href="https://github.com/marschall/memoryfilesystem/issues/60">Issue 60</a>.
 */
public class ToRealPathTest {

  private FileSystem fileSystem;
  private Path targetPath;

  @Before
  public void before() throws IOException {
    this.fileSystem = MemoryFileSystemBuilder.newEmpty().build("Issue60");

    // lets prepare the filesystem content...

    this.targetPath = this.fileSystem.getPath("target");
    Files.createDirectory(this.targetPath);

    // now create a link from /src -> /target
    Path srcPath = this.fileSystem.getPath("src");
    Files.createSymbolicLink(srcPath, this.targetPath);

    // lets check a bit here
    assertEquals("/target", this.targetPath.toAbsolutePath().toString());
    assertEquals("/src", srcPath.toAbsolutePath().toString());

    Path srcPath2 = this.fileSystem.getPath("src");
    assertTrue(Files.isSymbolicLink(srcPath2));
    assertTrue(Files.isDirectory(srcPath2));
    assertEquals(srcPath, srcPath2);

    // so far so good
  }

  @After
  public void after() throws IOException {
    this.fileSystem.close();
  }

  @Test
  public void realPathEquals() throws IOException {
    Path realPath = this.fileSystem.getPath("src").toRealPath();
    assertEquals(this.targetPath.toAbsolutePath(), realPath);
  }

  @Test
  public void realPathDirectory() throws IOException {
    Path realPath = this.fileSystem.getPath("src").toRealPath();
    assertTrue(Files.isDirectory(realPath));
  }

  @Test
  public void realPathSymlink() throws IOException {
    Path realPath = this.fileSystem.getPath("src").toRealPath();
    assertFalse(Files.isSymbolicLink(realPath));
  }

  @Test
  public void realPathToString() throws IOException {
    Path realPath = this.fileSystem.getPath("src").toRealPath();
    assertEquals("/target", realPath.toString());
  }

}