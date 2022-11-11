package com.github.marschall.memoryfilesystem;

import static com.github.marschall.memoryfilesystem.EmptyDirectory.emptyDirectory;
import static com.github.marschall.memoryfilesystem.FileContentsMatcher.hasContents;
import static com.github.marschall.memoryfilesystem.FileExistsMatcher.exists;
import static com.github.marschall.memoryfilesystem.FileUtility.setContents;
import static com.github.marschall.memoryfilesystem.IsSameFileMatcher.isSameFile;
import static com.github.marschall.memoryfilesystem.IsSymbolicLinkMatcher.isSymbolicLink;
import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardCopyOption.COPY_ATTRIBUTES;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.text.ParseException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class MemoryFileSystemCopyTest {

  @RegisterExtension
  final FileSystemExtension extension = new FileSystemExtension();

  @Test
  void copySymbolicLinkNoFollow() throws IOException {
    FileSystem fileSystem = this.extension.getFileSystem();

    // /link -> /file
    // copy /link to /copy with follow no symlinks
    Path file = fileSystem.getPath("/").resolve("file");
    Files.createFile(file);

    Path link = file.resolveSibling("link");
    Path copy = file.resolveSibling("copy");

    Files.createSymbolicLink(link, file);
    Files.copy(link, copy, NOFOLLOW_LINKS);

    assertThat(copy, exists());
    assertThat(copy, isSymbolicLink());

    assertEquals("/file", copy.toRealPath().toString());
  }

  @Test
  void copySymbolicLinkFollow() throws IOException {
    FileSystem fileSystem = this.extension.getFileSystem();

    // /link -> /file
    // copy /link to /copy with follow symlinks
    Path file = fileSystem.getPath("/").resolve("file");
    Path copy = fileSystem.getPath("/").resolve("copy");
    Files.createFile(file);

    Path link = file.resolveSibling("link");

    Files.createSymbolicLink(link, file);
    Files.copy(link, copy);

    assertThat(copy, exists());
    assertThat(copy, not(isSymbolicLink()));
  }

  @Test
  void copySymbolicLinkReplace() throws IOException {
    FileSystem fileSystem = this.extension.getFileSystem();

    // /target -> /file1
    // copy /file2 to /target with replace existing
    Path file1 = fileSystem.getPath("/").resolve("file1");
    Path file2 = fileSystem.getPath("/").resolve("file2");
    Files.createFile(file1);
    Files.createFile(file2);

    Path target = file1.resolveSibling("target");

    Files.createSymbolicLink(target, file1);

    Files.copy(file2, target, REPLACE_EXISTING);

    assertThat(target, exists());
    assertThat(target, not(isSymbolicLink()));

    assertEquals("/target", target.toRealPath().toString());
  }

  @Test
  void copySymbolicLinkReplaceNoFollow() throws IOException {
    FileSystem fileSystem = this.extension.getFileSystem();

    // /link -> /file1
    // /target -> /file1
    // copy /link to /target with replace existing and no follow
    Path link = fileSystem.getPath("/").resolve("link");
    Path file1 = link.resolveSibling("file1");
    Files.createFile(file1);
    Files.createSymbolicLink(link, file1);

    Path target = fileSystem.getPath("/").resolve("target");
    Files.createSymbolicLink(target, link.resolveSibling("file2"));

    Files.copy(link, target, REPLACE_EXISTING, NOFOLLOW_LINKS);

    assertThat(target, exists(NOFOLLOW_LINKS));
    assertThat(target, isSymbolicLink());

    assertEquals("/file1", target.toRealPath().toString());
  }

  @Test
  void copySameFile() throws IOException {
    FileSystem fileSystem = this.extension.getFileSystem();

    Path a = fileSystem.getPath("a");
    Path b = fileSystem.getPath("./b/../a");

    FileUtility.createAndSetContents(a, "aaa");
    assertThat(a, exists());
    assertThat(b, exists());
    assertThat(a, isSameFile(b));

    Files.copy(a, b);
    assertThat(a, exists());
    assertThat(b, exists());

    assertThat(a, hasContents("aaa"));
    assertThat(b, hasContents("aaa"));
  }

  @Test
  void copyRootIntoItself() throws IOException {
    Path root = this.extension.getFileSystem().getPath("/");
    Files.copy(root, root);
  }

  @Test
  void copyRootDifferentFileSystems() throws IOException {
    Path firstRoot = this.extension.getFileSystem().getPath("/");
    try (FileSystem second = MemoryFileSystemBuilder.newEmpty().build("second")) {
      Path secondRoot = second.getPath("/");
      assertThrows(IOException.class, () -> Files.copy(firstRoot, secondRoot), "moving the root should not work");

      assertThrows(IOException.class, () -> Files.copy(secondRoot, firstRoot), "moving the root should not work");
    }
  }

  @Test
  void copyRoot() {
    Path root = this.extension.getFileSystem().getPath("/");
    Path path = this.extension.getFileSystem().getPath("/a");
    assertThrows(IOException.class, () -> Files.copy(root, path), "moving the root should not work");

    assertThrows(IOException.class, () -> Files.copy(path, root), "moving the root should not work");
  }

  @Test
  void copyAlreadyExists() throws IOException {
    // copying a folder to an already existing one should throw FileAlreadyExistsException
    FileSystem fileSystem = this.extension.getFileSystem();
    Path source = fileSystem.getPath("source");
    Path target = fileSystem.getPath("target");

    Files.createDirectory(source);
    Files.createDirectory(target);

    assertThrows(FileAlreadyExistsException.class,
            () -> Files.copy(source, target),
            "should not be able to overwrite existing directories");

  }

  @Test
  void copyAlreadyExistsNotEmpty() throws IOException {
    // copying a folder to an already existing one that is not empty should throw DirectoryNotEmptyException
    FileSystem fileSystem = this.extension.getFileSystem();
    Path source = fileSystem.getPath("source");
    Path target = fileSystem.getPath("target");
    Path child = fileSystem.getPath("target/child.txt");

    Files.createDirectory(source);
    Files.createDirectory(target);
    Files.createFile(child);

    assertThrows(DirectoryNotEmptyException.class,
            () -> Files.copy(source, target, REPLACE_EXISTING),
            "should not be able to overwrite non-empty directories");

  }

  @Test
  void copyOverwriteExists() throws IOException, ParseException {
    // copying a folder to an already existing one should work with REPLACE_EXISTING
    FileSystem fileSystem = this.extension.getFileSystem();
    Path source = fileSystem.getPath("source");
    Path target = fileSystem.getPath("target");

    Files.createDirectory(source);
    Files.createDirectory(target);

    FileTime sourceTime = FileTime.from(Instant.parse("2012-11-07T20:30:22.123456789Z"));
    FileTime targetTime = FileTime.from(Instant.parse("2012-10-07T20:30:22.987654321Z"));
    Files.setLastModifiedTime(source, sourceTime);
    Files.setLastModifiedTime(target, targetTime);

    assertEquals(sourceTime, Files.getLastModifiedTime(source));
    assertEquals(targetTime, Files.getLastModifiedTime(target));

    Files.copy(source, target, REPLACE_EXISTING, COPY_ATTRIBUTES);
    assertThat(source, exists());
    assertThat(target, exists());

    assertEquals(sourceTime, Files.getLastModifiedTime(source));
    assertEquals(sourceTime, Files.getLastModifiedTime(target));
  }

  @Test
  void copyAttributes() throws IOException, ParseException {
    FileSystem fileSystem = this.extension.getFileSystem();
    Path source = fileSystem.getPath("/source.txt");
    Path target = fileSystem.getPath("/target.txt");

    Files.createFile(source);

    FileTime lastModifiedTime = FileTime.from(Instant.parse("2012-11-07T20:30:22.111111111Z"));
    FileTime lastAccessedTime = FileTime.from(Instant.parse("2012-10-07T20:30:22.222222222Z"));
    FileTime createTime = FileTime.from(Instant.parse("2012-09-07T20:30:22.333333333Z"));

    BasicFileAttributeView sourceBasicFileAttributeView = Files.getFileAttributeView(source, BasicFileAttributeView.class);
    BasicFileAttributes sourceBasicAttributes = sourceBasicFileAttributeView.readAttributes();

    assertNotEquals(lastModifiedTime, sourceBasicAttributes.lastModifiedTime());
    assertNotEquals(lastAccessedTime, sourceBasicAttributes.lastAccessTime());
    assertNotEquals(createTime, sourceBasicAttributes.creationTime());
    sourceBasicFileAttributeView.setTimes(lastModifiedTime, lastAccessedTime, createTime);

    Files.copy(source, target, StandardCopyOption.COPY_ATTRIBUTES);

    BasicFileAttributeView targetBasicFileAttributeView = Files.getFileAttributeView(target, BasicFileAttributeView.class);
    BasicFileAttributes targetBasicAttributes = targetBasicFileAttributeView.readAttributes();
    assertEquals(lastModifiedTime, targetBasicAttributes.lastModifiedTime());
    assertEquals(lastAccessedTime, targetBasicAttributes.lastAccessTime());
    assertEquals(createTime, targetBasicAttributes.creationTime());
  }

  @Test
  void copyNoExistingNoAttributes() throws IOException {
    FileSystem fileSystem = this.extension.getFileSystem();
    Path a = fileSystem.getPath("/1/a");
    Path b = fileSystem.getPath("/2/b");
    Files.createDirectories(b.toAbsolutePath().getParent());

    FileUtility.createAndSetContents(a, "aaa");
    assertThat(a, exists());
    assertThat(b, not(exists()));

    Files.copy(a, b);
    assertThat(a, exists());
    assertThat(b, exists());

    assertThat(a, hasContents("aaa"));
    assertThat(b, hasContents("aaa"));

    setContents(a, "a1");

    assertThat(a, hasContents("a1"));
    assertThat(b, hasContents("aaa"));
  }

  @Test
  void copyAcrossFileSystems() throws IOException {
    FileSystem source = this.extension.getFileSystem();
    try (FileSystem target = MemoryFileSystemBuilder.newEmpty().build("target")) {
      Path a = source.getPath("a");
      Path b = target.getPath("b");

      FileUtility.createAndSetContents(a, "aaa");
      assertThat(a, exists());
      assertThat(b, not(exists()));

      Files.copy(a, b);
      assertThat(a, exists());
      assertThat(b, exists());

      assertThat(a, hasContents("aaa"));
      assertThat(b, hasContents("aaa"));

      setContents(a, "a1");

      assertThat(a, hasContents("a1"));
      assertThat(b, hasContents("aaa"));
    }
  }

  @Test
  void copyReplaceExsitingNoAttributes() throws IOException {
    FileSystem fileSystem = this.extension.getFileSystem();
    Path a = fileSystem.getPath("/1/a");
    Path b = fileSystem.getPath("/2/b");

    FileUtility.createAndSetContents(a, "aaa");
    FileUtility.createAndSetContents(b, "bbb");
    assertThat(a, exists());
    assertThat(b, exists());

    Files.copy(a, b, StandardCopyOption.REPLACE_EXISTING);
    assertThat(a, exists());
    assertThat(b, exists());

    assertThat(a, hasContents("aaa"));
    assertThat(b, hasContents("aaa"));

    setContents(a, "a1");

    assertThat(a, hasContents("a1"));
    assertThat(b, hasContents("aaa"));
  }

  @Test
  void issue102() throws IOException {
    FileSystem fileSystem = this.extension.getFileSystem();
    Path baseDir = Files.createDirectory(fileSystem.getPath("/anywhere"));
    Path sourceDir = Files.createDirectory(baseDir.resolve("somewhere"));
    Path targetDir = baseDir.resolve("nowhere"); // *Not* created

    Files.createFile(sourceDir.resolve("a_file")); // Key to triggering

    Files.copy(sourceDir, targetDir, COPY_ATTRIBUTES, REPLACE_EXISTING);
    assertThat(targetDir, exists());
    assertThat(targetDir, emptyDirectory());
  }

  @Test
  void moveRootIntoItself() throws IOException {
    Path root = this.extension.getFileSystem().getPath("/");
    Files.move(root, root);
  }

  @Test
  void moveRootIntoSubfolder() throws IOException {
    Path dir = Files.createDirectory(this.extension.getFileSystem().getPath("/dir"));
    Path sub = dir.resolve("sub");
    assertThrows(FileSystemException.class, () -> Files.move(dir, sub));
  }

  void moveRoot() {
    Path root = this.extension.getFileSystem().getPath("/");
    Path path = this.extension.getFileSystem().getPath("/a");
    assertThrows(IOException.class,
            () -> Files.move(root, path),
            "moving the root should not work");

    assertThrows(IOException.class,
            () -> Files.move(path, root),
            "moving the root should not work");
  }



  @Test
  void moveRootDifferentFileSystems() throws IOException {
    Path firstRoot = this.extension.getFileSystem().getPath("/");
    try (FileSystem second = MemoryFileSystemBuilder.newEmpty().build("second")) {
      Path secondRoot = second.getPath("/");
      assertThrows(IOException.class,
              () -> Files.move(firstRoot, secondRoot),
              "moving the root should not work");

      assertThrows(IOException.class,
              () -> Files.move(secondRoot, firstRoot),
              "moving the root should not work");
    }
  }

  @Test
  void moveToDifferentParent() throws IOException {
    FileSystem fileSystem = this.extension.getFileSystem();

    // move /a/c to /b/c

    Path a = fileSystem.getPath("/a");
    Files.createDirectory(a);
    Path b = fileSystem.getPath("/b");
    Files.createDirectory(b);
    Path ac = fileSystem.getPath("/a/c");
    Files.createFile(ac);
    Path bc = fileSystem.getPath("/b/c");

    assertThat(a, exists());
    assertThat(b, exists());
    assertThat(ac, exists());
    assertThat(bc, not(exists()));

    Files.move(ac, bc);
    assertThat(ac, not(exists()));
    assertThat(bc, exists());

    List<Path> aKids = new ArrayList<>(1);
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(a)) {
      for (Path path : stream) {
        aKids.add(path);
      }
    }
    assertThat(aKids, empty());

    List<Path> bKids = new ArrayList<>(1);
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(b)) {
      for (Path path : stream) {
        bKids.add(path);
      }
    }
    assertEquals(bKids, Collections.singletonList(bc));
  }

  @Test
  void renameFile() throws IOException {
    FileSystem fileSystem = this.extension.getFileSystem();

    // move /a/c to /b/c

    Path a = fileSystem.getPath("/a");
    Files.createDirectory(a);
    Path ab = fileSystem.getPath("/a/b");
    Files.createFile(ab);
    Path ac = fileSystem.getPath("/a/c");

    assertThat(a, exists());
    assertThat(ab, exists());
    assertThat(ac, not(exists()));

    Files.move(ab, ac);
    assertThat(ab, not(exists()));
    assertThat(ac, exists());

    List<Path> aKids = new ArrayList<>(1);
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(a)) {
      for (Path path : stream) {
        aKids.add(path);
      }
    }
    assertEquals(Collections.singletonList(ac), aKids);
  }

  @Test
  void moveSameFile() throws IOException {
    FileSystem fileSystem = this.extension.getFileSystem();

    Path a = fileSystem.getPath("a");
    Path b = fileSystem.getPath("./b/../a");

    FileUtility.createAndSetContents(a, "aaa");
    assertThat(a, exists());
    assertThat(b, exists());

    Files.move(a, b);
    assertThat(a, exists());
    assertThat(b, exists());

    assertThat(a, hasContents("aaa"));
    assertThat(b, hasContents("aaa"));
  }

  @Test
  void moveAlreadyExistsNotEmpty() throws IOException {
    // moving a folder to an already existing one that is not empty should throw DirectoryNotEmptyException
    FileSystem fileSystem = this.extension.getFileSystem();
    Path source = fileSystem.getPath("source");
    Path target = fileSystem.getPath("target");
    Path child = fileSystem.getPath("target/child.txt");

    Files.createDirectory(source);
    Files.createDirectory(target);
    Files.createFile(child);

    assertThrows(DirectoryNotEmptyException.class,
            () -> Files.move(source, target, REPLACE_EXISTING),
            "should not be able to overwrite non-empty directories");

  }

  @Test
  void moveAlreadyExists() throws IOException {
    // moving a folder to an already existing one should throw FileAlreadyExistsException
    FileSystem fileSystem = this.extension.getFileSystem();
    Path source = fileSystem.getPath("source");
    Path target = fileSystem.getPath("target");

    Files.createDirectory(source);
    Files.createDirectory(target);

    assertThrows(FileAlreadyExistsException.class,
            () -> Files.move(source, target),
            "should not be able to overwrite existing directories");

  }

  @Test
  void moveOverwriteExists() throws IOException, ParseException {
    // moving a folder to an already existing one should work with REPLACE_EXISTING
    FileSystem fileSystem = this.extension.getFileSystem();
    Path source = fileSystem.getPath("source");
    Path target = fileSystem.getPath("target");

    Files.createDirectory(source);
    Files.createDirectory(target);

    FileTime sourceTime = FileTime.from(Instant.parse("2012-11-07T20:30:22.123456789Z"));
    FileTime targetTime = FileTime.from(Instant.parse("2012-10-07T20:30:22.987654321Z"));
    Files.setLastModifiedTime(source, sourceTime);
    Files.setLastModifiedTime(target, targetTime);

    assertEquals(sourceTime, Files.getLastModifiedTime(source));
    assertEquals(targetTime, Files.getLastModifiedTime(target));

    Files.move(source, target, REPLACE_EXISTING);
    assertThat(source, not(exists()));
    assertThat(target, exists());

    assertEquals(sourceTime, Files.getLastModifiedTime(target));
  }

  @Test
  void moveDifferentFileSystem() throws IOException {
    FileSystem source = this.extension.getFileSystem();
    try (FileSystem target = MemoryFileSystemBuilder.newEmpty().build("target")) {
      Path a = source.getPath("a");
      Path b = target.getPath("b");

      FileUtility.createAndSetContents(a, "aaa");
      assertThat(a, exists());
      assertThat(b, not(exists()));

      Files.move(a, b);
      assertThat(a, not(exists()));
      assertThat(b, exists());

      assertThat(b, hasContents("aaa"));
    }
  }

  @Test
  void moveNoExistingNoAttributes() throws IOException {
    FileSystem fileSystem = this.extension.getFileSystem();
    Path a = fileSystem.getPath("/1/a");
    Path b = fileSystem.getPath("/2/b");
    Files.createDirectories(b.toAbsolutePath().getParent());

    FileUtility.createAndSetContents(a, "aaa");
    assertThat(a, exists());
    assertThat(b, not(exists()));

    Files.move(a, b);
    assertThat(a, not(exists()));
    assertThat(b, exists());

    assertThat(b, hasContents("aaa"));
  }

  @Test
  void moveReplaceExistingNoAttributes() throws IOException {
    FileSystem fileSystem = this.extension.getFileSystem();
    Path a = fileSystem.getPath("/1/a");
    Path b = fileSystem.getPath("/2/b");

    FileUtility.createAndSetContents(a, "aaa");
    FileUtility.createAndSetContents(b, "bbb");
    assertThat(a, exists());
    assertThat(b, exists());

    Files.move(a, b, StandardCopyOption.REPLACE_EXISTING);
    assertThat(a, not(exists()));
    assertThat(b, exists());

    assertThat(b, hasContents("aaa"));
  }

  @Test
  void moveNonEmptyFolder() throws IOException {
    FileSystem fileSystem = this.extension.getFileSystem();
    Path src = fileSystem.getPath("/src");
    Path target = fileSystem.getPath("/target");

    Files.createDirectory(src);
    Files.createFile(src.resolve("file"));

    Files.move(src, target);
    assertThat(src.resolve("file"), not(exists()));
    assertThat(target.resolve("file"), exists());
  }

  @Test
  void moveSymlink() throws IOException {
    FileSystem fileSystem = this.extension.getFileSystem();

    Path target = fileSystem.getPath("/target");
    Files.createDirectory(target);
    Path from = fileSystem.getPath("/from");
    Files.createSymbolicLink(from, target);
    Path to = fileSystem.getPath("/to");

    Files.move(from, to);
    assertThat(to, isSymbolicLink());
    assertEquals(target, to.toRealPath());
  }

}
