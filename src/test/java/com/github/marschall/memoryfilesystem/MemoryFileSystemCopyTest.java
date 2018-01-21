package com.github.marschall.memoryfilesystem;

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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;

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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;

public class MemoryFileSystemCopyTest {

  @Rule
  public final FileSystemRule rule = new FileSystemRule();

  @Test
  public void copySymbolicLinkNoFollow() throws IOException {
    FileSystem fileSystem = this.rule.getFileSystem();

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
  public void copySymbolicLinkFollow() throws IOException {
    FileSystem fileSystem = this.rule.getFileSystem();

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
  public void copySymbolicLinkReplace() throws IOException {
    FileSystem fileSystem = this.rule.getFileSystem();

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
  public void copySymbolicLinkReplaceNoFollow() throws IOException {
    FileSystem fileSystem = this.rule.getFileSystem();

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
  public void copySameFile() throws IOException {
    FileSystem fileSystem = this.rule.getFileSystem();

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
  public void copyRootIntoItself() throws IOException {
    Path root = this.rule.getFileSystem().getPath("/");
    Files.copy(root, root);
  }

  @Test
  public void copyRootDifferentFileSystems() throws IOException {
    Path firstRoot = this.rule.getFileSystem().getPath("/");
    try (FileSystem second = MemoryFileSystemBuilder.newEmpty().build("second");) {
      Path secondRoot = second.getPath("/");
      try {
        Files.copy(firstRoot, secondRoot);
        fail("moving the root should not work");
      } catch (IOException e) {
        // expected
      }
      try {
        Files.copy(secondRoot, firstRoot);
        fail("moving the root should not work");
      } catch (IOException e) {
        // expected
      }
    }
  }

  @Test
  public void copyRoot() throws IOException {
    Path root = this.rule.getFileSystem().getPath("/");
    Path path = this.rule.getFileSystem().getPath("/a");
    try {
      Files.copy(root, path);
      fail("moving the root should not work");
    } catch (IOException e) {
      // expected
    }
    try {
      Files.copy(path, root);
      fail("moving the root should not work");
    } catch (IOException e) {
      // expected
    }
  }

  @Test
  public void copyAlreadyExists() throws IOException, ParseException {
    // copying a folder to an already existing one should throw FileAlreadyExistsException
    FileSystem fileSystem = this.rule.getFileSystem();
    Path source = fileSystem.getPath("source");
    Path target = fileSystem.getPath("target");

    Files.createDirectory(source);
    Files.createDirectory(target);

    try {
      Files.copy(source, target);
      fail("should not be able to overwrite exsiting directories");
    } catch (FileAlreadyExistsException e) {
      // should reach here
      assert(true);
    }

  }

  @Test
  public void copyAlreadyExistsNotEmpty() throws IOException, ParseException {
    // copying a folder to an already existing one that is not empty should throw DirectoryNotEmptyException
    FileSystem fileSystem = this.rule.getFileSystem();
    Path source = fileSystem.getPath("source");
    Path target = fileSystem.getPath("target");
    Path child = fileSystem.getPath("target/child.txt");

    Files.createDirectory(source);
    Files.createDirectory(target);
    Files.createFile(child);

    try {
      Files.copy(source, target, REPLACE_EXISTING);
      fail("should not be able to overwrite non-empty directories");
    } catch (DirectoryNotEmptyException e) {
      // should reach here
      assert(true);
    }

  }

  @Test
  public void copyOverwriteExists() throws IOException, ParseException {
    // copying a folder to an already existing one should work with REPLACE_EXISTING
    FileSystem fileSystem = this.rule.getFileSystem();
    Path source = fileSystem.getPath("source");
    Path target = fileSystem.getPath("target");

    Files.createDirectory(source);
    Files.createDirectory(target);

    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    FileTime sourceTime = FileTime.fromMillis(format.parse("2012-11-07T20:30:22").getTime());
    FileTime targetTime = FileTime.fromMillis(format.parse("2012-10-07T20:30:22").getTime());
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
  public void copyAttributes() throws IOException, ParseException {
    FileSystem fileSystem = this.rule.getFileSystem();
    Path source = fileSystem.getPath("/source.txt");
    Path target = fileSystem.getPath("/target.txt");

    Files.createFile(source);

    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    FileTime lastModifiedTime = FileTime.fromMillis(format.parse("2012-11-07T20:30:22").getTime());
    FileTime lastAccessedTime = FileTime.fromMillis(format.parse("2012-10-07T20:30:22").getTime());
    FileTime createTime = FileTime.fromMillis(format.parse("2012-09-07T20:30:22").getTime());

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
  public void copyNoExisitingNoAttributes() throws IOException {
    FileSystem fileSystem = this.rule.getFileSystem();
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
  public void copyAcrossFileSystems() throws IOException {
    FileSystem source = this.rule.getFileSystem();
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
  public void copyReplaceExisitingNoAttributes() throws IOException {
    FileSystem fileSystem = this.rule.getFileSystem();
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
  public void moveRootIntoItself() throws IOException {
    Path root = this.rule.getFileSystem().getPath("/");
    Files.move(root, root);
  }

  @Test(expected = FileSystemException.class)
  public void moveRootIntoSubfolder() throws IOException {
    Path dir = Files.createDirectory(this.rule.getFileSystem().getPath("/dir"));
    Path sub = dir.resolve("sub");
    Files.move(dir, sub);
  }

  public void moveRoot() throws IOException {
    Path root = this.rule.getFileSystem().getPath("/");
    Path path = this.rule.getFileSystem().getPath("/a");
    try {
      Files.move(root, path);
      fail("moving the root should not work");
    } catch (IOException e) {
      // expected
    }
    try {
      Files.move(path, root);
      fail("moving the root should not work");
    } catch (IOException e) {
      // expected
    }
  }



  @Test
  public void moveRootDifferentFileSystems() throws IOException {
    Path firstRoot = this.rule.getFileSystem().getPath("/");
    try (FileSystem second = MemoryFileSystemBuilder.newEmpty().build("second");) {
      Path secondRoot = second.getPath("/");
      try {
        Files.move(firstRoot, secondRoot);
        fail("moving the root should not work");
      } catch (IOException e) {
        // expected
      }
      try {
        Files.move(secondRoot, firstRoot);
        fail("moving the root should not work");
      } catch (IOException e) {
        // expected
      }
    }
  }

  @Test
  public void moveToDifferentParent() throws IOException {
    FileSystem fileSystem = this.rule.getFileSystem();

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
  public void renameFile() throws IOException {
    FileSystem fileSystem = this.rule.getFileSystem();

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
  public void moveSameFile() throws IOException {
    FileSystem fileSystem = this.rule.getFileSystem();

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
  public void moveAlreadyExistsNotEmpty() throws IOException, ParseException {
    // moving a folder to an already existing one that is not empty should throw DirectoryNotEmptyException
    FileSystem fileSystem = this.rule.getFileSystem();
    Path source = fileSystem.getPath("source");
    Path target = fileSystem.getPath("target");
    Path child = fileSystem.getPath("target/child.txt");

    Files.createDirectory(source);
    Files.createDirectory(target);
    Files.createFile(child);

    try {
      Files.move(source, target, REPLACE_EXISTING);
      fail("should not be able to overwrite non-empty directories");
    } catch (DirectoryNotEmptyException e) {
      // should reach here
      assert(true);
    }

  }

  @Test
  public void moveAlreadyExists() throws IOException, ParseException {
    // moving a folder to an already existing one should throw FileAlreadyExistsException
    FileSystem fileSystem = this.rule.getFileSystem();
    Path source = fileSystem.getPath("source");
    Path target = fileSystem.getPath("target");

    Files.createDirectory(source);
    Files.createDirectory(target);

    try {
      Files.move(source, target);
      fail("should not be able to overwrite exsiting directories");
    } catch (FileAlreadyExistsException e) {
      // should reach here
      assert(true);
    }

  }

  @Test
  public void moveOverwriteExists() throws IOException, ParseException {
    // moving a folder to an already existing one should work with REPLACE_EXISTING
    FileSystem fileSystem = this.rule.getFileSystem();
    Path source = fileSystem.getPath("source");
    Path target = fileSystem.getPath("target");

    Files.createDirectory(source);
    Files.createDirectory(target);

    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    FileTime sourceTime = FileTime.fromMillis(format.parse("2012-11-07T20:30:22").getTime());
    FileTime targetTime = FileTime.fromMillis(format.parse("2012-10-07T20:30:22").getTime());
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
  public void moveDifferentFileSystem() throws IOException {
    FileSystem source = this.rule.getFileSystem();
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
  public void moveNoExisitingNoAttributes() throws IOException {
    FileSystem fileSystem = this.rule.getFileSystem();
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
  public void moveReplaceExisitingNoAttributes() throws IOException {
    FileSystem fileSystem = this.rule.getFileSystem();
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
  public void moveNonEmptyFolder() throws IOException {
    FileSystem fileSystem = this.rule.getFileSystem();
    Path src = fileSystem.getPath("/src");
    Path target = fileSystem.getPath("/target");

    Files.createDirectory(src);
    Files.createFile(src.resolve("file"));

    Files.move(src, target);
    assertThat(src.resolve("file"), not(exists()));
    assertThat(target.resolve("file"), exists());
  }

  @Test
  public void moveSymlink() throws IOException {
    FileSystem fileSystem = this.rule.getFileSystem();

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
