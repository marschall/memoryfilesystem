package com.github.marschall.memoryfilesystem;

import static com.github.marschall.memoryfilesystem.Constants.SAMPLE_ENV;
import static com.github.marschall.memoryfilesystem.FileExistsMatcher.exists;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE_NEW;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemException;
import java.nio.file.FileSystemLoopException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.UserDefinedFileAttributeView;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.regex.PatternSyntaxException;

import org.junit.Rule;
import org.junit.Test;

public class MemoryFileSystemTest {

  // spawn more tree blocks
  private static final int SAMPLE_ITERATIONS = 1000;

  private static final byte[] SAMPLE_DATA = new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9};

  @Rule
  public final FileSystemRule rule = new FileSystemRule();

  @Test
  public void scatteringRead() throws IOException {
    FileSystem fileSystem = this.rule.getFileSystem();

    Path path = fileSystem.getPath("file.txt");
    Files.createFile(path);
    this.setContents(path, "abcd");

    byte[] a = new byte[1];
    byte[] b = new byte[1];
    byte[] c = new byte[1];
    byte[] d = new byte[1];

    try (FileChannel channel = FileChannel.open(path, READ)) {
      ByteBuffer[] buffers = new ByteBuffer[]{ByteBuffer.wrap(a), ByteBuffer.wrap(b), ByteBuffer.wrap(c), ByteBuffer.wrap(d)};
      long read = channel.read(buffers, 1, 2);
      assertEquals("bytes read", 2L, read);
    }

    assertArrayEquals(new byte[]{0}, a);
    assertArrayEquals(new byte[]{'a'}, b);
    assertArrayEquals(new byte[]{'b'}, c);
    assertArrayEquals(new byte[]{0}, d);
  }

  @Test
  public void scatteringWrite() throws IOException {
    FileSystem fileSystem = this.rule.getFileSystem();

    Path path = fileSystem.getPath("file.txt");
    ByteBuffer a = ByteBuffer.wrap(new byte[]{'a'});
    ByteBuffer b = ByteBuffer.wrap(new byte[]{'b'});
    ByteBuffer c = ByteBuffer.wrap(new byte[]{'c'});
    ByteBuffer d = ByteBuffer.wrap(new byte[]{'d'});

    try (FileChannel channel = FileChannel.open(path, CREATE_NEW, WRITE)) {
      long written = channel.write(new ByteBuffer[]{a, b, c, d}, 1, 2);
      assertEquals("byte written", 2L, written);
    }
    this.assertContents(path, "bc");
  }

  @Test
  public void scatteringWriteAppend() throws IOException {
    FileSystem fileSystem = this.rule.getFileSystem();

    Path path = fileSystem.getPath("file.txt");

    Files.createFile(path);
    this.setContents(path, "z");

    ByteBuffer a = ByteBuffer.wrap(new byte[]{'a'});
    ByteBuffer b = ByteBuffer.wrap(new byte[]{'b'});
    ByteBuffer c = ByteBuffer.wrap(new byte[]{'c'});
    ByteBuffer d = ByteBuffer.wrap(new byte[]{'d'});

    try (FileChannel channel = FileChannel.open(path, APPEND)) {
      long written = channel.write(new ByteBuffer[]{a, b, c, d}, 1, 2);
      assertEquals("byte written", 2L, written);
    }
    this.assertContents(path, "zbc");
  }

  @Test
  public void lockAsyncChannel() throws IOException, InterruptedException, ExecutionException {
    FileSystem fileSystem = this.rule.getFileSystem();

    Path path = fileSystem.getPath("lock.txt");
    try (AsynchronousFileChannel channel = AsynchronousFileChannel.open(path, StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW)) {
      Future<FileLock> lockFuture = channel.lock();
      FileLock lock = lockFuture.get();
      assertSame(channel, lock.acquiredBy());
    }
  }

  @Test
  public void deleteOnCloseInputStream() throws IOException {
    Path path = this.rule.getFileSystem().getPath("/sample.txt");
    Files.createFile(path);
    assertThat(path, exists());

    try (InputStream inputStream = Files.newInputStream(path)) {
      // nothing
    }
    assertThat(path, exists());

    try (InputStream inputStream = Files.newInputStream(path, StandardOpenOption.DELETE_ON_CLOSE)) {
      // nothing
    }
    assertThat(path, not(exists()));
  }

  @Test
  public void deleteOnCloseOutputStream() throws IOException {
    Path path = this.rule.getFileSystem().getPath("/sample.txt");
    Files.createFile(path);
    assertThat(path, exists());

    try (OutputStream outputStream = Files.newOutputStream(path)) {
      // nothing
    }
    assertThat(path, exists());

    try (OutputStream outputStream = Files.newOutputStream(path, StandardOpenOption.DELETE_ON_CLOSE)) {
      // nothing
    }
    assertThat(path, not(exists()));
  }

  @Test
  public void deleteOnCloseByteChannelStream() throws IOException {
    Path path = this.rule.getFileSystem().getPath("/sample.txt");
    Files.createFile(path);
    assertThat(path, exists());

    try (SeekableByteChannel byteChannel = Files.newByteChannel(path)) {
      // nothing
    }
    assertThat(path, exists());

    try (SeekableByteChannel byteChannel = Files.newByteChannel(path, StandardOpenOption.DELETE_ON_CLOSE)) {
      // nothing
    }
    assertThat(path, not(exists()));
  }

  @Test
  public void probeContent() throws IOException {
    Path path = this.rule.getFileSystem().getPath("/sample.txt");
    Files.createFile(path);
    assertNull(Files.probeContentType(path));
  }

  @Test
  public void trasferFrom() throws IOException {
    FileSystem fileSystem = this.rule.getFileSystem();

    Path from = fileSystem.getPath("from.txt");
    Path to = fileSystem.getPath("to.txt");
    this.writeBigContents(from);

    long expectedSize = SAMPLE_ITERATIONS * SAMPLE_DATA.length;
    assertEquals(expectedSize, Files.size(from));

    try (
            FileChannel fromChannel = FileChannel.open(from, StandardOpenOption.READ);
            FileChannel toChannel = FileChannel.open(to, StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW);) {
      long trasferred = fromChannel.transferTo(0, expectedSize, toChannel);
      assertEquals(expectedSize, trasferred);
    }
    assertEquals(expectedSize, Files.size(to));
  }

  @Test
  public void trasferTo() throws IOException {
    FileSystem fileSystem = this.rule.getFileSystem();

    Path from = fileSystem.getPath("from.txt");
    Path to = fileSystem.getPath("to.txt");
    this.writeBigContents(from);

    long expectedSize = SAMPLE_ITERATIONS * SAMPLE_DATA.length;
    assertEquals(expectedSize, Files.size(from));

    try (
            FileChannel fromChannel = FileChannel.open(from, StandardOpenOption.READ);
            FileChannel toChannel = FileChannel.open(to, StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW);) {
      long trasferred = toChannel.transferFrom(fromChannel, 0, expectedSize);
      assertEquals(expectedSize, trasferred);
    }
    assertEquals(expectedSize, Files.size(to));

  }

  private void writeBigContents(Path path) throws IOException {
    try (SeekableByteChannel channel = Files.newByteChannel(path, StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW)) {
      ByteBuffer src = ByteBuffer.wrap(SAMPLE_DATA);
      for (int i = 0; i < SAMPLE_ITERATIONS; i++) {
        src.rewind();
        channel.write(src);
      }
    }
  }

  @Test
  public void directoryStream() throws IOException {
    FileSystem fileSystem = this.rule.getFileSystem();

    Files.createFile(fileSystem.getPath("a.java"));
    Files.createFile(fileSystem.getPath("a.cpp"));
    Files.createFile(fileSystem.getPath("a.hpp"));
    Files.createFile(fileSystem.getPath("a.c"));
    Files.createFile(fileSystem.getPath("a.h"));

    Files.createDirectory(fileSystem.getPath("d1"));
    Files.createDirectory(fileSystem.getPath("d2"));

    try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(fileSystem.getPath("/"))) {
      List<Path> actual = asList(directoryStream.iterator());
      List<Path> expected = Arrays.asList(
              fileSystem.getPath("/a.java"),
              fileSystem.getPath("/a.cpp"),
              fileSystem.getPath("/a.hpp"),
              fileSystem.getPath("/a.c"),
              fileSystem.getPath("/a.h"),
              fileSystem.getPath("/d1"),
              fileSystem.getPath("/d2"));

      assertEquals(expected.size(), actual.size());

      Set<Path> actualSet = new HashSet<>(actual);
      assertEquals(actualSet.size(), actual.size());
      Set<Path> expectedSet = new HashSet<>(expected);

      assertEquals(expectedSet, actualSet);
    }

    try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(fileSystem.getPath("/"), "*.{java,cpp}")) {
      List<Path> actual = asList(directoryStream.iterator());
      List<Path> expected = Arrays.asList(
              fileSystem.getPath("/a.java"),
              fileSystem.getPath("/a.cpp"));

      assertEquals(expected.size(), actual.size());

      Set<Path> actualSet = new HashSet<>(actual);
      assertEquals(actualSet.size(), actual.size());
      Set<Path> expectedSet = new HashSet<>(expected);

      assertEquals(expectedSet, actualSet);
    }
  }

  static <T> List<T> asList(Iterator<T> iterator) {
    List<T> list = new ArrayList<>();
    while (iterator.hasNext()) {
      list.add(iterator.next());
    }
    return list;
  }

  static <T> List<T> asList(Iterable<T> iterable) {
    return asList(iterable.iterator());
  }

  @Test(expected = IllegalArgumentException.class)
  public void setDirectory() throws IOException {
    FileSystem fileSystem = this.rule.getFileSystem();
    Path path = fileSystem.getPath("/");
    Files.setAttribute(path, "isDirectory", false);
  }


  @Test(expected = FileSystemLoopException.class)
  public void symbolicLinkLoop() throws IOException {
    FileSystem fileSystem = this.rule.getFileSystem();
    Path a = fileSystem.getPath("/a");
    Path b = fileSystem.getPath("/b");
    Files.createSymbolicLink(a, b);
    Files.createSymbolicLink(b, a);
    a.toRealPath();
  }

  @Test(expected = FileSystemException.class)
  public void dontDeleteOpenFile() throws IOException {
    FileSystem fileSystem = this.rule.getFileSystem();
    Path path = fileSystem.getPath("test");
    try (SeekableByteChannel channel = Files.newByteChannel(path, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
      Files.delete(path);
      fail("you shound't be able to delete a file wile it's open");
    }
  }

  @Test
  public void inputStream() throws IOException {
    FileSystem fileSystem = this.rule.getFileSystem();
    Path path = fileSystem.getPath("test");
    try (SeekableByteChannel channel = Files.newByteChannel(path, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
      channel.write(ByteBuffer.wrap(new byte[]{1, 2, 3}));
    }
    try (InputStream input = Files.newInputStream(path)) {
      byte[] data = new byte[5];
      int start = 1;
      int read = 0;
      while ((read = input.read(data, start, 555)) != -1) {
        start += read;
      }
      assertEquals(4, start); // the next read should start at 4
      assertArrayEquals(new byte[]{0, 1, 2, 3, 0},  data);
    }
  }

  @Test
  public void truncateExisting() throws IOException {
    FileSystem fileSystem = this.rule.getFileSystem();
    Path path = fileSystem.getPath("test");
    try (SeekableByteChannel channel = Files.newByteChannel(path, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
      channel.write(ByteBuffer.wrap(new byte[]{1, 2, 3}));
    }
    Object size = Files.getAttribute(path, "size");
    assertEquals(3L, size);

    // TRUNCATE_EXISTING with READ should not truncate
    try (SeekableByteChannel channel = Files.newByteChannel(path, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.READ)) {
      // ignore
    }

    size = Files.getAttribute(path, "size");
    assertEquals(3L, size);

    // WRITE alone should not truncate
    try (SeekableByteChannel channel = Files.newByteChannel(path, StandardOpenOption.WRITE)) {
      // ignore
    }

    size = Files.getAttribute(path, "size");
    assertEquals(3L, size);

    // TRUNCATE_EXISTING with WRITE should truncate
    try (SeekableByteChannel channel = Files.newByteChannel(path, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
      // ignore
    }

    size = Files.getAttribute(path, "size");
    assertEquals(0L, size);
  }

  @Test
  public void position() throws IOException {
    FileSystem fileSystem = this.rule.getFileSystem();
    Path path = Files.createTempFile(fileSystem.getPath("/"), "sample", ".txt");
    try (SeekableByteChannel channel = Files.newByteChannel(path, WRITE)) {
      assertEquals(0L, channel.position());

      channel.position(5L);
      assertEquals(5L, channel.position());
      assertEquals(0, channel.size());

      ByteBuffer src = ByteBuffer.wrap(new byte[]{1, 2, 3, 4, 5});
      assertEquals(5, channel.write(src));

      assertEquals(10L, channel.position());
      assertEquals(10L, channel.size());
    }
  }

  @Test
  public void setPosition() throws IOException {
    FileSystem fileSystem = this.rule.getFileSystem();
    Path path = Files.createTempFile(fileSystem.getPath("/"), "sample", ".txt");
    try (SeekableByteChannel channel = Files.newByteChannel(path, WRITE)) {
      ByteBuffer src = ByteBuffer.wrap(new byte[]{1, 2, 3, 4, 5});
      assertEquals(5, channel.write(src));
    }
    try (SeekableByteChannel channel = Files.newByteChannel(path, READ)) {
      ByteBuffer dst = ByteBuffer.wrap(new byte[5]);
      channel.position(42L);
      assertEquals(-1, channel.read(dst));
    }
  }

  @Test
  public void isRegularFile() throws IOException {
    FileSystem fileSystem = this.rule.getFileSystem();
    Path path = fileSystem.getPath("/");
    Map<String, Object> attributes = Files.readAttributes(path, "isDirectory");
    assertEquals(Collections.singletonMap("isDirectory", true), attributes);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void getPathMatcherUnknown() {
    FileSystem fileSystem = this.rule.getFileSystem();
    fileSystem.getPathMatcher("syntax:patten");
  }

  @Test(expected = IllegalArgumentException.class)
  public void getPathMatcherInvalid1() {
    FileSystem fileSystem = this.rule.getFileSystem();
    fileSystem.getPathMatcher("invalid");
  }

  @Test(expected = IllegalArgumentException.class)
  public void getPathMatcherInvalid2() {
    FileSystem fileSystem = this.rule.getFileSystem();
    fileSystem.getPathMatcher("invalid:");
  }

  @Test
  public void getPathMatcherGlob() {
    FileSystem fileSystem = this.rule.getFileSystem();
    PathMatcher matcher = fileSystem.getPathMatcher("glob:*.java");
    assertTrue(matcher instanceof GlobPathMatcher);
  }

  @Test
  public void getPathMatcherRegex() {
    FileSystem fileSystem = this.rule.getFileSystem();
    PathMatcher matcher = fileSystem.getPathMatcher("regex:.*\\.java");
    assertTrue(matcher instanceof RegexPathMatcher);
  }

  @Test(expected = PatternSyntaxException.class)
  public void getPathMatcherRegexInvalid() {
    FileSystem fileSystem = this.rule.getFileSystem();
    PathMatcher matcher = fileSystem.getPathMatcher("regex:*\\.java");
    assertTrue(matcher instanceof RegexPathMatcher);
  }

  @Test(expected = IllegalArgumentException.class)
  public void emptySubPath() {
    FileSystem fileSystem = this.rule.getFileSystem();
    assertEquals(fileSystem.getPath(""), fileSystem.getPath("").subpath(0, 0));
  }

  @Test
  public void subPath() {
    FileSystem fileSystem = this.rule.getFileSystem();
    assertEquals(fileSystem.getPath("a"), fileSystem.getPath("a").subpath(0, 1));
    assertEquals(fileSystem.getPath("a"), fileSystem.getPath("/a").subpath(0, 1));
    assertEquals(fileSystem.getPath("b"), fileSystem.getPath("/a/b").subpath(1, 2));
    assertEquals(fileSystem.getPath("b"), fileSystem.getPath("a/b").subpath(1, 2));
    assertEquals(fileSystem.getPath("b"), fileSystem.getPath("/a/b/c").subpath(1, 2));
    assertEquals(fileSystem.getPath("b"), fileSystem.getPath("a/b/c").subpath(1, 2));

    assertEquals(fileSystem.getPath("a/b"), fileSystem.getPath("a/b").subpath(0, 2));
    assertEquals(fileSystem.getPath("a/b"), fileSystem.getPath("/a/b").subpath(0, 2));
    assertEquals(fileSystem.getPath("b/c"), fileSystem.getPath("/a/b/c").subpath(1, 3));
    assertEquals(fileSystem.getPath("b/c"), fileSystem.getPath("a/b/c").subpath(1, 3));
    assertEquals(fileSystem.getPath("b/c"), fileSystem.getPath("/a/b/c/d").subpath(1, 3));
    assertEquals(fileSystem.getPath("b/c"), fileSystem.getPath("a/b/c/c").subpath(1, 3));
  }

  @Test
  public void normalizeRoot() {
    FileSystem fileSystem = this.rule.getFileSystem();
    Path root = fileSystem.getPath("/");

    assertEquals(root, root.normalize());
  }

  @Test
  public void normalizeAbsolute() {
    FileSystem fileSystem = this.rule.getFileSystem();

    assertEquals(fileSystem.getPath("/a"), fileSystem.getPath("/a").normalize());
    assertEquals(fileSystem.getPath("/a"), fileSystem.getPath("/a/.").normalize());
    assertEquals(fileSystem.getPath("/a/b/c"), fileSystem.getPath("/a/b/./c").normalize());
    assertEquals(fileSystem.getPath("/a/b/c"), fileSystem.getPath("/a/b/c/.").normalize());
    assertEquals(fileSystem.getPath("/a/b/c"), fileSystem.getPath("/./a/b/c").normalize());
    assertEquals(fileSystem.getPath("/a/b/c"), fileSystem.getPath("/a/./b/c/.").normalize());
    assertEquals(fileSystem.getPath("/a"), fileSystem.getPath("/./a").normalize());
    assertEquals(fileSystem.getPath("/"), fileSystem.getPath("/.").normalize());
    assertEquals(fileSystem.getPath("/a"), fileSystem.getPath("/a/.").normalize());
    assertEquals(fileSystem.getPath("/"), fileSystem.getPath("/a/..").normalize());
    assertEquals(fileSystem.getPath("/a"), fileSystem.getPath("/a/b/..").normalize());
    assertEquals(fileSystem.getPath("/a/c"), fileSystem.getPath("/a/b/../c").normalize());
    assertEquals(fileSystem.getPath("/"), fileSystem.getPath("/a/../..").normalize());
    assertEquals(fileSystem.getPath("/"), fileSystem.getPath("/..").normalize());
    assertEquals(fileSystem.getPath("/"), fileSystem.getPath("/../..").normalize());
    assertEquals(fileSystem.getPath("/a/b"), fileSystem.getPath("/../a/b").normalize());
    assertEquals(fileSystem.getPath("/a/b"), fileSystem.getPath("/../../a/b").normalize());
    assertEquals(fileSystem.getPath("/c"), fileSystem.getPath("/a/b/../../c").normalize());
  }

  @Test
  public void normalizeRelative() {
    FileSystem fileSystem = this.rule.getFileSystem();

    assertEquals(fileSystem.getPath("a"), fileSystem.getPath("a").normalize());
    assertEquals(fileSystem.getPath("a"), fileSystem.getPath("a/.").normalize());
    assertEquals(fileSystem.getPath(""), fileSystem.getPath("a/..").normalize());
    assertEquals(fileSystem.getPath(".."), fileSystem.getPath("..").normalize());
    assertEquals(fileSystem.getPath("../.."), fileSystem.getPath("../..").normalize());
    assertEquals(fileSystem.getPath("../.."), fileSystem.getPath(".././..").normalize());
    assertEquals(fileSystem.getPath("../../a/b/c"), fileSystem.getPath("../../a/b/c").normalize());
    assertEquals(fileSystem.getPath("../../a/b"), fileSystem.getPath("../../a/b/c/..").normalize());
    assertEquals(fileSystem.getPath("../../a/b"), fileSystem.getPath("../../a/b/c/./..").normalize());
  }

  @Test
  public void resolveRoot() {
    FileSystem fileSystem = this.rule.getFileSystem();

    assertEquals(fileSystem.getPath("/"), fileSystem.getPath("/a").resolve(fileSystem.getPath("/")));
    assertEquals(fileSystem.getPath("/"), fileSystem.getPath("a").resolve(fileSystem.getPath("/")));
  }

  @Test
  public void resolveAbsoluteOtherAbsolute() {
    FileSystem fileSystem = this.rule.getFileSystem();
    Path absolute = fileSystem.getPath("/a/b");

    assertEquals(absolute, fileSystem.getPath("").resolve(absolute));
    assertEquals(absolute, fileSystem.getPath("/").resolve(absolute));
    assertEquals(absolute, fileSystem.getPath("c/d").resolve(absolute));
    assertEquals(absolute, fileSystem.getPath("/c/d").resolve(absolute));
  }

  @Test
  public void resolveAbsoluteOtherRelative() {
    FileSystem fileSystem = this.rule.getFileSystem();
    Path realtive = fileSystem.getPath("a/b");

    assertEquals(fileSystem.getPath("a/b"), fileSystem.getPath("").resolve(realtive));
    assertEquals(fileSystem.getPath("/a/b"), fileSystem.getPath("/").resolve(realtive));
    assertEquals(fileSystem.getPath("c/d/a/b"), fileSystem.getPath("c/d").resolve(realtive));
    assertEquals(fileSystem.getPath("/c/d/a/b"), fileSystem.getPath("/c/d").resolve(realtive));
  }


  @Test
  public void resolveAbsoluteOtherAbsoluteString() {
    FileSystem fileSystem = this.rule.getFileSystem();
    String absolute = "/a/b";
    Path absolutePath = fileSystem.getPath("/a/b");

    assertEquals(absolutePath, fileSystem.getPath("").resolve(absolute));
    assertEquals(absolutePath, fileSystem.getPath("/").resolve(absolute));
    assertEquals(absolutePath, fileSystem.getPath("c/d").resolve(absolute));
    assertEquals(absolutePath, fileSystem.getPath("/c/d").resolve(absolute));
  }

  @Test
  public void resolveAbsoluteOtherRelativeString() {
    FileSystem fileSystem = this.rule.getFileSystem();
    String realtive = "a/b";

    assertEquals(fileSystem.getPath("a/b"), fileSystem.getPath("").resolve(realtive));
    assertEquals(fileSystem.getPath("/a/b"), fileSystem.getPath("/").resolve(realtive));
    assertEquals(fileSystem.getPath("c/d/a/b"), fileSystem.getPath("c/d").resolve(realtive));
    assertEquals(fileSystem.getPath("/c/d/a/b"), fileSystem.getPath("/c/d").resolve(realtive));
  }

  @Test
  public void resolveSibling() {
    FileSystem fileSystem = this.rule.getFileSystem();
    assertEquals(fileSystem.getPath("b"), fileSystem.getPath("a").resolveSibling(fileSystem.getPath("b")));

    // argument is relative
    assertEquals(fileSystem.getPath("/a/c/d"), fileSystem.getPath("/a/b").resolveSibling(fileSystem.getPath("c/d")));
    assertEquals(fileSystem.getPath("a/c/d"), fileSystem.getPath("a/b").resolveSibling(fileSystem.getPath("c/d")));
    assertEquals(fileSystem.getPath("/c/d"), fileSystem.getPath("/a").resolveSibling(fileSystem.getPath("c/d")));
    assertEquals(fileSystem.getPath("c/d"), fileSystem.getPath("a").resolveSibling(fileSystem.getPath("c/d")));

    // argument is absolute
    assertEquals(fileSystem.getPath("/c/d"), fileSystem.getPath("/a/b").resolveSibling(fileSystem.getPath("/c/d")));
    assertEquals(fileSystem.getPath("/c/d"), fileSystem.getPath("a/b").resolveSibling(fileSystem.getPath("/c/d")));
    assertEquals(fileSystem.getPath("/c/d"), fileSystem.getPath("/a").resolveSibling(fileSystem.getPath("/c/d")));
    assertEquals(fileSystem.getPath("/c/d"), fileSystem.getPath("a").resolveSibling(fileSystem.getPath("/c/d")));
    assertEquals(fileSystem.getPath("/a/b"), fileSystem.getPath("").resolveSibling(fileSystem.getPath("/a/b")));

    // argument is empty
    assertEquals(fileSystem.getPath(""), fileSystem.getPath("a").resolveSibling(fileSystem.getPath("")));
    assertEquals(fileSystem.getPath("/a"), fileSystem.getPath("/a/b").resolveSibling(fileSystem.getPath("")));
    assertEquals(fileSystem.getPath("a"), fileSystem.getPath("a/b").resolveSibling(fileSystem.getPath("")));
    assertEquals(fileSystem.getPath("/"), fileSystem.getPath("/a").resolveSibling(fileSystem.getPath("")));
    assertEquals(fileSystem.getPath(""), fileSystem.getPath("a").resolveSibling(fileSystem.getPath("")));

    // receiver is empty
    assertEquals(fileSystem.getPath("a/b"), fileSystem.getPath("").resolveSibling(fileSystem.getPath("a/b")));
    assertEquals(fileSystem.getPath("/a/b"), fileSystem.getPath("").resolveSibling(fileSystem.getPath("/a/b")));

    // argument is root
    assertEquals(fileSystem.getPath("/"), fileSystem.getPath("a/b").resolveSibling(fileSystem.getPath("/")));
    assertEquals(fileSystem.getPath("/"), fileSystem.getPath("/a/b").resolveSibling(fileSystem.getPath("/")));
    assertEquals(fileSystem.getPath("/"), fileSystem.getPath("a").resolveSibling(fileSystem.getPath("/")));
    assertEquals(fileSystem.getPath("/"), fileSystem.getPath("/a").resolveSibling(fileSystem.getPath("/")));
    assertEquals(fileSystem.getPath("/"), fileSystem.getPath("").resolveSibling(fileSystem.getPath("/")));
  }


  @Test
  public void resolveSiblingAgainstRoot() {
    FileSystem fileSystem = this.rule.getFileSystem();
    Path root = fileSystem.getPath("/");

    assertEquals(fileSystem.getPath("a"), root.resolveSibling(fileSystem.getPath("a")));
    assertEquals(fileSystem.getPath("a/b"), root.resolveSibling(fileSystem.getPath("a/b")));
    assertEquals(fileSystem.getPath("/a"), root.resolveSibling(fileSystem.getPath("/a")));
    assertEquals(fileSystem.getPath("/a/b"), root.resolveSibling(fileSystem.getPath("/a/b")));
    assertEquals(fileSystem.getPath(""), root.resolveSibling(fileSystem.getPath("")));
  }

  @Test
  public void resolveSiblingAgainstRootString() {
    FileSystem fileSystem = this.rule.getFileSystem();
    Path root = fileSystem.getPath("/");

    assertEquals(fileSystem.getPath("a"), root.resolveSibling("a"));
    assertEquals(fileSystem.getPath("a/b"), root.resolveSibling("a/b"));
    assertEquals(fileSystem.getPath("/a"), root.resolveSibling("/a"));
    assertEquals(fileSystem.getPath("/a/b"), root.resolveSibling("/a/b"));
    assertEquals(fileSystem.getPath(""), root.resolveSibling(""));
  }


  @Test
  public void relativizeAbsolute() {
    FileSystem fileSystem = this.rule.getFileSystem();
    Path first = fileSystem.getPath("/a/b");
    Path second = fileSystem.getPath("/a/b/c");

    assertEquals(fileSystem.getPath("c"), first.relativize(second));
    assertEquals(fileSystem.getPath(".."), second.relativize(first));
    assertEquals(fileSystem.getPath(""), first.relativize(first));

    // ---

    first = fileSystem.getPath("/a/b");
    second = fileSystem.getPath("/a/b/c/d");

    assertEquals(fileSystem.getPath("c/d"), first.relativize(second));
    assertEquals(fileSystem.getPath("../.."), second.relativize(first));
    assertEquals(fileSystem.getPath(""), first.relativize(first));

    // ---

    first = fileSystem.getPath("/a/b");
    second = fileSystem.getPath("/c");

    assertEquals(fileSystem.getPath("../../c"), first.relativize(second));
    assertEquals(fileSystem.getPath("../a/b"), second.relativize(first));
    assertEquals(fileSystem.getPath(""), first.relativize(first));

    // ---

    first = fileSystem.getPath("/a/b");
    second = fileSystem.getPath("/c/d");

    assertEquals(fileSystem.getPath("../../c/d"), first.relativize(second));
    assertEquals(fileSystem.getPath("../../a/b"), second.relativize(first));
    assertEquals(fileSystem.getPath(""), first.relativize(first));
  }

  @Test(expected = IllegalArgumentException.class)
  public void relativizeAbsoluteUnsupported1() {
    FileSystem fileSystem = this.rule.getFileSystem();
    Path first = fileSystem.getPath("/a/b");
    Path second = fileSystem.getPath("c");
    first.relativize(second);
  }

  @Test(expected = IllegalArgumentException.class)
  public void relativizeAbsoluteUnsupported2() {
    FileSystem fileSystem = this.rule.getFileSystem();
    Path first = fileSystem.getPath("/a/b");
    Path second = fileSystem.getPath("c");
    second.relativize(first);
  }


  @Test
  public void relativizeRelative() {
    FileSystem fileSystem = this.rule.getFileSystem();
    Path first = fileSystem.getPath("a/b");
    Path second = fileSystem.getPath("a/b/c");

    assertEquals(fileSystem.getPath("c"), first.relativize(second));
    assertEquals(fileSystem.getPath(".."), second.relativize(first));
    assertEquals(fileSystem.getPath(""), first.relativize(first));

    // ---

    first = fileSystem.getPath("a/b");
    second = fileSystem.getPath("a/b/c/d");

    assertEquals(fileSystem.getPath("c/d"), first.relativize(second));
    assertEquals(fileSystem.getPath("../.."), second.relativize(first));
    assertEquals(fileSystem.getPath(""), first.relativize(first));

    // ---

    first = fileSystem.getPath("a/b");
    second = fileSystem.getPath("c");

    assertEquals(fileSystem.getPath("../../c"), first.relativize(second));
    assertEquals(fileSystem.getPath("../a/b"), second.relativize(first));
    assertEquals(fileSystem.getPath(""), first.relativize(first));

    // ---

    first = fileSystem.getPath("a/b");
    second = fileSystem.getPath("c/d");

    assertEquals(fileSystem.getPath("../../c/d"), first.relativize(second));
    assertEquals(fileSystem.getPath("../../a/b"), second.relativize(first));
    assertEquals(fileSystem.getPath(""), first.relativize(first));
  }


  @Test
  public void relativizeRelativeRoot() {
    FileSystem fileSystem = this.rule.getFileSystem();
    Path first = fileSystem.getPath("/");
    Path second = fileSystem.getPath("/a/b");

    assertEquals(fileSystem.getPath("a/b"), first.relativize(second));
    assertEquals(fileSystem.getPath("../.."), second.relativize(first));
    assertEquals(fileSystem.getPath(""), first.relativize(first));
  }

  @Test
  public void absoluteIterator() {
    FileSystem fileSystem = this.rule.getFileSystem();
    Path usrBin = fileSystem.getPath("/usr/bin");
    Iterable<String> expected = Arrays.asList("usr", "bin");
    this.assertIterator(fileSystem, usrBin, expected);
  }

  @Test
  public void relativeIterator() {
    FileSystem fileSystem = this.rule.getFileSystem();
    Path usrBin = fileSystem.getPath("usr/bin");
    Iterable<String> expected = Arrays.asList("usr", "bin");
    this.assertIterator(fileSystem, usrBin, expected);
  }

  private void assertIterator(FileSystem fileSystem, Path path, Iterable<String> expected) {
    Iterator<Path> actualIterator = path.iterator();
    Iterator<String> expectedIterator = expected.iterator();
    while (actualIterator.hasNext()) {
      Path actualPath = actualIterator.next();
      try {
        actualIterator.remove();
        fail("path iterator should not support #remove()");
      } catch (UnsupportedOperationException e) {
        assertTrue("path iterator #remove() should throw UnsupportedOperationException", true);
      }

      assertTrue(expectedIterator.hasNext());
      String expectedName = expectedIterator.next();
      Path expectedPath = fileSystem.getPath(expectedName);

      assertEquals(expectedPath, actualPath);
      assertFalse(actualPath.isAbsolute());
    }
    assertFalse(expectedIterator.hasNext());
  }

  @Test
  public void endsWith() {
    FileSystem fileSystem = this.rule.getFileSystem();

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
  public void startsWith() {
    FileSystem fileSystem = this.rule.getFileSystem();

    assertTrue(fileSystem.getPath("a").startsWith(fileSystem.getPath("a")));
    assertFalse(fileSystem.getPath("a").startsWith(fileSystem.getPath("a/b")));
    assertFalse(fileSystem.getPath("a").startsWith(fileSystem.getPath("/a")));
    assertFalse(fileSystem.getPath("a").startsWith(fileSystem.getPath("/")));
    assertFalse(fileSystem.getPath("a").startsWith(fileSystem.getPath("")));
    assertTrue(fileSystem.getPath("a/b").startsWith(fileSystem.getPath("a")));
    assertTrue(fileSystem.getPath("a/b").startsWith(fileSystem.getPath("a/b")));
    assertFalse(fileSystem.getPath("a/b").startsWith(fileSystem.getPath("/a")));
    assertFalse(fileSystem.getPath("a/b").startsWith(fileSystem.getPath("/")));
    assertFalse(fileSystem.getPath("a/b").startsWith(fileSystem.getPath("")));

    assertTrue(fileSystem.getPath("/a").startsWith(fileSystem.getPath("/a")));
    assertFalse(fileSystem.getPath("/a").startsWith(fileSystem.getPath("/a/b")));
    assertFalse(fileSystem.getPath("/a").startsWith(fileSystem.getPath("a")));
    assertTrue(fileSystem.getPath("/a").startsWith(fileSystem.getPath("/")));
    assertFalse(fileSystem.getPath("/a").startsWith(fileSystem.getPath("")));
    assertTrue(fileSystem.getPath("/a/b").startsWith(fileSystem.getPath("/a")));
    assertTrue(fileSystem.getPath("/a/b").startsWith(fileSystem.getPath("/a/b")));
    assertFalse(fileSystem.getPath("/a/b").startsWith(fileSystem.getPath("/a/b/c")));
    assertFalse(fileSystem.getPath("/a/b").startsWith(fileSystem.getPath("a")));
    assertTrue(fileSystem.getPath("/a/b").startsWith(fileSystem.getPath("/")));
    assertFalse(fileSystem.getPath("/a/b").startsWith(fileSystem.getPath("")));

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
  public void getFileName() {
    FileSystem fileSystem = this.rule.getFileSystem();
    Path usrBin = fileSystem.getPath("/usr/bin");
    Path bin = fileSystem.getPath("bin");

    Path fileName = usrBin.getFileName();
    assertNotNull(fileName);

    assertEquals(fileName, bin);
    assertFalse(fileName.isAbsolute());
  }

  @Test
  public void absoluteGetParent() {
    FileSystem fileSystem = this.rule.getFileSystem();
    Path usrBin = fileSystem.getPath("/usr/bin");
    Path usr = fileSystem.getPath("/usr");

    assertEquals(usr, usrBin.getParent());
    assertTrue(usrBin.getParent().isAbsolute());
    Path root = fileSystem.getRootDirectories().iterator().next();
    assertEquals(root, usr.getParent());
    assertTrue(usr.getParent().isAbsolute());
  }

  @Test
  public void relativeGetParent() {
    FileSystem fileSystem = this.rule.getFileSystem();
    Path usrBin = fileSystem.getPath("usr/bin");
    Path usr = fileSystem.getPath("usr");

    assertEquals(usr, usrBin.getParent());
    assertFalse(usrBin.getParent().isAbsolute());
    assertNull(usr.getParent());
  }

  @Test(expected = IllegalArgumentException.class)
  public void absoluteGetName0() {
    FileSystem fileSystem = this.rule.getFileSystem();
    Path usrBin = fileSystem.getPath("/usr/bin");
    usrBin.getName(-1);
  }

  @Test(expected = IllegalArgumentException.class)
  public void absoluteGetNameToLong() {
    FileSystem fileSystem = this.rule.getFileSystem();
    Path usrBin = fileSystem.getPath("/usr/bin");
    usrBin.getName(2);
  }

  @Test(expected = IllegalArgumentException.class)
  public void emptyGetName() {
    FileSystem fileSystem = this.rule.getFileSystem();
    Path empty = fileSystem.getPath("");
    empty.getName(0);
  }

  @Test
  public void absoluteGetName() {
    FileSystem fileSystem = this.rule.getFileSystem();
    Path usrBin = fileSystem.getPath("/usr/bin");
    Path usr = fileSystem.getPath("usr");
    assertEquals(usr, usrBin.getName(0));
    Path bin = fileSystem.getPath("bin");
    assertEquals(bin, usrBin.getName(1));
  }

  @Test
  public void relativeGetName() {
    FileSystem fileSystem = this.rule.getFileSystem();
    Path usrBin = fileSystem.getPath("usr/bin");
    Path usr = fileSystem.getPath("usr");
    assertEquals(usr, usrBin.getName(0));
    Path bin = fileSystem.getPath("bin");
    assertEquals(bin, usrBin.getName(1));
  }

  @Test(expected = IllegalArgumentException.class)
  public void relativeGetName0() {
    FileSystem fileSystem = this.rule.getFileSystem();
    Path usrBin = fileSystem.getPath("usr/bin");
    usrBin.getName(-1);
  }

  @Test(expected = IllegalArgumentException.class)
  public void relativeGetNameToLong() {
    FileSystem fileSystem = this.rule.getFileSystem();
    Path usrBin = fileSystem.getPath("usr/bin");
    usrBin.getName(2);
  }

  @Test
  public void emptyPath() {
    FileSystem fileSystem = this.rule.getFileSystem();
    Path path = fileSystem.getPath("");
    assertFalse(path.isAbsolute());
    assertNull(path.getRoot());
  }

  @Test
  public void getNameCount() {
    FileSystem fileSystem = this.rule.getFileSystem();
    Path usrBin = fileSystem.getPath("/usr/bin");
    assertEquals(2, usrBin.getNameCount());

    usrBin = fileSystem.getPath("usr/bin");
    assertEquals(2, usrBin.getNameCount());
  }

  @Test
  public void isReadOnly() {
    FileSystem fileSystem = this.rule.getFileSystem();
    assertFalse(fileSystem.isReadOnly());
  }

  @Test
  public void absolutePaths() {
    FileSystem fileSystem = this.rule.getFileSystem();
    Path path = fileSystem.getPath("/");
    assertTrue(path.isAbsolute());
    assertSame(path, path.toAbsolutePath());

    path = fileSystem.getPath("/", "sample");
    assertTrue(path.isAbsolute());
    assertSame(path, path.toAbsolutePath());
    assertNotNull(path.getRoot());
    assertSame(this.getRoot(fileSystem), path.getRoot());
  }

  @Test
  public void relativePaths() {
    FileSystem fileSystem = this.rule.getFileSystem();
    Path path = fileSystem.getPath("sample");
    assertFalse(path.isAbsolute());
    assertNull(path.getRoot());
  }

  private Path getRoot(FileSystem fileSystem) {
    Iterable<Path> rootDirectories = fileSystem.getRootDirectories();
    Iterator<Path> iterator = rootDirectories.iterator();
    Path root = iterator.next();
    assertFalse(iterator.hasNext());
    return root;
  }

  @Test
  public void supportedFileAttributeViews() {
    FileSystem fileSystem = this.rule.getFileSystem();
    assertEquals(Collections.singleton(FileAttributeViews.BASIC), fileSystem.supportedFileAttributeViews());
  }

  @Test
  public void pathToString() {
    FileSystem fileSystem = this.rule.getFileSystem();
    Path path = fileSystem.getPath("/");
    assertEquals("/", path.toString());

    path = fileSystem.getPath("/home");
    assertEquals("/home", path.toString());

    path = fileSystem.getPath("/home/pmarscha");
    assertEquals("/home/pmarscha", path.toString());

    path = fileSystem.getPath("home");
    assertEquals("home", path.toString());

    path = fileSystem.getPath("home/pmarscha");
    assertEquals("home/pmarscha", path.toString());

    path = fileSystem.getPath("home/./../pmarscha");
    assertEquals("home/./../pmarscha", path.toString());
  }

  @Test
  public void defaultSeparator() {
    FileSystem fileSystem = this.rule.getFileSystem();
    assertEquals("/", fileSystem.getSeparator());
  }


  @Test(expected = IllegalArgumentException.class)
  public void slash() {
    FileSystem fileSystem = this.rule.getFileSystem();
    Path path = fileSystem.getPath("/");
    path.subpath(0, 1);
  }

  @Test(expected = IOException.class)
  public void createDirectoryNoParent() throws IOException {
    FileSystem fileSystem = this.rule.getFileSystem();
    Path homePmarscha = fileSystem.getPath("/home/pmarscha");
    assertThat(homePmarscha, not(exists()));
    Files.createDirectory(homePmarscha);
    assertThat(homePmarscha,exists());
  }

  @Test
  public void pathOrdering() {
    FileSystem fileSystem = this.rule.getFileSystem();
    Path root = fileSystem.getPath("/");
    Path empty = fileSystem.getPath("");
    Path a = fileSystem.getPath("a");
    Path slashA = fileSystem.getPath("/a");
    Path slashAA = fileSystem.getPath("/a/a");

    assertThat(empty, lessThan(a));
    assertThat(a, greaterThan(empty));

    assertThat(root, lessThan(empty));
    assertThat(root, lessThan(a));
    assertThat(root, lessThan(slashA));
    assertThat(root, lessThan(slashAA));
    assertThat(empty, greaterThan(root));
    assertThat(a, greaterThan(root));
    assertThat(slashA, greaterThan(root));
    assertThat(slashAA, greaterThan(root));

    assertEquals(0, root.compareTo(root));
    assertEquals(0, empty.compareTo(empty));
    assertEquals(0, a.compareTo(a));
    assertEquals(0, slashA.compareTo(slashA));
    assertEquals(0, slashA.compareTo(slashA));

    assertThat(a, greaterThan(slashA));
    assertThat(a, greaterThan(slashAA));
    assertThat(slashA, lessThan(a));
    assertThat(slashAA, lessThan(a));

    assertThat(slashA, lessThan(slashAA));
    assertThat(slashAA, greaterThan(slashA));
  }

  @Test
  public void xattrs() throws IOException {
    MemoryFileSystemBuilder builder = MemoryFileSystemBuilder.newEmpty().addFileAttributeView(UserDefinedFileAttributeView.class);
    try (FileSystem fileSystem = builder.build("xtattry")) {
      assertTrue(fileSystem.supportedFileAttributeViews().contains("user"));

      Path path = fileSystem.getPath("meta");
      Files.createFile(path);
      UserDefinedFileAttributeView userAttributes = Files.getFileAttributeView(path, UserDefinedFileAttributeView.class);
      assertThat(userAttributes.list(), empty());

      userAttributes.write("meta-key", ByteBuffer.wrap(new byte[]{1, 2, 3}));
      assertEquals(Collections.singletonList("meta-key"), userAttributes.list());
      assertEquals(3, userAttributes.size("meta-key"));

      byte[] readBack = new byte[5];
      ByteBuffer buffer = ByteBuffer.wrap(readBack);
      assertEquals(3, userAttributes.read("meta-key", buffer));

      assertArrayEquals(new byte[]{1, 2, 3, 0, 0}, readBack);


      userAttributes.delete("meta-key");
      assertThat(userAttributes.list(), empty());

      try {
        userAttributes.read("wrong-key", buffer);
        fail("there should be nothing under \"wrong-key\"");
      } catch (IOException e) {
        // should reach here
      }
    }
  }

  @Test
  public void pathOrderingDifferentFileSystem() throws IOException {
    FileSystem fileSystem1 = this.rule.getFileSystem();
    try (FileSystem fileSystem2 = FileSystems.newFileSystem(URI.create("memory:name1"), SAMPLE_ENV)) {
      Path root1 = fileSystem1.getPath("/");
      Path root2 = fileSystem2.getPath("/");

      assertThat(root1, not(equalTo(root2)));
      assertThat(root2, not(equalTo(root1)));
      assertThat(root1, lessThan(root2));
      assertThat(root2, greaterThan(root1));

      Path empty1 = fileSystem1.getPath("");
      Path empty2 = fileSystem2.getPath("");

      assertThat(empty1, not(equalTo(empty2)));
      assertThat(empty2, not(equalTo(empty1)));
      assertThat(empty1, lessThan(empty2));
      assertThat(empty2, greaterThan(empty1));

      Path realtive1 = fileSystem1.getPath("a");
      Path realtive2 = fileSystem2.getPath("a");

      assertThat(realtive1, not(equalTo(realtive2)));
      assertThat(realtive2, not(equalTo(realtive1)));
      assertThat(realtive1, lessThan(realtive2));
      assertThat(realtive2, greaterThan(realtive1));

      Path absolute1 = fileSystem1.getPath("/a");
      Path absolute2 = fileSystem2.getPath("/a");

      assertThat(absolute1, not(equalTo(absolute2)));
      assertThat(absolute2, not(equalTo(absolute1)));
      assertThat(absolute1, lessThan(absolute2));
      assertThat(absolute2, greaterThan(absolute1));
    }

  }

  @Test(expected = ClassCastException.class)
  public void pathOrderingIncompatible() {
    FileSystem fileSystem = this.rule.getFileSystem();
    Path a = fileSystem.getPath("a");
    Path b = FileSystems.getDefault().getPath("b");
    a.compareTo(b);
  }


  @Test
  public void createDirectories() throws IOException {
    FileSystem fileSystem = this.rule.getFileSystem();
    Path homePmarscha = fileSystem.getPath("/home/pmarscha");
    assertThat(homePmarscha, not(exists()));
    Files.createDirectories(homePmarscha);
    assertThat(homePmarscha, exists());
  }


  @Test
  public void createDirectory() throws IOException {
    FileSystem fileSystem = this.rule.getFileSystem();
    Path home = fileSystem.getPath("/home");
    assertThat(home, not(exists()));
    Files.createDirectory(home);
    assertThat(home, exists());
    assertTrue(Files.isDirectory(home));
    assertFalse(Files.isRegularFile(home));
  }

  @Test(expected = FileAlreadyExistsException.class)
  public void createDirectoryAlreadyExists() throws IOException {
    FileSystem fileSystem = this.rule.getFileSystem();
    Path home = fileSystem.getPath("/home");
    assertThat(home, not(exists()));
    Files.createDirectory(home);
    assertThat(home, exists());
    Files.createDirectory(home);
  }

  @Test
  public void getRootDirectories() {
    FileSystem fileSystem = this.rule.getFileSystem();
    Iterator<Path> directories = fileSystem.getRootDirectories().iterator();
    assertTrue(directories.hasNext());
    directories.next();
    try {
      directories.remove();
      fail("root directories iterator should not support remove");
    } catch (UnsupportedOperationException e) {
      // should reach here
    }
    assertFalse(directories.hasNext());
  }

  @Test
  public void copySameFile() throws IOException {
    FileSystem fileSystem = this.rule.getFileSystem();

    Path a = fileSystem.getPath("a");
    Path b = fileSystem.getPath("./b/../a");

    this.createAndSetContents(a, "aaa");
    assertThat(a, exists());
    assertThat(b, exists());

    Files.copy(a, b);
    assertThat(a, exists());
    assertThat(b, exists());

    this.assertContents(a, "aaa");
    this.assertContents(b, "aaa");
  }

  @Test
  public void moveSameFile() throws IOException {
    FileSystem fileSystem = this.rule.getFileSystem();

    Path a = fileSystem.getPath("a");
    Path b = fileSystem.getPath("./b/../a");

    this.createAndSetContents(a, "aaa");
    assertThat(a, exists());
    assertThat(b, exists());

    Files.move(a, b);
    assertThat(a, exists());
    assertThat(b, exists());

    this.assertContents(a, "aaa");
    this.assertContents(b, "aaa");
  }

  @Test
  public void copyNoExisitingNoAttributes() throws IOException {
    FileSystem fileSystem = this.rule.getFileSystem();
    Path a = fileSystem.getPath("/1/a");
    Path b = fileSystem.getPath("/2/b");
    Files.createDirectories(b.toAbsolutePath().getParent());

    this.createAndSetContents(a, "aaa");
    assertThat(a, exists());
    assertThat(b, not(exists()));

    Files.copy(a, b);
    assertThat(a, exists());
    assertThat(b, exists());

    this.assertContents(a, "aaa");
    this.assertContents(b, "aaa");

    this.setContents(a, "a1");

    this.assertContents(a, "a1");
    this.assertContents(b, "aaa");
  }

  @Test
  public void copyAcrossFileSystems() throws IOException {
    FileSystem source = this.rule.getFileSystem();
    try (FileSystem target = MemoryFileSystemBuilder.newEmpty().build("target")) {
      Path a = source.getPath("a");
      Path b = target.getPath("b");

      this.createAndSetContents(a, "aaa");
      assertThat(a, exists());
      assertThat(b, not(exists()));

      Files.copy(a, b);
      assertThat(a, exists());
      assertThat(b, exists());

      this.assertContents(a, "aaa");
      this.assertContents(b, "aaa");

      this.setContents(a, "a1");

      this.assertContents(a, "a1");
      this.assertContents(b, "aaa");
    }
  }

  @Test
  public void copyReplaceExisitingNoAttributes() throws IOException {
    FileSystem fileSystem = this.rule.getFileSystem();
    Path a = fileSystem.getPath("/1/a");
    Path b = fileSystem.getPath("/2/b");

    this.createAndSetContents(a, "aaa");
    this.createAndSetContents(b, "bbb");
    assertThat(a, exists());
    assertThat(b, exists());

    Files.copy(a, b, StandardCopyOption.REPLACE_EXISTING);
    assertThat(a, exists());
    assertThat(b, exists());

    this.assertContents(a, "aaa");
    this.assertContents(b, "aaa");

    this.setContents(a, "a1");

    this.assertContents(a, "a1");
    this.assertContents(b, "aaa");
  }

  @Test
  public void moveNoExisitingNoAttributes() throws IOException {
    FileSystem fileSystem = this.rule.getFileSystem();
    Path a = fileSystem.getPath("/1/a");
    Path b = fileSystem.getPath("/2/b");
    Files.createDirectories(b.toAbsolutePath().getParent());

    this.createAndSetContents(a, "aaa");
    assertThat(a, exists());
    assertThat(b, not(exists()));

    Files.move(a, b);
    assertThat(a, not(exists()));
    assertThat(b, exists());

    this.assertContents(b, "aaa");
  }

  @Test
  public void moveDifferentFileSystem() throws IOException {
    FileSystem source = this.rule.getFileSystem();
    try (FileSystem target = MemoryFileSystemBuilder.newEmpty().build("target")) {
      Path a = source.getPath("a");
      Path b = target.getPath("b");

      this.createAndSetContents(a, "aaa");
      assertThat(a, exists());
      assertThat(b, not(exists()));

      Files.move(a, b);
      assertThat(a, not(exists()));
      assertThat(b, exists());

      this.assertContents(b, "aaa");
    }
  }

  @Test
  public void moveReplaceExisitingNoAttributes() throws IOException {
    FileSystem fileSystem = this.rule.getFileSystem();
    Path a = fileSystem.getPath("/1/a");
    Path b = fileSystem.getPath("/2/b");

    this.createAndSetContents(a, "aaa");
    this.createAndSetContents(b, "bbb");
    assertThat(a, exists());
    assertThat(b, exists());

    Files.move(a, b, StandardCopyOption.REPLACE_EXISTING);
    assertThat(a, not(exists()));
    assertThat(b, exists());

    this.assertContents(b, "aaa");
  }

  private void createAndSetContents(Path path, String contents) throws IOException {
    Path parent = path.toAbsolutePath().getParent();
    if (parent.getParent() != null) { // check for root
      Files.createDirectories(parent);
    }
    try (SeekableByteChannel channel = Files.newByteChannel(path, WRITE, CREATE_NEW)) {
      channel.write(ByteBuffer.wrap(contents.getBytes(US_ASCII)));
    }
  }

  private void setContents(Path path, String contents) throws IOException {
    try (SeekableByteChannel channel = Files.newByteChannel(path, WRITE, TRUNCATE_EXISTING)) {
      channel.write(ByteBuffer.wrap(contents.getBytes(US_ASCII)));
    }
  }

  private void assertContents(Path path, String expected) throws IOException {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream(expected.length());
    try (InputStream input = Files.newInputStream(path, READ)) {
      int read;
      byte[] buffer = new byte[512];
      while ((read = input.read(buffer)) != -1) {
        outputStream.write(buffer, 0, read);
      }
    }
    assertEquals(expected, new String(outputStream.toByteArray(), US_ASCII));
  }

}
