package com.github.marschall.memoryfilesystem;

import static com.github.marschall.memoryfilesystem.FileExistsMatcher.exists;
import static com.github.marschall.memoryfilesystem.IsSameFileMatcher.isSameFile;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class MacOsMemoryFileSystemTest {

  @Rule
  public final MacOsFileSystemRule rule = new MacOsFileSystemRule();

  private FileSystem fileSystem;

  private final boolean useDefault;

  public MacOsMemoryFileSystemTest(boolean useDefault) {
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
    String osName = (String) System.getProperties().get("os.name");
    boolean isMac = osName.startsWith("Mac");
    if (isMac) {
      return Arrays.asList(new Object[]{true},
              new Object[]{false});
    } else {
      return Collections.singletonList(new Object[]{false});
    }
  }

  @Test
  public void macOsNormalization() throws IOException {
    FileSystem fileSystem = this.getFileSystem();
    String aUmlaut = "\u00C4";
    Path aPath = fileSystem.getPath(aUmlaut);
    String normalized = Normalizer.normalize(aUmlaut, Form.NFD);
    Path nPath = fileSystem.getPath(normalized);

    Path createdFile = null;
    try {
      createdFile = Files.createFile(aPath);
      assertEquals(1, createdFile.getFileName().toString().length());
      assertEquals(1, createdFile.toAbsolutePath().getFileName().toString().length());
      assertEquals(1, createdFile.toRealPath().getFileName().toString().length());

      assertThat(aPath, exists());
      assertThat(nPath, exists());
      assertThat(aPath, isSameFile(nPath));
      assertThat(nPath, isSameFile(aPath));
      assertThat(aPath, equalTo(nPath));
    } finally {
      if (createdFile != null) {
        Files.delete(createdFile);
      }
    }
  }

  @Test
  public void macOsComparison() throws IOException {
    FileSystem fileSystem = this.getFileSystem();
    Path aLower = fileSystem.getPath("a");
    Path aUpper = fileSystem.getPath("A");
    assertThat(aLower, not(equalTo(aUpper)));
    Path createdFile = null;
    try {
      createdFile = Files.createFile(aLower);
      assertThat(aLower, exists());
      assertThat(aUpper, exists());
      assertThat(aLower, isSameFile(aUpper));
    } finally {
      if (createdFile != null) {
        Files.delete(createdFile);
      }
    }
  }


  @Test
  public void forbiddenCharacters() throws IOException {
    try {
      char c = 0;
      this.getFileSystem().getPath(c + ".txt");
      fail("0x00 should be forbidden");
    } catch (InvalidPathException e) {
      // should reach here
    }
  }

  @Test
  public void notForbiddenCharacters() throws IOException {
    for (int i = 1; i < 128; ++i) {
      char c = (char) i;
      if (c != '/') {
        Path path = this.getFileSystem().getPath(c + ".txt");
        assertNotNull(path);
        try {
          Files.createFile(path);
        } finally {
          Files.delete(path);
        }
      }
    }
  }

  @Test
  public void macOsPaths() throws IOException {
    FileSystem fileSystem = this.getFileSystem();
    String aUmlaut = "\u00C4";
    String normalized = Normalizer.normalize(aUmlaut, Form.NFD);
    assertEquals(1, aUmlaut.length());
    assertEquals(2, normalized.length());
    Path aPath = fileSystem.getPath("/" + aUmlaut);
    Path nPath = fileSystem.getPath("/" + normalized);
    assertEquals(1, aPath.getName(0).toString().length());
    assertThat(aPath, equalTo(nPath));
  }

}
