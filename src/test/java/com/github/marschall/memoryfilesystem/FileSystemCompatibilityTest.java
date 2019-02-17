package com.github.marschall.memoryfilesystem;

import static com.github.marschall.memoryfilesystem.FileContentsMatcher.hasContents;
import static com.github.marschall.memoryfilesystem.FileUtility.setContents;
import static com.github.marschall.memoryfilesystem.IsAbsoluteMatcher.isAbsolute;
import static com.github.marschall.memoryfilesystem.IsAbsoluteMatcher.isRelative;
import static com.github.marschall.memoryfilesystem.PathMatchesMatcher.matches;
import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.WRITE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.NonReadableChannelException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.attribute.BasicFileAttributeView;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class FileSystemCompatibilityTest {

  private static final String DISPLAY_NAME = "native: {arguments}";

  @RegisterExtension
  final FileSystemExtension extension = new FileSystemExtension();

  private FileSystem fileSystem;

  static Stream<Boolean> useDefault() {
    return Stream.of(Boolean.TRUE, Boolean.FALSE);
  }

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

  @ParameterizedTest(name = DISPLAY_NAME)
  @MethodSource("useDefault")
  void writeOnly(boolean useDefault) throws IOException {
    Path currentDirectory = this.getFileSystem(useDefault).getPath("");
    Path path = Files.createTempFile(currentDirectory, "task-list", ".png");
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

  @ParameterizedTest(name = DISPLAY_NAME)
  @MethodSource("useDefault")
  void truncate(boolean useDefault) throws IOException {
    Path currentDirectory = this.getFileSystem(useDefault).getPath("");
    Path path = Files.createTempFile(currentDirectory, "sample", ".txt");
    try {
      try (SeekableByteChannel channel = Files.newByteChannel(path, WRITE)) {
        channel.write(ByteBuffer.wrap(new byte[]{1, 2, 3, 4, 5}));
      }
      try (SeekableByteChannel channel = Files.newByteChannel(path, WRITE)) {
        try {
          channel.truncate(-1L);
          fail("negative truncation should not be allowed");
        } catch (IllegalArgumentException e) {
          // should reach here
        }
      }
    } finally {
      Files.delete(path);
    }
  }

  @ParameterizedTest(name = DISPLAY_NAME)
  @MethodSource("useDefault")
  void viewOnNotExistingFile(boolean useDefault) throws IOException {
    Path currentDirectory = this.getFileSystem(useDefault).getPath("");
    Path notExisting = currentDirectory.resolve("not-existing.txt");
    BasicFileAttributeView view = Files.getFileAttributeView(notExisting, BasicFileAttributeView.class);
    assertNotNull(view);
    try {
      view.readAttributes();
      fail("reading from a non-existing view should fail");
    } catch (NoSuchFileException e) {
      // should reach here
    }
  }

  @ParameterizedTest(name = DISPLAY_NAME)
  @MethodSource("useDefault")
  void position(boolean useDefault) throws IOException {
    Path currentDirectory = this.getFileSystem(useDefault).getPath("");
    Path path = Files.createTempFile(currentDirectory, "sample", ".txt");
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

  @ParameterizedTest(name = DISPLAY_NAME)
  @MethodSource("useDefault")
  void emptyPath(boolean useDefault) {
    FileSystem fileSystem = this.getFileSystem(useDefault);
    Path path = fileSystem.getPath("");
    assertThat(path, isRelative());
    assertNull(path.getRoot());
    assertEquals(path, path.getFileName());
    assertEquals(path, path.getName(0));
    assertEquals(path, path.subpath(0, 1));
    assertEquals(1, path.getNameCount());
  }

  @ParameterizedTest(name = DISPLAY_NAME)
  @MethodSource("useDefault")
  void positionAfterTruncate(boolean useDefault) throws IOException {
    Path currentDirectory = this.getFileSystem(useDefault).getPath("");
    Path tempFile = Files.createTempFile(currentDirectory, "prefix", "suffix");
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
  void append() throws IOException {
    Path path = Files.createTempFile("sample", ".txt");
    try (SeekableByteChannel channel = Files.newByteChannel(path, APPEND)) {
      //      channel.position(channel.size());
      assertEquals(0L, channel.position());
      ByteBuffer src = ByteBuffer.wrap(new byte[]{1, 2, 3, 4, 5});
      channel.position(0L);
      channel.write(src);
      assertEquals(5L, channel.position());
      //      channel.truncate(channel.size() - 1L);
      //      channel.truncate(1L);
    } finally {
      Files.delete(path);
    }
  }

  @Test
  void appendPostion() throws IOException {
    Path path = Files.createTempFile("sample", ".txt");
    String originalContent = "0123456789";
    setContents(path, originalContent);
    try (SeekableByteChannel channel = Files.newByteChannel(path, APPEND)) {
      assertEquals(originalContent.length(), channel.position(), "position");
      byte[] appended = new byte[]{'a', 'b', 'c', 'd'};
      ByteBuffer src = ByteBuffer.wrap(appended);
      assertEquals(originalContent.length(), channel.position(), "position");
      channel.write(src);
      assertEquals(originalContent.length() + appended.length, channel.position(), "position");

      channel.truncate(0L);
      assertThat(path, hasContents(""));
    } finally {
      Files.delete(path);
    }
  }

  @ParameterizedTest(name = DISPLAY_NAME)
  @MethodSource("useDefault")
  void root(boolean useDefault) {
    FileSystem fileSystem = this.getFileSystem(useDefault);
    for (Path root : fileSystem.getRootDirectories()) {
      assertThat(root, isAbsolute());
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

  @ParameterizedTest(name = DISPLAY_NAME)
  @MethodSource("useDefault")
  void regression93(boolean useDefault) {
    FileSystem fileSystem = this.getFileSystem(useDefault);

    Path child = fileSystem.getPath(".gitignore");

    PathMatcher matcher = fileSystem.getPathMatcher("glob:**/.gitignore");
    assertThat(matcher, not(matches(child)));
  }

}
