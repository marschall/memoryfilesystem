package com.github.marschall.memoryfilesystem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Regression test for <a href="https://github.com/marschall/memoryfilesystem/issues/60">Issue 60</a>.
 */
public class ToRealPathTest {

  @Rule
  public final FileSystemRule rule = new FileSystemRule();

  private Path targetPath;

  @Before
  public void before() throws IOException {
    FileSystem fileSystem = this.rule.getFileSystem();

    // lets prepare the filesystem content...

    this.targetPath = fileSystem.getPath("target");
    Files.createDirectory(this.targetPath);

    // now create a link from /src -> /target
    Path srcPath = fileSystem.getPath("src");
    Files.createSymbolicLink(srcPath, this.targetPath);

    // lets check a bit here
    assertEquals("/target", this.targetPath.toAbsolutePath().toString());
    assertEquals("/src", srcPath.toAbsolutePath().toString());

    Path srcPath2 = fileSystem.getPath("src");
    assertTrue(Files.isSymbolicLink(srcPath2));
    assertTrue(Files.isDirectory(srcPath2));
    assertEquals(srcPath, srcPath2);

    // so far so good
  }


  @Test
  public void realPathEquals() throws IOException {
    Path realPath = this.rule.getFileSystem().getPath("src").toRealPath();
    assertEquals(this.targetPath.toAbsolutePath(), realPath);
  }

  @Test
  public void realPathDirectory() throws IOException {
    Path realPath = this.rule.getFileSystem().getPath("src").toRealPath();
    assertTrue(Files.isDirectory(realPath));
  }

  @Test
  public void realPathSymlink() throws IOException {
    Path realPath = this.rule.getFileSystem().getPath("src").toRealPath();
    assertFalse(Files.isSymbolicLink(realPath));
  }

  @Test
  public void realPathToString() throws IOException {
    Path realPath = this.rule.getFileSystem().getPath("src").toRealPath();
    assertEquals("/target", realPath.toString());
  }

}