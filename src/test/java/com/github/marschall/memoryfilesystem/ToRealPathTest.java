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
 * Regression test for <a href="https://github.com/marschall/memoryfilesystem/issues/60">Issue 60</a>
 * and <a href="https://github.com/marschall/memoryfilesystem/issues/61">Issue 61</a>.
 */
public class ToRealPathTest {

  @Rule
  public final FileSystemRule rule = new FileSystemRule();

  private Path existingDirectoryPath;
  private Path existingFilePath;

  @Before
  public void before() throws IOException {
    FileSystem fileSystem = this.rule.getFileSystem();

    // lets prepare the filesystem content...
    // structure should looks like:
    /*

    /existingDirectory/existingFile
    /linkDirectory -> /existingDirectory
    /linkFile -> /existingDirectory/existingFile

     */

    this.existingDirectoryPath = fileSystem.getPath("existingDirectory");
    Files.createDirectory(this.existingDirectoryPath);
    this.existingFilePath = this.existingDirectoryPath.resolve("existingFile");
    Files.createFile(this.existingFilePath);


    // create a directory symbolic link from /linkDirectory -> /existingDirectory
    Path linkDirectoryPath = fileSystem.getPath("linkDirectory");
    Files.createSymbolicLink(linkDirectoryPath, this.existingDirectoryPath);

    // check attributes of this directory link
    assertEquals("/existingDirectory", this.existingDirectoryPath.toAbsolutePath().toString());
    assertEquals("/linkDirectory", linkDirectoryPath.toAbsolutePath().toString());

    Path linkDirectoryPath2 = fileSystem.getPath("linkDirectory");
    assertTrue(Files.isSymbolicLink(linkDirectoryPath2));
    assertTrue(Files.isDirectory(linkDirectoryPath2));
    assertEquals(linkDirectoryPath, linkDirectoryPath2);

    // create a symbolic file link
    Path linkFilePath = fileSystem.getPath("linkFile");
    Files.createSymbolicLink(linkFilePath, this.existingFilePath);
  }


  @Test
  public void realPathDirectoryEquals() throws IOException {
    Path linkDirectoryRealPath = this.rule.getFileSystem().getPath("linkDirectory").toRealPath();
    assertEquals(this.existingDirectoryPath.toAbsolutePath(), linkDirectoryRealPath);
    assertEquals(this.existingDirectoryPath.toRealPath(), linkDirectoryRealPath);
  }

  @Test
  public void realPathFileEquals() throws IOException {
    Path linkFileRealPath = this.rule.getFileSystem().getPath("linkFile").toRealPath();
    assertEquals(this.existingFilePath.toRealPath(), linkFileRealPath);

    linkFileRealPath = this.rule.getFileSystem().getPath("linkDirectory").resolve("existingFile").toRealPath();
    assertEquals(this.existingFilePath.toRealPath(), linkFileRealPath);
  }

  @Test
  public void realPathIsDirectory() throws IOException {
    Path linkDirectoryRealPath = this.rule.getFileSystem().getPath("linkDirectory").toRealPath();
    assertTrue(Files.isDirectory(linkDirectoryRealPath));
  }

  @Test
  public void realPathIsFile() throws IOException {
    Path linkFileRealPath = this.rule.getFileSystem().getPath("linkFile").toRealPath();
    assertFalse(Files.isDirectory(linkFileRealPath));
    assertTrue(Files.isRegularFile(linkFileRealPath));
  }

  @Test
  public void realPathIsSymlink() throws IOException {
    Path linkDirectoryRealPath = this.rule.getFileSystem().getPath("linkDirectory").toRealPath();
    assertFalse(Files.isSymbolicLink(linkDirectoryRealPath));

    Path linkFileRealPath = this.rule.getFileSystem().getPath("linkFile").toRealPath();
    assertFalse(Files.isSymbolicLink(linkFileRealPath));

    linkFileRealPath = this.rule.getFileSystem().getPath("linkDirectory").resolve("existingFile").toRealPath();
    assertFalse(Files.isSymbolicLink(linkFileRealPath));
  }

  @Test
  public void realPathDirectoryToString() throws IOException {
    Path linkDirectoryRealPath = this.rule.getFileSystem().getPath("linkDirectory").toRealPath();
    assertEquals("/existingDirectory", linkDirectoryRealPath.toString());
  }

  @Test
  public void realPathFileToString() throws IOException {
    Path linkFileRealPath = this.rule.getFileSystem().getPath("linkFile").toRealPath();
    assertEquals("/existingDirectory/existingFile", linkFileRealPath.toString());

    linkFileRealPath = this.rule.getFileSystem().getPath("linkDirectory").resolve("existingFile").toRealPath();
    assertEquals("/existingDirectory/existingFile", linkFileRealPath.toString());
  }

}