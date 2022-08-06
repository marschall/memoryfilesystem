package com.github.marschall.memoryfilesystem;

import static com.github.marschall.memoryfilesystem.Constants.SAMPLE_ENV;
import static com.github.marschall.memoryfilesystem.FileContentsMatcher.hasContents;
import static com.github.marschall.memoryfilesystem.FileExistsMatcher.exists;
import static com.github.marschall.memoryfilesystem.FileUtility.setContents;
import static com.github.marschall.memoryfilesystem.IsAbsoluteMatcher.isAbsolute;
import static com.github.marschall.memoryfilesystem.IsAbsoluteMatcher.isRelative;
import static com.github.marschall.memoryfilesystem.IsSameFileMatcher.isSameFile;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE_NEW;
import static java.nio.file.StandardOpenOption.DELETE_ON_CLOSE;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.isA;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.OverlappingFileLockException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemException;
import java.nio.file.FileSystemLoopException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotLinkException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.ProviderMismatchException;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.UserDefinedFileAttributeView;
import java.text.ParseException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class MemoryFileSystemTest {

  // spawn more tree blocks
  private static final int SAMPLE_ITERATIONS = 1000;

  private static final byte[] SAMPLE_DATA = new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9};

  @RegisterExtension
  final FileSystemExtension extension = new FileSystemExtension();

  @Test
  void tryLockNoArguments() throws IOException {
    FileSystem fileSystem = this.extension.getFileSystem();

    Path path = fileSystem.getPath("file.txt");
    Files.createFile(path);
    setContents(path, "0123456789");

    try (FileChannel channel = FileChannel.open(path, WRITE)) {
      FileLock firstLock = channel.tryLock();
      assertNotNull(firstLock);
      assertTrue(firstLock.isValid(), "valid");
      assertFalse(firstLock.isShared(), "shared");

      assertNull(channel.tryLock());

      assertSame(channel, firstLock.acquiredBy());
      assertSame(channel, firstLock.channel());

      firstLock.release();
      assertFalse(firstLock.isValid(), "valid");

      FileLock secondLock = channel.tryLock();
      assertNotNull(secondLock);
    }

  }

  @Test
  void overLappingLocking() throws IOException {
    FileSystem fileSystem = this.extension.getFileSystem();

    Path path = fileSystem.getPath("file.txt");
    FileUtility.createAndSetContents(path, "0123456789");

    try (FileChannel firstChannel = FileChannel.open(path, WRITE)) {

      FileLock firstLock = firstChannel.lock(2L, 6L, true);

      assertTrue(firstLock.isValid(), "valid");
      assertTrue(firstLock.isShared(), "shared");
      firstLock.release();
      assertFalse(firstLock.isValid(), "valid");

      firstLock = firstChannel.lock(2L, 6L, false);

      assertTrue(firstLock.isValid(), "valid");
      assertFalse(firstLock.isShared(), "shared");

      try (FileChannel secondChannel = FileChannel.open(path, WRITE)) {

        assertNull(secondChannel.tryLock());
        assertNull(secondChannel.tryLock(1L, 8L, true));
        assertNull(secondChannel.tryLock(1L, 8L, false));

        assertThrows(OverlappingFileLockException.class, () -> secondChannel.lock(1L, 8L, true));

        assertThrows(OverlappingFileLockException.class, () -> secondChannel.lock(1L, 8L, false));

        FileLock secondLock = secondChannel.lock(0L, 2L, true);
        assertNotNull(secondLock);
        secondLock.release();

        // closing the first channel should release the first lock
        firstChannel.close();

        assertFalse(firstLock.isValid(), "valid");

        secondLock = secondChannel.lock();
        assertNotNull(secondLock);

      }

    }
  }

  @Test
  void writeByteArrayAppending() throws IOException {
    FileSystem fileSystem = this.extension.getFileSystem();

    Path path = fileSystem.getPath("file.txt");
    FileUtility.createAndSetContents(path, "z");

    byte[] data = new byte[]{'a', 'b', 'c', 'd'};

    try (OutputStream outputStream = Files.newOutputStream(path, APPEND)) {
      outputStream.write(data, 1, 2);
    }

    assertThat(path, hasContents("zbc"));
  }

  @Test
  void writeByteArrayNonAppending() throws IOException {
    FileSystem fileSystem = this.extension.getFileSystem();

    Path path = fileSystem.getPath("file.txt");
    byte[] data = new byte[]{'a', 'b', 'c', 'd'};

    try (OutputStream outputStream = Files.newOutputStream(path, CREATE_NEW)) {
      outputStream.write(data, 1, 2);
    }

    assertThat(path, hasContents("bc"));
  }

  @Test
  void readIntoBuffer() throws IOException {
    FileSystem fileSystem = this.extension.getFileSystem();

    Path path = fileSystem.getPath("file.txt");
    FileUtility.createAndSetContents(path, "abcd");

    byte[] data = new byte[2];

    try (FileChannel channel = FileChannel.open(path, READ)) {
      long read = channel.read(ByteBuffer.wrap(data), 1L);
      assertEquals(2L, read, "bytes read");
    }

    assertArrayEquals(new byte[]{'b', 'c'}, data);
  }

  @Test
  void scatteringRead() throws IOException {
    FileSystem fileSystem = this.extension.getFileSystem();

    Path path = fileSystem.getPath("file.txt");
    Files.createFile(path);
    setContents(path, "abcd");

    byte[] a = new byte[1];
    byte[] b = new byte[1];
    byte[] c = new byte[1];
    byte[] d = new byte[1];

    try (FileChannel channel = FileChannel.open(path, READ)) {
      ByteBuffer[] buffers = new ByteBuffer[]{ByteBuffer.wrap(a), ByteBuffer.wrap(b), ByteBuffer.wrap(c), ByteBuffer.wrap(d)};
      long read = channel.read(buffers, 1, 2);
      assertEquals(2L, read, "bytes read");
    }

    assertArrayEquals(new byte[]{0}, a);
    assertArrayEquals(new byte[]{'a'}, b);
    assertArrayEquals(new byte[]{'b'}, c);
    assertArrayEquals(new byte[]{0}, d);
  }

  @Test
  void scatteringReadBufferTooSmall() throws IOException {
    FileSystem fileSystem = this.extension.getFileSystem();

    Path path = fileSystem.getPath("file.txt");
    FileUtility.createAndSetContents(path, "abcdef");

    byte[] a = new byte[1];
    byte[] b = new byte[1];
    byte[] c = new byte[1];
    byte[] d = new byte[1];

    try (FileChannel channel = FileChannel.open(path, READ)) {
      ByteBuffer[] buffers = new ByteBuffer[]{ByteBuffer.wrap(a), ByteBuffer.wrap(b), ByteBuffer.wrap(c), ByteBuffer.wrap(d)};
      long read = channel.read(buffers);
      assertEquals(4L, read, "bytes read");
    }

    assertArrayEquals(new byte[]{'a'}, a);
    assertArrayEquals(new byte[]{'b'}, b);
    assertArrayEquals(new byte[]{'c'}, c);
    assertArrayEquals(new byte[]{'d'}, d);
  }

  @Test
  void scatteringWrite() throws IOException {
    FileSystem fileSystem = this.extension.getFileSystem();

    Path path = fileSystem.getPath("file.txt");
    ByteBuffer a = ByteBuffer.wrap(new byte[]{'a'});
    ByteBuffer b = ByteBuffer.wrap(new byte[]{'b'});
    ByteBuffer c = ByteBuffer.wrap(new byte[]{'c'});
    ByteBuffer d = ByteBuffer.wrap(new byte[]{'d'});

    try (FileChannel channel = FileChannel.open(path, CREATE_NEW, WRITE)) {
      long written = channel.write(new ByteBuffer[]{a, b, c, d}, 1, 2);
      assertEquals(2L, written, "byte written");
    }
    assertThat(path, hasContents("bc"));
  }

  @Test
  void scatteringWriteAppend() throws IOException {
    FileSystem fileSystem = this.extension.getFileSystem();

    Path path = fileSystem.getPath("file.txt");

    FileUtility.createAndSetContents(path, "z");

    ByteBuffer a = ByteBuffer.wrap(new byte[]{'a'});
    ByteBuffer b = ByteBuffer.wrap(new byte[]{'b'});
    ByteBuffer c = ByteBuffer.wrap(new byte[]{'c'});
    ByteBuffer d = ByteBuffer.wrap(new byte[]{'d'});

    try (FileChannel channel = FileChannel.open(path, APPEND)) {
      long written = channel.write(new ByteBuffer[]{a, b, c, d}, 1, 2);
      assertEquals(2L, written, "byte written");
    }
    assertThat(path, hasContents("zbc"));
  }

  @Test
  void writeAsyncChannelTruncate() throws IOException {
    FileSystem fileSystem = this.extension.getFileSystem();

    Path path = fileSystem.getPath("async.txt");
    FileUtility.createAndSetContents(path, "abc");

    try (AsynchronousFileChannel channel = AsynchronousFileChannel.open(path, WRITE)) {
      assertEquals(3L, channel.size());
      AsynchronousFileChannel result = channel.truncate(2L);
      assertSame(channel, result);
      assertEquals(2L, channel.size());
    }
    assertThat(path, hasContents("ab"));
  }

  @Test
  void writeSyncChannelTruncate() throws IOException {
    FileSystem fileSystem = this.extension.getFileSystem();

    Path path = fileSystem.getPath("async.txt");
    FileUtility.createAndSetContents(path, "abc");

    try (FileChannel channel = FileChannel.open(path, WRITE)) {
      assertEquals(3L, channel.size());
      FileChannel result = channel.truncate(2L);
      assertSame(channel, result);
      assertEquals(2L, channel.size());
    }
    assertThat(path, hasContents("ab"));
  }

  @Test
  void regressionIssue33() throws IOException {
    // https://github.com/marschall/memoryfilesystem/issues/33
    Path path = this.extension.getFileSystem().getPath("one").toAbsolutePath();
    Files.write(path, "hallo world".getBytes(UTF_8));
    Files.readAllBytes(path);
  }

  @Test
  void regressionIssue48() throws IOException {
    Path path = this.extension.getFileSystem().getPath("file");
    try (OutputStream stream = Files.newOutputStream(path)) {
      for (int i = 0; i <= 255; i++) {
        stream.write(i);
      }
    }
    try (InputStream stream = Files.newInputStream(path)) {
      for (int i = 0; i <= 255; i++) {
        assertEquals(i, stream.read());
      }
      assertEquals(-1, stream.read());
    }
  }

  @Test
  void blockChannelRead() throws IOException {
    // https://github.com/marschall/memoryfilesystem/issues/33
    Path path = this.extension.getFileSystem().getPath("one").toAbsolutePath();
    byte[] data = new byte[]{'a', 'b'};
    Files.write(path, data);
    byte[] readBack = new byte[data.length];
    try (SeekableByteChannel channel = Files.newByteChannel(path)) {
      ByteBuffer buffer = ByteBuffer.wrap(readBack);
      assertEquals(data.length, channel.read(buffer));
    }
    assertArrayEquals(data, readBack);
  }

  @Test
  void writeAsyncChannelBasicMethods() throws IOException {
    FileSystem fileSystem = this.extension.getFileSystem();

    Path path = fileSystem.getPath("async.txt");
    FileUtility.createAndSetContents(path, "z");

    try (AsynchronousFileChannel channel = AsynchronousFileChannel.open(path, WRITE)) {
      assertTrue(channel.isOpen(), "open");

      assertEquals(1L, channel.size());

      channel.close();

      assertFalse(channel.isOpen(), "open");
    }
  }

  @Test
  void writeAsyncChannelWriteCompletionHandler() throws IOException, InterruptedException {
    FileSystem fileSystem = this.extension.getFileSystem();

    Path path = fileSystem.getPath("async.txt");
    FileUtility.createAndSetContents(path, "z");

    try (AsynchronousFileChannel channel = AsynchronousFileChannel.open(path, WRITE)) {
      Object attachment = new Object();
      ByteBuffer buffer = ByteBuffer.wrap(new byte[]{'a', 'b'});

      CompletionHandlerStub<Integer, Object> handler = new CompletionHandlerStub<>();

      channel.write(buffer, 1L, attachment, handler);

      handler.await();

      assertTrue(handler.isCompleted());
      assertFalse(handler.isFailed());

      assertSame(attachment, handler.getAttachment(), "attachment");
      assertEquals(2, handler.getResult().intValue(), "bytes written");
    }

    assertThat(path, hasContents("zab"));
  }

  @Test
  void writeAsyncChannelReadCompletionHandler() throws IOException, InterruptedException {
    FileSystem fileSystem = this.extension.getFileSystem();

    Path path = fileSystem.getPath("async.txt");
    FileUtility.createAndSetContents(path, "abcd");

    try (AsynchronousFileChannel channel = AsynchronousFileChannel.open(path, READ)) {
      Object attachment = new Object();
      byte[] data = new byte[2];
      ByteBuffer buffer = ByteBuffer.wrap(data);

      CompletionHandlerStub<Integer, Object> handler = new CompletionHandlerStub<>();

      channel.read(buffer, 1L, attachment, handler);

      handler.await();

      assertTrue(handler.isCompleted());
      assertFalse(handler.isFailed());

      assertSame(attachment, handler.getAttachment(), "attachment");
      assertEquals(2, handler.getResult().intValue(), "bytes read");
      assertArrayEquals(new byte[]{'b', 'c'}, data);
    }

    assertThat(path, hasContents("abcd"));
  }

  @Test
  void writeAsyncChannelWriteIoException() throws IOException, InterruptedException {
    FileSystem fileSystem = this.extension.getFileSystem();

    Path path = fileSystem.getPath("async.txt");
    FileUtility.createAndSetContents(path, "z");

    try (AsynchronousFileChannel channel = AsynchronousFileChannel.open(path, READ)) {
      Object attachment = new Object();
      ByteBuffer buffer = ByteBuffer.wrap(new byte[]{'a', 'b'});

      CompletionHandlerStub<Integer, Object> handler = new CompletionHandlerStub<>();

      channel.write(buffer, 1L, attachment, handler);

      handler.await();

      assertFalse(handler.isCompleted(), "completed");
      assertTrue(handler.isFailed(), "failed");

      assertSame(attachment, handler.getAttachment(), "attachment");
      // TODO fix Hamcrest
      assertThat(handler.getException(), isA((Class<Throwable>) (Object) NonWritableChannelException.class));
    }

    assertThat(path, hasContents("z"));
  }

  @Test
  void writeAsyncChannelWriteCompletionFuture() throws IOException, InterruptedException, ExecutionException {
    FileSystem fileSystem = this.extension.getFileSystem();

    Path path = fileSystem.getPath("async.txt");
    FileUtility.createAndSetContents(path, "z");

    try (AsynchronousFileChannel channel = AsynchronousFileChannel.open(path, WRITE)) {
      ByteBuffer buffer = ByteBuffer.wrap(new byte[]{'a', 'b'});

      Future<Integer> future = channel.write(buffer, 1L);

      Integer written = future.get();
      assertEquals(2, written.intValue());
    }

    assertThat(path, hasContents("zab"));
  }

  @Test
  void writeAsyncChannelReadCompletionFuture() throws IOException, InterruptedException, ExecutionException {
    FileSystem fileSystem = this.extension.getFileSystem();

    Path path = fileSystem.getPath("async.txt");
    FileUtility.createAndSetContents(path, "abcd");

    try (AsynchronousFileChannel channel = AsynchronousFileChannel.open(path, READ)) {
      byte[] data = new byte[2];
      ByteBuffer buffer = ByteBuffer.wrap(data);

      Future<Integer> future = channel.read(buffer, 1);

      Integer read = future.get();
      assertEquals(2, read.intValue());
      assertArrayEquals(new byte[]{'b', 'c'},  data);
    }

    assertThat(path, hasContents("abcd"));
  }

  @Test
  void writeAsyncChannelWriteCompletionFutureFailed() throws IOException, InterruptedException {
    FileSystem fileSystem = this.extension.getFileSystem();

    Path path = fileSystem.getPath("async.txt");
    FileUtility.createAndSetContents(path, "z");

    try (AsynchronousFileChannel channel = AsynchronousFileChannel.open(path, READ)) {
      ByteBuffer buffer = ByteBuffer.wrap(new byte[]{'a', 'b'});

      Future<Integer> future = channel.write(buffer, 1L);

      ExecutionException e = assertThrows(ExecutionException.class,
              () -> future.get(),
              "write to reading channel should fail");
      Throwable cause = e.getCause();

      // TODO fix Hamcrest
      assertThat(cause, isA((Class<Throwable>) (Object) NonWritableChannelException.class));
    }
    assertThat(path, hasContents("z"));
  }

  @Test
  void lockAsyncChannel() throws IOException, InterruptedException, ExecutionException {
    FileSystem fileSystem = this.extension.getFileSystem();

    Path path = fileSystem.getPath("lock.txt");
    try (AsynchronousFileChannel channel = AsynchronousFileChannel.open(path, WRITE, CREATE_NEW)) {
      Future<FileLock> lockFuture = channel.lock();
      FileLock lock = lockFuture.get();
      assertSame(channel, lock.acquiredBy());
    }
  }

  @Test
  void deleteOnCloseInputStream() throws IOException {
    Path path = this.extension.getFileSystem().getPath("/sample.txt");
    Files.createFile(path);
    assertThat(path, exists());

    try (InputStream inputStream = Files.newInputStream(path)) {
      // nothing
    }
    assertThat(path, exists());

    try (InputStream inputStream = Files.newInputStream(path, DELETE_ON_CLOSE)) {
      // nothing
    }
    assertThat(path, not(exists()));
  }

  @Test
  void newBufferedWriterNoArguments() throws IOException {
    // https://github.com/marschall/memoryfilesystem/issues/9
    Path path = this.extension.getFileSystem().getPath("hello.txt");

    // file should be created
    try (BufferedWriter writer = Files.newBufferedWriter(path, US_ASCII)) {
      writer.write("world");
    }
    assertThat(path, exists());
    assertThat(path, hasContents("world"));

    // file should be truncated
    try (BufferedWriter writer = Files.newBufferedWriter(path, US_ASCII)) {
      writer.write("h");
    }
    assertThat(path, exists());
    assertThat(path, hasContents("h"));
  }

  @Test
  void deleteOnCloseOutputStream() throws IOException {
    Path path = this.extension.getFileSystem().getPath("/sample.txt");
    Files.createFile(path);
    assertThat(path, exists());

    try (OutputStream outputStream = Files.newOutputStream(path)) {
      // nothing
    }
    assertThat(path, exists());

    try (OutputStream outputStream = Files.newOutputStream(path, DELETE_ON_CLOSE)) {
      // nothing
    }
    assertThat(path, not(exists()));
  }

  @Test
  void deleteOnCloseByteChannelStream() throws IOException {
    Path path = this.extension.getFileSystem().getPath("/sample.txt");
    Files.createFile(path);
    assertThat(path, exists());

    try (SeekableByteChannel byteChannel = Files.newByteChannel(path)) {
      // nothing
    }
    assertThat(path, exists());

    try (SeekableByteChannel byteChannel = Files.newByteChannel(path, DELETE_ON_CLOSE)) {
      // nothing
    }
    assertThat(path, not(exists()));
  }

  @Test
  void probeContent() throws IOException {
    Path path = this.extension.getFileSystem().getPath("/sample.txt");
    Files.createFile(path);
    // returns "null" on JDK7 and "text/plain" on JDK8
    Files.probeContentType(path);
  }

  @Test
  void trasferTo() throws IOException {
    FileSystem fileSystem = this.extension.getFileSystem();

    Path from = fileSystem.getPath("from.txt");
    Path to = fileSystem.getPath("to.txt");
    this.writeBigContents(from);

    long expectedSize = SAMPLE_ITERATIONS * SAMPLE_DATA.length;
    assertEquals(expectedSize, Files.size(from));

    try (
            FileChannel fromChannel = FileChannel.open(from, READ);
            FileChannel toChannel = FileChannel.open(to, WRITE, CREATE_NEW)) {
      long trasferred = fromChannel.transferTo(0, expectedSize, toChannel);
      assertEquals(expectedSize, trasferred);
    }
    assertEquals(expectedSize, Files.size(to));
  }

  @Test
  void transferFromExpectedSize() throws IOException {
    FileSystem fileSystem = this.extension.getFileSystem();

    Path from = fileSystem.getPath("from.txt");
    Path to = fileSystem.getPath("to.txt");
    this.writeBigContents(from);

    long expectedSize = SAMPLE_ITERATIONS * SAMPLE_DATA.length;
    assertEquals(expectedSize, Files.size(from));

    try (
            FileChannel fromChannel = FileChannel.open(from, READ);
            FileChannel toChannel = FileChannel.open(to, WRITE, CREATE_NEW)) {
      long trasferred = toChannel.transferFrom(fromChannel, 0, expectedSize);
      assertEquals(expectedSize, trasferred);
    }
    assertEquals(expectedSize, Files.size(to));

  }

  @Test
  void transferFromLarger() throws IOException {
    FileSystem fileSystem = this.extension.getFileSystem();

    Path from = fileSystem.getPath("from.txt");
    Path to = fileSystem.getPath("to.txt");
    byte[] content = "How could this be wrong?".getBytes(US_ASCII);
    Files.write(from, content);
    long expectedSize = content.length;
    assertEquals(expectedSize, Files.size(from));

    try (SeekableByteChannel fromChannel = Files.newByteChannel(from);
            FileChannel toChannel = FileChannel.open(to, READ, WRITE, CREATE_NEW)) {
      long count = toChannel.transferFrom(fromChannel, 0, content.length * 2);

      assertEquals(expectedSize, count);
    }
    assertEquals(expectedSize, Files.size(to));
    assertArrayEquals(content, Files.readAllBytes(to));
  }

  private void writeBigContents(Path path) throws IOException {
    try (SeekableByteChannel channel = Files.newByteChannel(path, WRITE, CREATE_NEW)) {
      ByteBuffer src = ByteBuffer.wrap(SAMPLE_DATA);
      for (int i = 0; i < SAMPLE_ITERATIONS; i++) {
        src.rewind();
        channel.write(src);
      }
    }
  }

  @Test
  void setDirectory() {
    FileSystem fileSystem = this.extension.getFileSystem();
    Path path = fileSystem.getPath("/");
    assertThrows(IllegalArgumentException.class, () -> Files.setAttribute(path, "isDirectory", false));
  }


  @Test
  void getFileStore() throws IOException {
    FileSystem fileSystem = this.extension.getFileSystem();
    Path root = fileSystem.getPath("/");
    FileStore fileStore = Files.getFileStore(root);
    assertNotNull(fileStore);
  }

  @Test
  void targetDoesNotExist() throws IOException {
    FileSystem fileSystem = this.extension.getFileSystem();
    Path link = fileSystem.getPath("/link");
    Path target = fileSystem.getPath("/target");
    Files.createSymbolicLink(link, target);

    Path readBack = Files.readSymbolicLink(link);
    assertThat(readBack, not(exists()));
    assertEquals("/target", readBack.toString());
  }

  @Test
  void targetIsLink() throws IOException {
    FileSystem fileSystem = this.extension.getFileSystem();
    // a -> b -> c
    Path a = fileSystem.getPath("/a");
    Path b = fileSystem.getPath("/b");
    Path c = fileSystem.getPath("/c");
    Files.createSymbolicLink(a, b);
    Files.createSymbolicLink(b, c);

    Path readBack = Files.readSymbolicLink(a);
    assertThat(readBack, exists(NOFOLLOW_LINKS));
    assertThat(readBack, not(exists()));
    assertTrue(Files.isSymbolicLink(readBack));
    assertEquals("/b", readBack.toString());
  }


  @Test
  void symbolicLinkLoop() throws IOException {
    FileSystem fileSystem = this.extension.getFileSystem();
    Path a = fileSystem.getPath("/a");
    Path b = fileSystem.getPath("/b");
    Files.createSymbolicLink(a, b);
    Files.createSymbolicLink(b, a);

    Path aAsSymlink = Files.readSymbolicLink(a);
    assertEquals("/b", aAsSymlink.toString());

    assertThrows(FileSystemLoopException.class,
            a::toRealPath,
            "FileSystemLoopException expected");
  }

  @Test
  void readSymbolicLinkNotALink() throws IOException {
    FileSystem fileSystem = this.extension.getFileSystem();
    Path target = fileSystem.getPath("/target");
    Files.createFile(target);

    assertThrows(NotLinkException.class, () -> Files.readSymbolicLink(target));
  }

  @Test
  void readSymbolicLink() throws IOException {
    FileSystem fileSystem = this.extension.getFileSystem();
    Path link = fileSystem.getPath("/link");
    Path target = fileSystem.getPath("/target");
    Files.createFile(target);
    Files.createSymbolicLink(link, target);

    Path resolvedSymbolicLink = Files.readSymbolicLink(link);
    assertEquals("/target", resolvedSymbolicLink.toString());

    resolvedSymbolicLink = Files.readSymbolicLink(this.extension.getFileSystem().getPath("/link/.././link/"));
    assertEquals("/target", resolvedSymbolicLink.toString());

    assertEquals("/target", link.toRealPath().toString());
    assertEquals("/link", link.toRealPath(NOFOLLOW_LINKS).toString());
  }

  @Test
  void readSymbolicLinkDirectory() throws IOException {
    FileSystem fileSystem = this.extension.getFileSystem();

    Path link = Files.createDirectory(fileSystem.getPath("/dir1")).resolve("link");
    Path target = Files.createDirectory(fileSystem.getPath("/dir2")).resolve("target");
    Files.createFile(target);
    Files.createSymbolicLink(link, target);

    Path aAsSymlink = Files.readSymbolicLink(link);
    assertEquals("/dir2/target", aAsSymlink.toString());
    assertEquals("/dir1/link", link.toRealPath(NOFOLLOW_LINKS).toString());
    assertEquals("/dir2/target", link.toRealPath().toString());
  }

  /**
   * Regression test for issue 35.
   *
   * @throws IOException if the test fails
   */
  @Test
  void issue35() throws IOException {
    FileSystem fileSystem = this.extension.getFileSystem();

    List<String> lines = Collections.singletonList("Hello world");

    Path file = fileSystem.getPath("/").resolve("file");
    Files.write(file, lines, UTF_8);

    Path link = file.resolveSibling("link");
    // link -> file
    Files.createSymbolicLink(link, file);

    List<String> readBack = Files.readAllLines(link, UTF_8);
    assertEquals(lines, readBack);
  }


  /**
   * Regression test for issue 38.
   *
   * @throws IOException if the test fails
   */
  @Test
  void issue38() throws IOException {
    FileSystem fileSystem = this.extension.getFileSystem();
    Path root = Files.createDirectory(fileSystem.getPath("/").resolve("root"));

    List<String> lines = Collections.singletonList("Hello world");

    Path target = root.resolve("file");
    Files.write(target, lines, UTF_8);

    Path link = target.resolveSibling("link");
    // link -> file
    Files.createSymbolicLink(link, target.getFileName());

    List<String> readBack = Files.readAllLines(link, UTF_8);
    assertEquals(lines, readBack);

  }


  @Test
  void readFromSymbolicLink() throws IOException {
    FileSystem fileSystem = this.extension.getFileSystem();

    List<String> lines = Collections.singletonList("Hello world");

    Path file = fileSystem.getPath("/").resolve("file");
    Files.createFile(file);

    Path link = file.resolveSibling("link");
    // link -> file
    Files.createSymbolicLink(link, file);

    Files.write(link, lines, UTF_8);
    List<String> readBack = Files.readAllLines(link, UTF_8);
    assertEquals(lines, readBack);
  }

  @Test
  void readSymlinkLoop() throws IOException {
    FileSystem fileSystem = this.extension.getFileSystem();

    Path file = fileSystem.getPath("/").resolve("file");

    Path link = file.resolveSibling("link");
    Files.createSymbolicLink(link, file);
    Files.createSymbolicLink(file, link);

    assertThrows(FileSystemLoopException.class,
            () -> Files.readAllLines(link, UTF_8),
            // if the run into a loop we'll actually never reach here
            "loop");
  }

  @Test
  void dontDeleteOpenFile() throws IOException {
    FileSystem fileSystem = this.extension.getFileSystem();
    Path path = fileSystem.getPath("test");
    try (SeekableByteChannel channel = Files.newByteChannel(path, CREATE_NEW, WRITE)) {
      assertThrows(FileSystemException.class , () -> Files.delete(path), "you shound't be able to delete a file wile it's open");
      assertThrows(FileSystemException.class , () -> Files.deleteIfExists(path), "you shound't be able to delete a file wile it's open");
    }
  }

  @Test
  void delete() throws IOException {
    FileSystem fileSystem = this.extension.getFileSystem();
    Path directory = fileSystem.getPath("directory");
    Path path = directory.resolve("test");
    assertSame(directory, Files.createDirectory(directory));
    assertSame(path, Files.createFile(path));

    assertThrows(DirectoryNotEmptyException.class, () -> Files.delete(directory), "you shound't be able to delete a non-emtpy directory");

    Files.delete(path);
    assertThat(path, not(exists()));
    assertThrows(NoSuchFileException.class, () -> Files.delete(path), "you shound't be able to delete a non-existing file");

    Files.delete(directory);
    assertThat(directory, not(exists()));
    assertThrows(NoSuchFileException.class, () -> Files.delete(directory), "you shound't be able to delete a non-existing directory");
  }

  @Test
  void deleteIfExists() throws IOException {
    FileSystem fileSystem = this.extension.getFileSystem();
    Path directory = fileSystem.getPath("directory");
    Path path = directory.resolve("test");
    assertSame(directory, Files.createDirectory(directory));
    assertSame(path, Files.createFile(path));

    assertThrows(DirectoryNotEmptyException.class, () -> Files.delete(directory), "you shound't be able to delete a non-emtpy directory");

    assertTrue(Files.deleteIfExists(path));
    assertThat(path, not(exists()));
    assertFalse(Files.deleteIfExists(path));

    assertTrue(Files.deleteIfExists(directory));
    assertThat(directory, not(exists()));
    assertFalse(Files.deleteIfExists(directory));
  }

  @Test
  void inputStream() throws IOException {
    FileSystem fileSystem = this.extension.getFileSystem();
    Path path = fileSystem.getPath("test");
    try (SeekableByteChannel channel = Files.newByteChannel(path, CREATE_NEW, WRITE)) {
      channel.write(ByteBuffer.wrap(new byte[]{1, 2, 3}));
    }
    try (InputStream input = Files.newInputStream(path)) {
      byte[] data = new byte[5];
      int start = 1;
      int read;
      while ((read = input.read(data, start, 555)) != -1) {
        start += read;
      }
      assertEquals(4, start); // the next read should start at 4
      assertArrayEquals(new byte[]{0, 1, 2, 3, 0},  data);
    }
  }

  @Test
  void truncateExisting() throws IOException {
    FileSystem fileSystem = this.extension.getFileSystem();
    Path path = fileSystem.getPath("test");
    try (SeekableByteChannel channel = Files.newByteChannel(path, CREATE_NEW, WRITE)) {
      channel.write(ByteBuffer.wrap(new byte[]{1, 2, 3}));
    }
    Object size = Files.getAttribute(path, "size");
    assertEquals(3L, size);

    // TRUNCATE_EXISTING with READ should not truncate
    try (SeekableByteChannel channel = Files.newByteChannel(path, TRUNCATE_EXISTING, READ)) {
      // ignore
    }

    size = Files.getAttribute(path, "size");
    assertEquals(3L, size);

    // WRITE alone should not truncate
    try (SeekableByteChannel channel = Files.newByteChannel(path, WRITE)) {
      // ignore
    }

    size = Files.getAttribute(path, "size");
    assertEquals(3L, size);

    // TRUNCATE_EXISTING with WRITE should truncate
    try (SeekableByteChannel channel = Files.newByteChannel(path, TRUNCATE_EXISTING, WRITE)) {
      // ignore
    }

    size = Files.getAttribute(path, "size");
    assertEquals(0L, size);
  }

  @Test
  void position() throws IOException {
    FileSystem fileSystem = this.extension.getFileSystem();
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
  void setPosition() throws IOException {
    FileSystem fileSystem = this.extension.getFileSystem();
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
  void testIsSameFile() {
    FileSystem fileSystem = this.extension.getFileSystem();
    Path memoryPath = fileSystem.getPath("/");
    Path defaultPath = Paths.get("/foo/bar");

    //    assertTrue(Files.isSameFile(defaultPath, defaultPath));
    assertThat(memoryPath, isSameFile(memoryPath));
    assertThat(memoryPath, not(isSameFile(defaultPath)));
    assertThat(defaultPath, not(isSameFile( memoryPath)));
  }

  @Test
  void isSameFileNotExisting() throws IOException {
    FileSystem fileSystem = this.extension.getFileSystem();
    Path path = fileSystem.getPath("/foo/bar");

    assertThat(path, not(exists()));
    Files.isSameFile(path, path);

    Path path2 = fileSystem.getPath("/foo/bar/zork");
    assertThat(path2, not(exists()));
    assertThrows(NoSuchFileException.class,
            () -> Files.isSameFile(path, path2),
            "file does not exist");
  }

  @Test
  void isRegularFile() throws IOException {
    FileSystem fileSystem = this.extension.getFileSystem();
    Path path = fileSystem.getPath("/");
    Map<String, Object> attributes = Files.readAttributes(path, "isDirectory");
    assertEquals(Collections.singletonMap("isDirectory", true), attributes);
  }

  @Test
  void emptySubPath() {
    FileSystem fileSystem = this.extension.getFileSystem();
    Path empty = fileSystem.getPath("");
    assertEquals(1, empty.getNameCount());
    assertEquals(empty, empty.subpath(0, 1));
  }

  @Test
  void subPath() {
    FileSystem fileSystem = this.extension.getFileSystem();
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
  void normalizeRoot() {
    FileSystem fileSystem = this.extension.getFileSystem();
    Path root = fileSystem.getPath("/");

    assertEquals(root, root.normalize());
  }

  @Test
  void normalizeAbsolute() {
    FileSystem fileSystem = this.extension.getFileSystem();

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
  void normalizeRelative() {
    FileSystem fileSystem = this.extension.getFileSystem();

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
  void resolveRoot() {
    FileSystem fileSystem = this.extension.getFileSystem();

    assertEquals(fileSystem.getPath("/"), fileSystem.getPath("/a").resolve(fileSystem.getPath("/")));
    assertEquals(fileSystem.getPath("/"), fileSystem.getPath("a").resolve(fileSystem.getPath("/")));
  }

  @Test
  void resolveAbsoluteOtherAbsolute() {
    FileSystem fileSystem = this.extension.getFileSystem();
    Path absolute = fileSystem.getPath("/a/b");

    assertEquals(absolute, fileSystem.getPath("").resolve(absolute));
    assertEquals(absolute, fileSystem.getPath("/").resolve(absolute));
    assertEquals(absolute, fileSystem.getPath("c/d").resolve(absolute));
    assertEquals(absolute, fileSystem.getPath("/c/d").resolve(absolute));
  }

  @Test
  void resolveAbsoluteOtherRelative() {
    FileSystem fileSystem = this.extension.getFileSystem();
    Path relative = fileSystem.getPath("a/b");

    assertEquals(fileSystem.getPath("a/b"), fileSystem.getPath("").resolve(relative));
    assertEquals(fileSystem.getPath("/a/b"), fileSystem.getPath("/").resolve(relative));
    assertEquals(fileSystem.getPath("c/d/a/b"), fileSystem.getPath("c/d").resolve(relative));
    assertEquals(fileSystem.getPath("/c/d/a/b"), fileSystem.getPath("/c/d").resolve(relative));
  }


  @Test
  void resolveAbsoluteOtherAbsoluteString() {
    FileSystem fileSystem = this.extension.getFileSystem();
    String absolute = "/a/b";
    Path absolutePath = fileSystem.getPath("/a/b");

    assertEquals(absolutePath, fileSystem.getPath("").resolve(absolute));
    assertEquals(absolutePath, fileSystem.getPath("/").resolve(absolute));
    assertEquals(absolutePath, fileSystem.getPath("c/d").resolve(absolute));
    assertEquals(absolutePath, fileSystem.getPath("/c/d").resolve(absolute));
  }

  @Test
  void resolveAbsoluteOtherRelativeString() {
    FileSystem fileSystem = this.extension.getFileSystem();
    String relative = "a/b";

    assertEquals(fileSystem.getPath("a/b"), fileSystem.getPath("").resolve(relative));
    assertEquals(fileSystem.getPath("/a/b"), fileSystem.getPath("/").resolve(relative));
    assertEquals(fileSystem.getPath("c/d/a/b"), fileSystem.getPath("c/d").resolve(relative));
    assertEquals(fileSystem.getPath("/c/d/a/b"), fileSystem.getPath("/c/d").resolve(relative));
  }

  @Test
  void resolveSibling() {
    FileSystem fileSystem = this.extension.getFileSystem();
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
  void resolveSiblingAgainstRoot() {
    FileSystem fileSystem = this.extension.getFileSystem();
    Path root = fileSystem.getPath("/");

    assertEquals(fileSystem.getPath("a"), root.resolveSibling(fileSystem.getPath("a")));
    assertEquals(fileSystem.getPath("a/b"), root.resolveSibling(fileSystem.getPath("a/b")));
    assertEquals(fileSystem.getPath("/a"), root.resolveSibling(fileSystem.getPath("/a")));
    assertEquals(fileSystem.getPath("/a/b"), root.resolveSibling(fileSystem.getPath("/a/b")));
    assertEquals(fileSystem.getPath(""), root.resolveSibling(fileSystem.getPath("")));
  }

  @Test
  void resolveSiblingAgainstRootString() {
    FileSystem fileSystem = this.extension.getFileSystem();
    Path root = fileSystem.getPath("/");

    assertEquals(fileSystem.getPath("a"), root.resolveSibling("a"));
    assertEquals(fileSystem.getPath("a/b"), root.resolveSibling("a/b"));
    assertEquals(fileSystem.getPath("/a"), root.resolveSibling("/a"));
    assertEquals(fileSystem.getPath("/a/b"), root.resolveSibling("/a/b"));
    assertEquals(fileSystem.getPath(""), root.resolveSibling(""));
  }


  @Test
  void relativizeAbsolute() {
    FileSystem fileSystem = this.extension.getFileSystem();
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

  @Test
  void relativizeAbsoluteUnsupported1() {
    FileSystem fileSystem = this.extension.getFileSystem();
    Path first = fileSystem.getPath("/a/b");
    Path second = fileSystem.getPath("c");
    assertThrows(IllegalArgumentException.class, () -> first.relativize(second));
  }

  @Test
  void relativizeAbsoluteUnsupported2() {
    FileSystem fileSystem = this.extension.getFileSystem();
    Path first = fileSystem.getPath("/a/b");
    Path second = fileSystem.getPath("c");
    assertThrows(IllegalArgumentException.class, () -> second.relativize(first));
  }


  @Test
  void relativizeRelative() {
    FileSystem fileSystem = this.extension.getFileSystem();
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
  void relativizeRelativeRoot() {
    FileSystem fileSystem = this.extension.getFileSystem();
    Path first = fileSystem.getPath("/");
    Path second = fileSystem.getPath("/a/b");

    assertEquals(fileSystem.getPath("a/b"), first.relativize(second));
    assertEquals(fileSystem.getPath("../.."), second.relativize(first));
    assertEquals(fileSystem.getPath(""), first.relativize(first));
  }

  @Test
  void absoluteIterator() {
    FileSystem fileSystem = this.extension.getFileSystem();
    Path usrBin = fileSystem.getPath("/usr/bin");
    Iterable<String> expected = Arrays.asList("usr", "bin");
    this.assertIterator(fileSystem, usrBin, expected);
  }

  @Test
  void relativeIterator() {
    FileSystem fileSystem = this.extension.getFileSystem();
    Path usrBin = fileSystem.getPath("usr/bin");
    Iterable<String> expected = Arrays.asList("usr", "bin");
    this.assertIterator(fileSystem, usrBin, expected);
  }

  private void assertIterator(FileSystem fileSystem, Path path, Iterable<String> expected) {
    Iterator<Path> actualIterator = path.iterator();
    Iterator<String> expectedIterator = expected.iterator();
    while (actualIterator.hasNext()) {
      Path actualPath = actualIterator.next();
      assertThrows(UnsupportedOperationException.class,
              actualIterator::remove,
              "path iterator should not support #remove()");

      assertTrue(expectedIterator.hasNext());
      String expectedName = expectedIterator.next();
      Path expectedPath = fileSystem.getPath(expectedName);

      assertEquals(expectedPath, actualPath);
      assertThat(actualPath, isRelative());
    }
    assertFalse(expectedIterator.hasNext());
  }

  @Test
  void endsWith() {
    FileSystem fileSystem = this.extension.getFileSystem();

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
  void startsWith() {
    FileSystem fileSystem = this.extension.getFileSystem();

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
  void getFileName() {
    FileSystem fileSystem = this.extension.getFileSystem();
    Path usrBin = fileSystem.getPath("/usr/bin");
    Path bin = fileSystem.getPath("bin");

    Path fileName = usrBin.getFileName();
    assertNotNull(fileName);

    assertEquals(fileName, bin);
    assertThat(fileName, isRelative());
  }

  @Test
  void absoluteGetParent() {
    FileSystem fileSystem = this.extension.getFileSystem();
    Path usrBin = fileSystem.getPath("/usr/bin");
    Path usr = fileSystem.getPath("/usr");

    assertEquals(usr, usrBin.getParent());
    assertThat(usrBin.getParent(), isAbsolute());
    Path root = fileSystem.getRootDirectories().iterator().next();
    assertEquals(root, usr.getParent());
    assertThat(usr.getParent(), isAbsolute());
  }

  @Test
  void relativeGetParent() {
    FileSystem fileSystem = this.extension.getFileSystem();
    Path usrBin = fileSystem.getPath("usr/bin");
    Path usr = fileSystem.getPath("usr");

    assertEquals(usr, usrBin.getParent());
    assertThat(usrBin.getParent(), isRelative());
    assertNull(usr.getParent());
  }

  @Test
  void absoluteGetName0() {
    FileSystem fileSystem = this.extension.getFileSystem();
    Path usrBin = fileSystem.getPath("/usr/bin");
    assertThrows(IllegalArgumentException.class, () -> usrBin.getName(-1));
  }

  @Test
  void absoluteGetNameToLong() {
    FileSystem fileSystem = this.extension.getFileSystem();
    Path usrBin = fileSystem.getPath("/usr/bin");
    assertThrows(IllegalArgumentException.class, () -> usrBin.getName(2));
  }

  @Test
  void emptyGetName() {
    FileSystem fileSystem = this.extension.getFileSystem();
    Path empty = fileSystem.getPath("");
    assertEquals(1, empty.getNameCount());
    assertEquals(empty, empty.getName(0));
  }

  @Test
  void absoluteGetName() {
    FileSystem fileSystem = this.extension.getFileSystem();
    Path usrBin = fileSystem.getPath("/usr/bin");
    Path usr = fileSystem.getPath("usr");
    assertEquals(usr, usrBin.getName(0));
    Path bin = fileSystem.getPath("bin");
    assertEquals(bin, usrBin.getName(1));
  }

  @Test
  void relativeGetName() {
    FileSystem fileSystem = this.extension.getFileSystem();
    Path usrBin = fileSystem.getPath("usr/bin");
    Path usr = fileSystem.getPath("usr");
    assertEquals(usr, usrBin.getName(0));
    Path bin = fileSystem.getPath("bin");
    assertEquals(bin, usrBin.getName(1));
  }

  @Test
  void relativeGetName0() {
    FileSystem fileSystem = this.extension.getFileSystem();
    Path usrBin = fileSystem.getPath("usr/bin");
    assertThrows(IllegalArgumentException.class, () -> usrBin.getName(-1));
  }

  @Test
  void relativeGetNameToLong() {
    FileSystem fileSystem = this.extension.getFileSystem();
    Path usrBin = fileSystem.getPath("usr/bin");
    assertThrows(IllegalArgumentException.class, () -> usrBin.getName(2));
  }

  @Test
  void emptyPath() {
    FileSystem fileSystem = this.extension.getFileSystem();
    Path path = fileSystem.getPath("");
    assertThat(path, isRelative());
    assertNull(path.getRoot());
  }

  // https://bugs.openjdk.java.net/browse/JDK-8043208
  @Test
  void jdk8043208() {
    FileSystem fileSystem = this.extension.getFileSystem();
    Path path = fileSystem.getPath("").normalize();
    assertThat(path, isRelative());
    assertNull(path.getRoot());
  }

  // https://bugs.openjdk.java.net/browse/JDK-8066943
  @Test
  @Disabled
  void jdk8066943() {
    FileSystem fileSystem = this.extension.getFileSystem();
    Path path = fileSystem.getPath("..").relativize(fileSystem.getPath("x"));
    assertThat(path, isRelative());
    assertEquals("../x", path.toString());
  }

  @Test
  void normalizeEmptyPaths() {
    FileSystem fileSystem = this.extension.getFileSystem();
    Path p1 = fileSystem.getPath(".");
    Path p2 = fileSystem.getPath("./s");
    assertTrue(p2.startsWith(p1));
    assertFalse(p2.normalize().startsWith(p1.normalize()));
  }

  @Test
  void getNameCount() {
    FileSystem fileSystem = this.extension.getFileSystem();
    Path usrBin = fileSystem.getPath("/usr/bin");
    assertEquals(2, usrBin.getNameCount());

    usrBin = fileSystem.getPath("usr/bin");
    assertEquals(2, usrBin.getNameCount());
  }

  @Test
  void isReadOnly() {
    FileSystem fileSystem = this.extension.getFileSystem();
    assertFalse(fileSystem.isReadOnly());
  }

  @Test
  void absolutePaths() {
    FileSystem fileSystem = this.extension.getFileSystem();
    Path path = fileSystem.getPath("/");
    assertThat(path, isAbsolute());
    assertSame(path, path.toAbsolutePath());

    path = fileSystem.getPath("/", "sample");
    assertThat(path, isAbsolute());
    assertSame(path, path.toAbsolutePath());
    assertNotNull(path.getRoot());
    assertSame(this.getRoot(fileSystem), path.getRoot());
  }

  @Test
  void relativePaths() {
    FileSystem fileSystem = this.extension.getFileSystem();
    Path path = fileSystem.getPath("sample");
    assertThat(path, isRelative());
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
  void supportedFileAttributeViews() {
    FileSystem fileSystem = this.extension.getFileSystem();
    assertEquals(Collections.singleton(FileAttributeViews.BASIC), fileSystem.supportedFileAttributeViews());
  }

  @Test
  void pathToString() {
    FileSystem fileSystem = this.extension.getFileSystem();
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
  void defaultSeparator() {
    FileSystem fileSystem = this.extension.getFileSystem();
    assertEquals("/", fileSystem.getSeparator());
  }


  @Test
  void slash() {
    FileSystem fileSystem = this.extension.getFileSystem();
    Path path = fileSystem.getPath("/");
    assertThrows(IllegalArgumentException.class, () -> path.subpath(0, 1));
  }

  @Test
  void createDirectoryNoParent() {
    FileSystem fileSystem = this.extension.getFileSystem();
    Path homePmarscha = fileSystem.getPath("/home/pmarscha");
    assertThat(homePmarscha, not(exists()));
    assertThrows(IOException.class, () -> Files.createDirectory(homePmarscha));
  }

  @Test
  void pathOrdering() {
    FileSystem fileSystem = this.extension.getFileSystem();
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
  void xattrs() throws IOException {
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

      assertThrows(IOException.class,
              () -> userAttributes.read("wrong-key", buffer),
              "there should be nothing under \"wrong-key\"");
    }
  }

  @Test
  void pathOrderingDifferentFileSystem() throws IOException {
    FileSystem fileSystem1 = this.extension.getFileSystem();
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

      Path relative1 = fileSystem1.getPath("a");
      Path relative2 = fileSystem2.getPath("a");

      assertThat(relative1, not(equalTo(relative2)));
      assertThat(relative2, not(equalTo(relative1)));
      assertThat(relative1, lessThan(relative2));
      assertThat(relative2, greaterThan(relative1));

      Path absolute1 = fileSystem1.getPath("/a");
      Path absolute2 = fileSystem2.getPath("/a");

      assertThat(absolute1, not(equalTo(absolute2)));
      assertThat(absolute2, not(equalTo(absolute1)));
      assertThat(absolute1, lessThan(absolute2));
      assertThat(absolute2, greaterThan(absolute1));
    }

  }

  @Test
  void pathOrderingIncompatible() {
    FileSystem fileSystem = this.extension.getFileSystem();
    Path a = fileSystem.getPath("a");
    Path b = FileSystems.getDefault().getPath("b");
    assertThrows(ClassCastException.class, () -> a.compareTo(b));
  }

  @Test
  void createDirectories() throws IOException {
    FileSystem fileSystem = this.extension.getFileSystem();
    Path homePmarscha = fileSystem.getPath("/home/pmarscha");
    assertThat(homePmarscha, not(exists()));
    Files.createDirectories(homePmarscha);
    assertThat(homePmarscha, exists());
  }


  @Test
  void createDirectory() throws IOException {
    FileSystem fileSystem = this.extension.getFileSystem();
    Path home = fileSystem.getPath("/home");
    assertThat(home, not(exists()));
    Files.createDirectory(home);
    assertThat(home, exists());
    assertTrue(Files.isDirectory(home));
    assertFalse(Files.isRegularFile(home));
  }

  @Test
  void createDirectoryAlreadyExists() throws IOException {
    FileSystem fileSystem = this.extension.getFileSystem();
    Path home = fileSystem.getPath("/home");
    assertThat(home, not(exists()));
    Files.createDirectory(home);
    assertThat(home, exists());
    assertThrows(FileAlreadyExistsException.class, () -> Files.createDirectory(home));
  }

  @Test
  void pathOfFileAlreadyExistsException() throws IOException {
    FileSystem fileSystem = this.extension.getFileSystem();

    Path root = fileSystem.getPath("/");
    FileAlreadyExistsException e = assertThrows(FileAlreadyExistsException.class,
            () -> Files.createDirectory(root),
            "root already exists");
    assertEquals("/", e.getFile());

    Path subPath = fileSystem.getPath("/sub/path");
    Files.createDirectories(subPath);

    e = assertThrows(FileAlreadyExistsException.class,
            () -> Files.createDirectory(subPath),
            "root already exists");
    assertEquals("/sub/path", e.getFile());
  }

  @Test
  void createDirectoriesWithRoot() throws IOException {
    FileSystem fileSystem = this.extension.getFileSystem();
    Path root = fileSystem.getPath("/");
    assertThat(root, exists());
    assertEquals(Files.createDirectories(root), root);
  }

  @Test
  void getRootDirectories() {
    FileSystem fileSystem = this.extension.getFileSystem();
    Iterator<Path> directories = fileSystem.getRootDirectories().iterator();
    assertTrue(directories.hasNext());
    directories.next();
    assertThrows(UnsupportedOperationException.class,
            directories::remove,
            "root directories iterator should not support remove");
    assertFalse(directories.hasNext());
  }

  @Test
  void normalizeEmptyPath() {
    // https://bugs.openjdk.java.net/browse/JDK-8037945
    Path path = this.extension.getFileSystem().getPath("");
    assertEquals(path, path.normalize());
  }

  @Test
  void providerMismatch() {
    Path root = Paths.get("");
    assertThrows(ProviderMismatchException.class, () -> this.extension.getFileSystem().provider().move(root, root));
  }

  @Test
  void readAttributes() throws IOException, ParseException {
    FileSystem fileSystem = this.extension.getFileSystem();
    Path patch = fileSystem.getPath("/file.txt");

    Files.createFile(patch);

    FileTime lastModifiedTime = FileTime.from(Instant.parse("2012-11-07T20:30:22.111111111Z"));
    FileTime lastAccessTime = FileTime.from(Instant.parse("2012-10-07T20:30:22.222222222Z"));
    FileTime createTime = FileTime.from(Instant.parse("2012-09-07T20:30:22.333333333Z"));

    BasicFileAttributeView basicFileAttributeView = Files.getFileAttributeView(patch, BasicFileAttributeView.class);
    basicFileAttributeView.setTimes(lastModifiedTime, lastAccessTime, createTime);

    Map<String, Object> attributes = Files.readAttributes(patch, "lastModifiedTime,lastAccessTime,size");

    Map<String, Object> expected = new HashMap<>(3);
    expected.put("size", 0L);
    expected.put("lastModifiedTime", lastModifiedTime);
    expected.put("lastAccessTime", lastAccessTime);

    assertEquals(expected, attributes);
  }

  @Test
  void notChangingAttributes() throws IOException, ParseException {
    // https://github.com/marschall/memoryfilesystem/issues/16
    FileSystem fileSystem = this.extension.getFileSystem();
    Path source = fileSystem.getPath("/source.txt");
    Files.createFile(source);

    FileTime originalTime = FileTime.from(Instant.parse("2011-11-01T20:30:15.123456789Z"));
    FileTime newTime = FileTime.from(Instant.parse("2012-11-07T20:30:22.987654321Z"));

    BasicFileAttributeView attributeView = Files.getFileAttributeView(source, BasicFileAttributeView.class);
    attributeView.setTimes(originalTime, originalTime, originalTime);

    BasicFileAttributes attributes = Files.readAttributes(source, BasicFileAttributes.class);
    attributeView.setTimes(newTime, newTime, newTime);

    assertEquals(originalTime, attributes.lastModifiedTime());
    assertEquals(originalTime, attributes.lastAccessTime());
    assertEquals(originalTime, attributes.creationTime());
  }

  @Test
  void setTimesNull() throws IOException, ParseException {
    FileSystem fileSystem = this.extension.getFileSystem();
    Path source = fileSystem.getPath("/source.txt");
    Files.createFile(source);

    FileTime originalTime = FileTime.from(Instant.parse("2011-11-01T20:30:15.111111111Z"));
    FileTime lastModifiedTime = FileTime.from(Instant.parse("2012-11-07T20:30:22.222222222Z"));
    FileTime lastAccessedTime = FileTime.from(Instant.parse("2012-10-07T20:30:22.333333333Z"));
    FileTime createTime = FileTime.from(Instant.parse("2012-09-07T20:30:22.444444444Z"));

    Files.getFileAttributeView(source, BasicFileAttributeView.class).setTimes(originalTime, originalTime, originalTime);

    Files.getFileAttributeView(source, BasicFileAttributeView.class).setTimes(null, null, createTime);
    assertEquals(createTime, Files.getAttribute(source, "creationTime"));
    assertEquals(originalTime, Files.getAttribute(source, "lastModifiedTime"));
    assertEquals(originalTime, Files.getAttribute(source, "lastAccessTime"));

    Files.getFileAttributeView(source, BasicFileAttributeView.class).setTimes(null, lastAccessedTime, null);
    assertEquals(createTime, Files.getAttribute(source, "creationTime"));
    assertEquals(originalTime, Files.getAttribute(source, "lastModifiedTime"));
    assertEquals(lastAccessedTime, Files.getAttribute(source, "lastAccessTime"));

    Files.getFileAttributeView(source, BasicFileAttributeView.class).setTimes(lastModifiedTime, null, null);
    assertEquals(createTime, Files.getAttribute(source, "creationTime"));
    assertEquals(lastModifiedTime, Files.getAttribute(source, "lastModifiedTime"));
    assertEquals(lastAccessedTime, Files.getAttribute(source, "lastAccessTime"));
  }

  @Test
  void regressionIssue46() throws IOException {
    Path path = this.extension.getFileSystem().getPath("existing.zip");
    Files.createFile(path);
    FileTime time = FileTime.fromMillis(System.currentTimeMillis());
    Files.setAttribute(path, "basic:lastModifiedTime", time);
  }

  @Test
  void inputStreamDoubleClose() throws IOException {
    // regression test for
    // https://github.com/marschall/memoryfilesystem/issues/49
    Path path = this.extension.getFileSystem().getPath("double-close.txt");

    Files.write(path, new byte[]{1, 2, 3});
    try (InputStream stream = Files.newInputStream(path)) {
      // intentionally double close
      stream.close();
    }

    try (InputStream stream = Files.newInputStream(path)) {
      // make sure we get no exception
    }
  }

  @Test
  void outputStreamDoubleClose() throws IOException {
    // regression test for
    // https://github.com/marschall/memoryfilesystem/issues/49
    Path path = this.extension.getFileSystem().getPath("double-close.txt");

    try (OutputStream stream = Files.newOutputStream(path, CREATE_NEW)) {
      // intentionally double close
      stream.close();
    }

    try (OutputStream stream = Files.newOutputStream(path, WRITE)) {
      // make sure we get no exception
    }
  }

  @Test
  void inputStreamNormalizePath() throws IOException {
    Path file = this.extension.getFileSystem().getPath("/file.txt");
    Files.createFile(file);
    try (InputStream inputStream = Files.newInputStream(this.extension.getFileSystem().getPath("/./file.txt/../file.txt"), READ)) {
      assertNotNull(inputStream);
    }
  }

  @Test
  void byteChannelDoubleClose() throws IOException {
    // regression test for
    // https://github.com/marschall/memoryfilesystem/issues/49
    Path path = this.extension.getFileSystem().getPath("double-close.txt");

    try (SeekableByteChannel channel = Files.newByteChannel(path, CREATE_NEW)) {
      // intentionally double close
      channel.close();
    }

    try (SeekableByteChannel channel = Files.newByteChannel(path)) {
      // make sure we get no exception
    }
  }

  /**
   * Regression test for <a href="https://github.com/marschall/memoryfilesystem/issues/47">Issue 47</a>.
   */
  @Test
  void symLinkDirectory() throws IOException {
    Path parent = this.extension.getFileSystem().getPath("/linkParent");
    Files.createDirectories(parent);
    Path target = this.extension.getFileSystem().getPath("/target");
    Files.createDirectories(target);
    Path link = parent.resolve("link");
    Files.createSymbolicLink(link, target);

    Files.write(link.resolve("kid"), "hallo".getBytes(US_ASCII));
  }

  /**
   * Regression test for <a href="https://github.com/marschall/memoryfilesystem/issues/55">Issue 55</a>.
   */
  @Test
  void unsupportedViews() {
    Path root = this.extension.getFileSystem().getPath("/");
    PosixFileAttributeView unsupportedView = Files.getFileAttributeView(root, PosixFileAttributeView.class);
    assertNull(unsupportedView);
  }

  @Test
  void channelWriteTruncateExisting() throws IOException {
    Path file = this.extension.getFileSystem().getPath("/file.txt");
    Files.createFile(file);
    assertThrows(IllegalArgumentException.class, () -> Files.newByteChannel(file, WRITE, APPEND, TRUNCATE_EXISTING), "APPEND and TRUNCATE_EXISTING and should not work");
  }

  @Test
  void channelNormalizePath() throws IOException {
    Path file = this.extension.getFileSystem().getPath("/file.txt");
    Files.createFile(file);
    try (SeekableByteChannel channel = Files.newByteChannel(this.extension.getFileSystem().getPath("/./file.txt/../file.txt"), WRITE)) {
      assertNotNull(channel);
    }
  }

  @Test
  void outputStreamWriteTruncateExisting() throws IOException {
    Path file = this.extension.getFileSystem().getPath("/file.txt");
    Files.createFile(file);
    assertThrows(IllegalArgumentException.class, () -> Files.newOutputStream(file, APPEND, TRUNCATE_EXISTING), "APPEND and TRUNCATE_EXISTING and should not work");
  }

  @Test
  void outputStreamNormalizePath() throws IOException {
    Path file = this.extension.getFileSystem().getPath("/file.txt");
    Files.createFile(file);
    try (OutputStream outputStream = Files.newOutputStream(this.extension.getFileSystem().getPath("/./file.txt/../file.txt"), WRITE)) {
      assertNotNull(outputStream);
    }
  }

  @Test
  void fromUriSingleSlash() throws IOException {
    Path root = this.extension.getFileSystem().getPath("/root");
    Files.createFile(root);
    Path singleSlash = Paths.get(URI.create("memory:name:/root"));
    Path doubleSlash = Paths.get(URI.create("memory:name:///root"));

    assertThat(singleSlash, isSameFile(root));
    assertThat(doubleSlash, isSameFile(root));
  }

  @Test
  void toUriDifferentFileSystem() {
    URI uri = URI.create("file:///etc/passwd");
    assertThrows(IllegalArgumentException.class,
            () -> this.extension.getFileSystem().provider().getPath(uri),
            () -> "URI " + uri + " should be invalid");
  }

  @Test
  void anonymousFileSystems() throws IOException {
    try (FileSystem first = MemoryFileSystemBuilder.newEmpty().build()) {
      try (FileSystem second = MemoryFileSystemBuilder.newEmpty().build()) {
        assertNotSame(first, second);
      }

    }
  }

  static final class CompletionHandlerStub<V, A> implements CompletionHandler<V, A> {

    private volatile V result;
    private volatile A attachment;
    private volatile boolean completed;
    private volatile Throwable exception;
    private volatile boolean failed;
    private final CountDownLatch countDownLatch;

    CompletionHandlerStub() {
      this.countDownLatch = new CountDownLatch(1);
    }

    @Override
    public void completed(V result, A attachment) {
      this.result = result;
      this.attachment = attachment;
      this.completed = true;
      this.countDownLatch.countDown();
    }

    @Override
    public void failed(Throwable exception, A attachment) {
      this.exception = exception;
      this.attachment = attachment;
      this.failed = true;
      this.countDownLatch.countDown();
    }

    void await() throws InterruptedException {
      this.countDownLatch.await();
    }

    V getResult() {
      return this.result;
    }

    A getAttachment() {
      return this.attachment;
    }

    boolean isCompleted() {
      return this.completed;
    }

    Throwable getException() {
      return this.exception;
    }

    boolean isFailed() {
      return this.failed;
    }

  }


}
