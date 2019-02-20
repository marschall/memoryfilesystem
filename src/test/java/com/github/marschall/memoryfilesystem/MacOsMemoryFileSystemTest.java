package com.github.marschall.memoryfilesystem;

import static com.github.marschall.memoryfilesystem.FileExistsMatcher.exists;
import static com.github.marschall.memoryfilesystem.IsSameFileMatcher.isSameFile;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.StandardOpenOption;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class MacOsMemoryFileSystemTest {

  private static final String DISPLAY_NAME = "native: {0}";

  @RegisterExtension
  final MacOsFileSystemExtension extension = new MacOsFileSystemExtension();

  private FileSystem fileSystem;

  FileSystem getFileSystem(boolean useDefault) {
    if (this.fileSystem == null) {
      if (useDefault) {
        this.fileSystem = FileSystems.getDefault();
      } else {
        this.fileSystem = this.extension.getFileSystem();
      }
    }
    return this.fileSystem;
  }

  static List<Object[]> fileSystems() {
    String osName = (String) System.getProperties().get("os.name");
    boolean isMac = osName.startsWith("Mac");
    if (isMac) {
      return Arrays.asList(new Object[]{true},
              new Object[]{false});
    } else {
      return Collections.singletonList(new Object[]{false});
    }
  }

  @Target(METHOD)
  @Retention(RUNTIME)
  @ParameterizedTest(name = DISPLAY_NAME)
  @MethodSource("fileSystems")
  @interface CompatibilityTest {

  }

  @CompatibilityTest
  void macOsNormalization(boolean useDefault) throws IOException {
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

  @CompatibilityTest
  void macOsComparison(boolean useDefault) throws IOException {
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


  @CompatibilityTest
  void forbiddenCharacters(boolean useDefault) {
    try {
      char c = 0;
      this.getFileSystem(useDefault).getPath(c + ".txt");
      fail("0x00 should be forbidden");
    } catch (InvalidPathException e) {
      // should reach here
    }
  }

  @CompatibilityTest
  void notForbiddenCharacters(boolean useDefault) throws IOException {
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

  @CompatibilityTest
  void macOsPaths(boolean useDefault) {
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

  @CompatibilityTest
  void caseInsensitivePatterns(boolean useDefault) throws IOException {
    FileSystem fileSystem = this.extension.getFileSystem();

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

  @CompatibilityTest
  void channelOnDirectoryReading(boolean useDefault) throws IOException {
    FileSystem fileSystem = this.getFileSystem(useDefault);

    Path child1 = fileSystem.getPath("child1");
    Files.createDirectory(child1);

    try (FileChannel channel = FileChannel.open(child1, StandardOpenOption.READ)) {
      channel.force(true);
      assertThrows(IOException.class, () -> channel.read(ByteBuffer.allocate(1)));
    } finally {
      if (useDefault) {
        Files.delete(child1);
      }
    }
  }

  @CompatibilityTest
  void channelOnDirectoryWriting(boolean useDefault) throws IOException {
    FileSystem fileSystem = this.getFileSystem(useDefault);

    Path child1 = fileSystem.getPath("child1");
    Files.createDirectory(child1);

    try {
      assertThrows(IOException.class, () -> FileChannel.open(child1, StandardOpenOption.WRITE));
    } finally {
      if (useDefault) {
        Files.delete(child1);
      }
    }
  }

}
