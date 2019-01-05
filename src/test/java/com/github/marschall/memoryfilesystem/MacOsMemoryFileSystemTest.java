package com.github.marschall.memoryfilesystem;

import static com.github.marschall.memoryfilesystem.FileExistsMatcher.exists;
import static com.github.marschall.memoryfilesystem.IsSameFileMatcher.isSameFile;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class MacOsMemoryFileSystemTest {

  private static final String DISPLAY_NAME = "native: {0}";

  @RegisterExtension
  public final MacOsFileSystemExtension rule = new MacOsFileSystemExtension();

  private FileSystem fileSystem;

  FileSystem getFileSystem(boolean useDefault) {
    if (this.fileSystem == null) {
      if (useDefault) {
        this.fileSystem = FileSystems.getDefault();
      } else {
        this.fileSystem = this.rule.getFileSystem();
      }
    }
    return this.fileSystem;
  }

  public static List<Object[]> fileSystems() {
    String osName = (String) System.getProperties().get("os.name");
    boolean isMac = osName.startsWith("Mac");
    if (isMac) {
      return Arrays.asList(new Object[]{true},
          new Object[]{false});
    } else {
      return Collections.singletonList(new Object[]{false});
    }
  }

  @ParameterizedTest(name = DISPLAY_NAME)
  @MethodSource("fileSystems")
  public void macOsNormalization(boolean useDefault) throws IOException {
    FileSystem fileSystem = this.getFileSystem(useDefault);
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

  @ParameterizedTest(name = DISPLAY_NAME)
  @MethodSource("fileSystems")
  public void macOsComparison(boolean useDefault) throws IOException {
    FileSystem fileSystem = this.getFileSystem(useDefault);
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


  @ParameterizedTest(name = DISPLAY_NAME)
  @MethodSource("fileSystems")
  public void forbiddenCharacters(boolean useDefault) {
    try {
      char c = 0;
      this.getFileSystem(useDefault).getPath(c + ".txt");
      fail("0x00 should be forbidden");
    } catch (InvalidPathException e) {
      // should reach here
    }
  }

  @ParameterizedTest(name = DISPLAY_NAME)
  @MethodSource("fileSystems")
  public void notForbiddenCharacters(boolean useDefault) throws IOException {
    for (int i = 1; i < 128; ++i) {
      char c = (char) i;
      if (c != '/') {
        Path path = this.getFileSystem(useDefault).getPath(c + ".txt");
        assertNotNull(path);
        try {
          Files.createFile(path);
        } finally {
          Files.delete(path);
        }
      }
    }
  }

  @ParameterizedTest(name = DISPLAY_NAME)
  @MethodSource("fileSystems")
  public void macOsPaths(boolean useDefault) {
    FileSystem fileSystem = this.getFileSystem(useDefault);
    String aUmlaut = "\u00C4";
    String normalized = Normalizer.normalize(aUmlaut, Form.NFD);
    assertEquals(1, aUmlaut.length());
    assertEquals(2, normalized.length());
    Path aPath = fileSystem.getPath("/" + aUmlaut);
    Path nPath = fileSystem.getPath("/" + normalized);
    assertEquals(1, aPath.getName(0).toString().length());
    assertThat(aPath, equalTo(nPath));
  }

  @ParameterizedTest(name = DISPLAY_NAME)
  @MethodSource("fileSystems")
  public void caseInsensitivePatterns(boolean useDefault) throws IOException {
    FileSystem fileSystem = this.rule.getFileSystem();

    Path child1 = fileSystem.getPath("child1");
    Files.createFile(child1);

    try {
      boolean found = false;
      try (DirectoryStream<Path> stream = Files.newDirectoryStream(fileSystem.getPath(""))) {
        PathMatcher regexMatcher = fileSystem.getPathMatcher("regex:CHILD.*");
        PathMatcher globMatcher = fileSystem.getPathMatcher("glob:CHILD*");
        for (Path path : stream) {
          assertTrue(regexMatcher.matches(path));
          assertTrue(globMatcher.matches(path));
          found = true;
        }
      }
      assertTrue(found);
    } finally {
      if (useDefault) {
        Files.delete(child1);
      }
    }
  }

}
