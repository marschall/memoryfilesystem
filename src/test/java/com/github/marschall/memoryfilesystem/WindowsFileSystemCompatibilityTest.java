package com.github.marschall.memoryfilesystem;

import static com.github.marschall.memoryfilesystem.FileExistsMatcher.exists;
import static com.github.marschall.memoryfilesystem.IsSameFileMatcher.isSameFile;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.nio.channels.FileChannel;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.DosFileAttributes;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class WindowsFileSystemCompatibilityTest {

  private static final String DISPLAY_NAME = "native: {0}";

  @RegisterExtension
  final WindowsFileSystemExtension extension = new WindowsFileSystemExtension();

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

  @Target(METHOD)
  @Retention(RUNTIME)
  @ParameterizedTest(name = DISPLAY_NAME)
  @MethodSource("fileSystems")
  @interface CompatibilityTest {

  }


  @CompatibilityTest
  void isHidden(boolean useDefault) throws IOException {
    Path hidden = this.getFileSystem(useDefault).getPath("hidden");
    Files.createFile(hidden);
    try {
      Files.setAttribute(hidden, "dos:hidden", true);
      assertTrue(Files.isHidden(hidden));
    } finally {
      Files.delete(hidden);
    }
  }

  @CompatibilityTest
  void isNotHidden(boolean useDefault) throws IOException {
    Path hidden = this.getFileSystem(useDefault).getPath(".not_hidden");
    Files.createFile(hidden);
    try {
      Files.setAttribute(hidden, "dos:hidden", false);
      assertFalse(Files.isHidden(hidden));
    } finally {
      Files.delete(hidden);
    }
  }

  @CompatibilityTest
  void rootAttributes(boolean useDefault) throws IOException {
    FileSystem fileSystem = this.getFileSystem(useDefault);
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

  @CompatibilityTest
  @Disabled("not ready")
  void forbiddenFileNames(boolean useDefault) {
    FileSystem fileSystem = this.getFileSystem(useDefault);
    Path root = fileSystem.getPath("C:\\");
    List<String> forbidden = asList("CON", "PRN", "AUX", "CLOCK$", "NULL", "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9", "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9");
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


  @CompatibilityTest
  void caseInsensitiveCasePreserving(boolean useDefault) throws IOException {
    FileSystem fileSystem = this.getFileSystem(useDefault);
    Path testFile = fileSystem.getPath("tesT");
    try {
      Files.createFile(testFile);
      assertEquals("tesT", testFile.toRealPath().getFileName().toString());

      Path testFile2 = fileSystem.getPath("Test");
      assertThat(testFile2, exists());

      assertEquals("tesT", testFile2.toRealPath().getFileName().toString());

    } finally {
      Files.delete(testFile);
    }
  }

  @CompatibilityTest
  void attributeCapitalization(boolean useDefault) throws IOException {
    FileSystem fileSystem = this.getFileSystem(useDefault);
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

  @CompatibilityTest
  @Disabled
  void windowsNormalization(boolean useDefault) throws IOException {
    FileSystem fileSystem = this.getFileSystem(useDefault);
    String aUmlaut = "\u00C4";
    Path aPath = fileSystem.getPath(aUmlaut);
    String normalized = Normalizer.normalize(aUmlaut, Form.NFD);
    Path nPath = fileSystem.getPath(normalized);

    Path createdFile = null;
    try {
      createdFile = Files.createFile(nPath);
      assertEquals(2, createdFile.getFileName().toString().length());
      assertEquals(2, createdFile.toAbsolutePath().getFileName().toString().length());
      // REVIEW ??
      assertEquals(2, createdFile.toRealPath().getFileName().toString().length());

      assertThat(aPath, not(exists()));
      assertThat(nPath, exists());
      //assertTrue(Files.isSameFile(aPath, nPath));
      //assertTrue(Files.isSameFile(nPath, aPath));
      assertThat(aPath, not(equalTo(nPath)));
    } finally {
      if (createdFile != null) {
        Files.delete(createdFile);
      }
    }
  }


  @CompatibilityTest
  @Disabled
  void windowsNoNormalization(boolean useDefault) throws IOException {
    /*
     * Verifies that Windows does no Unicode normalization and that we can have
     * both a NFC and NFD file.
     */
    FileSystem fileSystem = this.getFileSystem(useDefault);
    String aUmlaut = "\u00C4";
    Path nfcPath = fileSystem.getPath(aUmlaut);
    String normalized = Normalizer.normalize(aUmlaut, Form.NFD);
    Path nfdPath = fileSystem.getPath(normalized);

    Path nfcFile = null;
    Path nfdFile = null;
    try {
      nfcFile = Files.createFile(nfcPath);
      assertEquals(1, nfcFile.getFileName().toString().length());
      assertEquals(1, nfcFile.toAbsolutePath().getFileName().toString().length());
      assertEquals(1, nfcFile.toRealPath().getFileName().toString().length());

      assertThat(nfcPath, exists());
      assertThat(nfdPath, not(exists()));

      nfdFile = Files.createFile(nfdPath);
      assertEquals(2, nfdFile.getFileName().toString().length());
      assertEquals(2, nfdFile.toAbsolutePath().getFileName().toString().length());
      assertEquals(2, nfdFile.toRealPath().getFileName().toString().length());

      assertThat(nfcPath, not(equalTo(nfdPath)));
      assertThat(nfcPath, not(isSameFile(nfdPath)));
      assertThat(nfdPath, not(isSameFile(nfcPath)));
    } finally {
      if (nfcFile != null) {
        Files.delete(nfcFile);
      }
      if (nfdFile != null) {
        Files.delete(nfdFile);
      }
    }
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

    try {
      assertThrows(IOException.class, () -> FileChannel.open(child1, StandardOpenOption.WRITE));
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
