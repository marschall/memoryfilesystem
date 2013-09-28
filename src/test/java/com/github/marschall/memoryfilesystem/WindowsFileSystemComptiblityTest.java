package com.github.marschall.memoryfilesystem;

import static com.github.marschall.memoryfilesystem.FileExistsMatcher.exists;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.DosFileAttributes;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class WindowsFileSystemComptiblityTest {

  @Rule
  public final WindowsFileSystemRule rule = new WindowsFileSystemRule();

  private FileSystem fileSystem;

  private final boolean useDefault;

  public WindowsFileSystemComptiblityTest(boolean useDefault) {
    this.useDefault = useDefault;
  }

  FileSystem getFileSystem() {
    if (this.fileSystem == null) {
      if (this.useDefault) {
        this.fileSystem = FileSystems.getDefault();
      } else {
        this.fileSystem = this.rule.getFileSystem();
      }
    }
    return this.fileSystem;
  }


  @Parameters(name = "navite: {0}")
  public static List<Object[]> fileSystems() throws IOException {
    FileSystem defaultFileSystem = FileSystems.getDefault();
    Set<String> supportedFileAttributeViews = defaultFileSystem.supportedFileAttributeViews();
    // a DOS view is faked into the unix file system
    boolean isDos = supportedFileAttributeViews.contains("dos") && !supportedFileAttributeViews.contains("unix");
    if (isDos) {
      return Arrays.asList(new Object[]{true},
              new Object[]{false});
    } else {
      return Collections.singletonList(new Object[]{false});
    }
  }


  @Test
  public void isHidden() throws IOException {
    Path hidden = this.getFileSystem().getPath("hidden");
    Files.createFile(hidden);
    try {
      Files.setAttribute(hidden, "dos:hidden", true);
      assertTrue(Files.isHidden(hidden));
    } finally {
      Files.delete(hidden);
    }
  }

  @Test
  public void isNotHidden() throws IOException {
    Path hidden = this.getFileSystem().getPath(".not_hidden");
    Files.createFile(hidden);
    try {
      Files.setAttribute(hidden, "dos:hidden", false);
      assertFalse(Files.isHidden(hidden));
    } finally {
      Files.delete(hidden);
    }
  }

  @Test
  public void rootAttributes() throws IOException {
    FileSystem fileSystem = this.getFileSystem();
    Path root = fileSystem.getPath("C:\\");
    BasicFileAttributes attributes = Files.readAttributes(root, BasicFileAttributes.class);
    assertTrue(attributes.isDirectory());
    assertFalse(attributes.isRegularFile());

    DosFileAttributes dosFileAttributes = Files.readAttributes(root, DosFileAttributes.class);
    assertFalse(dosFileAttributes.isArchive());
    assertTrue(dosFileAttributes.isHidden());
    assertTrue(dosFileAttributes.isSystem());
    assertFalse(dosFileAttributes.isReadOnly());
  }

  @Test
  @Ignore("not ready")
  public void forbiddenFileNames() {
    FileSystem fileSystem = this.getFileSystem();
    Path root = fileSystem.getPath("C:\\");
    List<String> forbidden = asList("CON", "PRN", "AUX", "CLOCK$", "NUL", "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9", "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9");
    for (String each : forbidden) {
      Path forbiddenPath = root.resolve(each);
      try {
        Files.createFile(forbiddenPath);
        fail(forbiddenPath + " should be forbidden");
      } catch (IOException e) {
        // should reach here
      }

      forbiddenPath = root.resolve(each.toLowerCase(Locale.US));
      try {
        Files.createFile(forbiddenPath);
        fail(forbiddenPath + " should be forbidden");
      } catch (IOException e) {
        // should reach here
      }
    }
  }

  @Test
  public void caseInsensitiveCasePreserving() throws IOException {
    FileSystem fileSystem = this.getFileSystem();
    Path root = fileSystem.getPath("C:\\");
    Path testFile = root.resolve("tesT");
    try {
      Files.createFile(testFile);
      assertEquals("C:\\tesT", testFile.toRealPath().toString());

      Path testFile2 = root.resolve("Test");
      assertThat(testFile2, exists());

      assertEquals("C:\\tesT", testFile2.toRealPath().toString());

    } finally {
      Files.delete(testFile);
    }

  }

  @Test
  public void attributeCapitalization() throws IOException {
    FileSystem fileSystem = this.getFileSystem();
    Path root = fileSystem.getPath("C:\\");
    Map<String, Object> attributes = Files.readAttributes(root, "dos:*");
    Set<String> keys = attributes.keySet();

    assertThat(keys, hasItem("hidden"));
    assertThat(keys, hasItem("archive"));
    assertThat(keys, hasItem("system"));

    assertThat(keys, not(hasItem("isHidden")));
    assertThat(keys, not(hasItem("isArchive")));
    assertThat(keys, not(hasItem("isSystem")));
  }

}
