package com.github.marschall.memoryfilesystem;

import static com.github.marschall.memoryfilesystem.IsAbsoluteMatcher.isAbsolute;
import static com.github.marschall.memoryfilesystem.IsAbsoluteMatcher.isRelative;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class MemoryDirectoryStreamTest {

  @RegisterExtension
  final FileSystemExtension extension = new FileSystemExtension();

  @Test
  void directoryStreamAbsolute() throws IOException {
    FileSystem fileSystem = this.extension.getFileSystem();

    Files.createFile(fileSystem.getPath("a.java"));
    Files.createFile(fileSystem.getPath("a.cpp"));
    Files.createFile(fileSystem.getPath("a.hpp"));
    Files.createFile(fileSystem.getPath("a.c"));
    Files.createFile(fileSystem.getPath("a.h"));

    Files.createDirectory(fileSystem.getPath("d1"));
    Files.createDirectory(fileSystem.getPath("d2"));

    try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(fileSystem.getPath("/"))) {
      assertThat(directoryStream, everyItem(isAbsolute()));
    }

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

  @Test
  void absoluteGlobPattern() throws IOException {
    FileSystem fileSystem = this.extension.getFileSystem();

    Files.createDirectories(fileSystem.getPath("/root/child1"));
    Files.createDirectories(fileSystem.getPath("/root/not-child"));

    try (DirectoryStream<Path> stream = Files.newDirectoryStream(fileSystem.getPath("/root"), "/root/child*")) {
      for (Path path : stream) {
        assertEquals("child1", path.getFileName().toString());
      }
    }
  }

  @Test
  void directoryStreamRelative() throws IOException {
    FileSystem fileSystem = this.extension.getFileSystem();

    Path parent = Files.createDirectory(fileSystem.getPath("src"));
    assertThat(parent, isRelative());

    Files.createFile(parent.resolve("a.java"));
    Files.createDirectory(parent.resolve("d1"));

    try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(parent)) {
      List<String> actual = new ArrayList<>(2);
      for (Path each : directoryStream) {
        assertThat(each, isRelative());
        actual.add(each.toString());
      }
      List<String> expected = Arrays.asList("src/a.java", "src/d1");

      assertEquals(expected.size(), actual.size());

      Set<String> actualSet = new HashSet<>(actual);
      assertEquals(actualSet.size(), actual.size());
      Set<String> expectedSet = new HashSet<>(expected);

      assertEquals(expectedSet, actualSet);
    }

  }

  @Test
  void directoryStreamFollowsSymlink() throws IOException {
    FileSystem fileSystem = this.extension.getFileSystem();

    /*
     * /
     * /Volumes
     * /Volumes/Macintosh HD -> / (symlink)
     * /abc.txt
     */
    Path root = fileSystem.getRootDirectories().iterator().next();
    Path volumes = Files.createDirectory(root.resolve("Volumes"));
    Path macintoshHd = Files.createSymbolicLink(volumes.resolve("Macintosh HD"), root);
    Path abc = Files.createFile(root.resolve("abc.txt"));

    try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(macintoshHd)) {
      List<String> actual = new ArrayList<>(2);
      for (Path each : directoryStream) {
        actual.add(each.toRealPath().toString());
      }
      actual.sort(null);
      List<String> expected = Arrays.asList(
              volumes.toRealPath().toString(),
              abc.toRealPath().toString());
      assertEquals(expected, actual);
    }
  }

  @Test
  void empty() throws IOException {
    FileSystem fileSystem = this.extension.getFileSystem();

    try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(fileSystem.getPath("/"))) {
      Iterator<Path> iterator = directoryStream.iterator();

      assertFalse(iterator.hasNext());

      assertThrows(NoSuchElementException.class, iterator::next, "next() should throw");
    }

  }

  @Test
  void iteratorTwice() throws IOException {
    FileSystem fileSystem = this.extension.getFileSystem();

    Files.createFile(fileSystem.getPath("a.java"));

    try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(fileSystem.getPath("/"))) {
      for (Path each : directoryStream) {
        assertNotNull(each);
      }

      assertThrows(IllegalStateException.class, directoryStream::iterator, "iterator() can be called only once");
    }

  }

  @Test
  void alreadyClosed() throws IOException {
    FileSystem fileSystem = this.extension.getFileSystem();

    Files.createFile(fileSystem.getPath("a.java"));

    try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(fileSystem.getPath("/"))) {

      directoryStream.close();

      assertThrows(IllegalStateException.class, directoryStream::iterator, "already closed");
    }

  }

  @Test
  void close() throws IOException {
    FileSystem fileSystem = this.extension.getFileSystem();

    Files.createFile(fileSystem.getPath("a.java"));
    Files.createFile(fileSystem.getPath("b.java"));
    Files.createFile(fileSystem.getPath("c.java"));

    try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(fileSystem.getPath("/"))) {
      Iterator<Path> iterator = directoryStream.iterator();

      assertTrue(iterator.hasNext());

      directoryStream.close();

      assertFalse(iterator.hasNext());

      assertThrows(IllegalStateException.class, directoryStream::iterator, "iterator() can be called only once");
    }

  }

  @Test
  void remove() throws IOException {
    FileSystem fileSystem = this.extension.getFileSystem();

    Files.createFile(fileSystem.getPath("a.java"));

    try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(fileSystem.getPath("/"))) {
      Iterator<Path> iterator = directoryStream.iterator();

      assertThrows(UnsupportedOperationException.class, iterator::remove, "iterator.remove() should throw");
    }

  }

  // has to run on Java 7
  //  @Test
  //  void forEachRemaining() throws IOException {
  //    FileSystem fileSystem = this.rule.getFileSystem();
  //
  //    Files.createFile(fileSystem.getPath("a.java"));
  //    Files.createFile(fileSystem.getPath("b.java"));
  //    Files.createFile(fileSystem.getPath("c.java"));
  //    Files.createFile(fileSystem.getPath("d.cpp"));
  //
  //    try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(fileSystem.getPath("/"), "*.java")) {
  //      Iterator<Path> iterator = directoryStream.iterator();
  //
  //      String first = iterator.next().getFileName().toString();
  //      assertTrue(first.endsWith(".java"));
  //
  //      final Set<String> rest = new HashSet<>();
  //      iterator.forEachRemaining(new Consumer<Path>() {
  //
  //        @Override
  //        void accept(Path path) {
  //          rest.add(path.getFileName().toString());
  //        }
  //      });
  //
  //      assertThat(rest, hasSize(2));
  //      assertFalse(rest.contains(first));
  //
  //      assertFalse(iterator.hasNext());
  //    }
  //
  //  }

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

}
