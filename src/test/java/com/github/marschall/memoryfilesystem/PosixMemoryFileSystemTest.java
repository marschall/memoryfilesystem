package com.github.marschall.memoryfilesystem;

import static com.github.marschall.memoryfilesystem.FileExistsMatcher.exists;
import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.WRITE;
import static java.nio.file.attribute.PosixFilePermission.OWNER_READ;
import static java.nio.file.attribute.PosixFilePermission.OWNER_WRITE;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.junit.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.channels.ByteChannel;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.time.Instant;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;


class PosixMemoryFileSystemTest {

  @RegisterExtension
  final PosixFileSystemExtension extension = new PosixFileSystemExtension();

  @Test
  void defaultAttributes() throws IOException {
    FileSystem fileSystem = this.extension.getFileSystem();
    Path file = fileSystem.getPath("file.txt");

    Files.createFile(file);

    PosixFileAttributeView sourcePosixFileAttributeView = Files.getFileAttributeView(file, PosixFileAttributeView.class);
    PosixFileAttributes sourcePosixAttributes = sourcePosixFileAttributeView.readAttributes();
    assertNotNull(sourcePosixAttributes.permissions(), "permissions");
    assertNotNull(sourcePosixAttributes.owner(), "owner");
    assertNotNull(sourcePosixAttributes.group(), "group");
  }

  @Test
  void getOwner() throws IOException {
    FileSystem fileSystem = this.extension.getFileSystem();
    UserPrincipal owner = Files.getOwner(fileSystem.getPath("/"));
    assertNotNull(owner);
  }

  @Test
  void supportedFileAttributeViews() {
    FileSystem fileSystem = this.extension.getFileSystem();
    Set<String> actual = fileSystem.supportedFileAttributeViews();
    Set<String> expected = new HashSet<>(Arrays.asList("basic", "owner", "posix"));
    assertEquals(expected, actual);
  }

  @Test
  void copyAttributes() throws IOException {
    FileSystem fileSystem = this.extension.getFileSystem();
    Path source = fileSystem.getPath("source.txt");
    Path target = fileSystem.getPath("target.txt");

    Files.createFile(source);

    PosixFileAttributeView sourcePosixFileAttributeView = Files.getFileAttributeView(source, PosixFileAttributeView.class);

    EnumSet<PosixFilePermission> permissions = EnumSet.of(OWNER_READ, OWNER_WRITE);
    sourcePosixFileAttributeView.setPermissions(permissions);

    Files.copy(source, target, StandardCopyOption.COPY_ATTRIBUTES);

    PosixFileAttributeView targetPosixFileAttributeView = Files.getFileAttributeView(target, PosixFileAttributeView.class);
    PosixFileAttributes targetPosixAttributes = targetPosixFileAttributeView.readAttributes();
    assertEquals(permissions, targetPosixAttributes.permissions());
    assertNotSame(permissions, targetPosixAttributes.permissions());
  }

  @Test
  void dontCopyAttributes() throws IOException {
    FileSystem fileSystem = this.extension.getFileSystem();
    Path source = fileSystem.getPath("source.txt");
    Path target = fileSystem.getPath("target.txt");

    Files.createFile(source);

    PosixFileAttributeView sourcePosixFileAttributeView = Files.getFileAttributeView(source, PosixFileAttributeView.class);

    EnumSet<PosixFilePermission> permissions = EnumSet.of(OWNER_READ, OWNER_WRITE);
    sourcePosixFileAttributeView.setPermissions(permissions);

    Files.copy(source, target);

    PosixFileAttributeView targetPosixFileAttributeView = Files.getFileAttributeView(target, PosixFileAttributeView.class);
    PosixFileAttributes targetPosixAttributes = targetPosixFileAttributeView.readAttributes();
    assertNotEquals(permissions, targetPosixAttributes.permissions());
  }

  // https://bugs.openjdk.java.net/browse/JDK-8066915
  @Test
  void jdk8066915() throws IOException {
    try (FileSystem fileSystem = MemoryFileSystemBuilder.newLinux()
            .setSupportFileChannelOnDirectory(false)
            .build()) {

      Path directory = fileSystem.getPath("directory");
      Files.createDirectory(directory);

      try (ByteChannel channel = Files.newByteChannel(directory)) {
        fail("should not be able to create channel on directory");
      } catch (FileSystemException e) {
        // should reach here
        assertEquals(directory.toAbsolutePath().toString(), e.getFile(), "file");
      }

      try (ByteChannel channel = Files.newByteChannel(directory, READ)) {
        fail("should not be able to create channel on directory");

      } catch (FileSystemException e) {
        // should reach here
        assertEquals(directory.toAbsolutePath().toString(), e.getFile(), "file");
      }

      try (ByteChannel channel = Files.newByteChannel(directory, WRITE)) {
        fail("should not be able to create channel on directory");
      } catch (FileSystemException e) {
        // should reach here
        assertEquals(directory.toAbsolutePath().toString(), e.getFile(), "file");
      }
    }
  }

  @Test
  void noTruncation() throws IOException {
    FileSystem fileSystem = this.extension.getFileSystem();
    Instant mtime = Instant.parse("2019-02-27T12:37:03.123456789Z");
    Instant atime = Instant.parse("2019-02-27T12:37:03.223456789Z");
    Instant ctime = Instant.parse("2019-02-27T12:37:03.323456789Z");

    Path file = Files.createFile(fileSystem.getPath("C:\\file.txt"));
    BasicFileAttributeView view = Files.getFileAttributeView(file, BasicFileAttributeView.class);
    view.setTimes(FileTime.from(mtime), FileTime.from(atime), FileTime.from(ctime));

    BasicFileAttributes attributes = view.readAttributes();

    assertEquals(mtime, attributes.lastModifiedTime().toInstant());
    assertEquals(atime, attributes.lastAccessTime().toInstant());
    assertEquals(ctime, attributes.creationTime().toInstant());
  }

  /**
   * Regression test for <a href="https://github.com/marschall/memoryfilesystem/issues/128">Incorrect symlink target existence via Files.exists</a>
   *
   * @throws IOException if this test fails
   */
  @Test
  void issue128() throws IOException {
    FileSystem fileSystem = this.extension.getFileSystem();
    Path realFile = fileSystem.getPath("realFile");
    FileUtility.createAndSetContents(realFile, "Test");
    Path symLink = Files.createSymbolicLink(fileSystem.getPath("symLink"), realFile);

    assertThat("Real file should exist", realFile, exists());
    assertThat("Symlink file should exist without following links", symLink, exists(NOFOLLOW_LINKS));
    assertThat("Target of symlink file should exist", Files.readSymbolicLink(symLink), exists());
    assertEquals(realFile, Files.readSymbolicLink(symLink), "Target of symlink file should be real file");

    assertThat("Symlink file target should exist when following links", symLink, exists());
  }

  /**
   * Regression test for <a href="https://github.com/marschall/memoryfilesystem/issues/128">Incorrect symlink target existence via Files.exists</a>
   *
   * @throws IOException if this test fails
   */
  @Test
  void issue128Directory() throws IOException {
    FileSystem fileSystem = this.extension.getFileSystem();
    Path realDirectory = fileSystem.getPath("realDirectory");
    Files.createDirectory(realDirectory);
    Path realFile = realDirectory.resolve("realFile");
    FileUtility.createAndSetContents(realFile, "Test");
    Path symLinkDirectory = Files.createSymbolicLink(fileSystem.getPath("symLink"), realDirectory);
    Path symLinkFile = symLinkDirectory.resolve("realFile");

    assertThat("Real file should exist", realFile, exists());
    assertThat("Symlink file should not exist without following links", symLinkFile, not(exists(NOFOLLOW_LINKS)));

    assertThat("Symlink file target should exist when following links", symLinkFile, exists());
  }

  @Test
  void toRealPathWithSymlink() throws IOException {
    // /home/user/subdir/symlink -> realfile -> /home/user/subdir/realfile

    FileSystem fileSystem = this.extension.getFileSystem();
    Path subdir = fileSystem.getPath("subdir");

    Files.createDirectory(subdir);
    Files.createFile(subdir.resolve("realfile"));
    Files.createSymbolicLink(subdir.resolve("symlink"), fileSystem.getPath("realfile"));

    Path relative = fileSystem.getPath("subdir", "symlink");
    assertEquals(fileSystem.getPath("subdir", "realfile").toAbsolutePath(), relative.toRealPath());
    assertEquals(fileSystem.getPath("subdir", "symlink").toAbsolutePath(), relative.toRealPath(NOFOLLOW_LINKS));
  }

  @Test
  void toRealPathWithSymlinkInTheMiddle() throws IOException {
    // /home/user/subdir1/symlink -> subdir2
    // /home/user/subdir1/symlink/file -> /home/user/subdir1/subdir2/file

    FileSystem fileSystem = this.extension.getFileSystem();
    Path subdir1 = fileSystem.getPath("subdir1");
    Path subdir2 = subdir1.resolve("subdir2");
    Path realfile = subdir2.resolve("realfile");

    Files.createDirectories(subdir2);
    Files.createFile(realfile);

    Files.createSymbolicLink(subdir1.resolve("symlink"), fileSystem.getPath("subdir2"));

    Path relative = fileSystem.getPath("subdir1", "symlink", "realfile");
    assertEquals(fileSystem.getPath("subdir1", "subdir2", "realfile").toAbsolutePath(), relative.toRealPath());
    //    assertEquals(fileSystem.getPath("subdir1", "symlink", "realfile").toAbsolutePath(), relative.toRealPath(NOFOLLOW_LINKS));
  }

  @Test
  void testFileExistenceInSubfolderWithFollowingLinks() throws IOException {
    FileSystem fileSystem = this.extension.getFileSystem();
    // dir/symLink -> dir/realFile
    Path dir = Files.createDirectories(fileSystem.getPath("dir"));
    Path realFile = dir.resolve("realFile");
    FileUtility.createAndSetContents(realFile, "Test");
    Path symLink = Files.createSymbolicLink(dir.resolve("symLink"), realFile);

    // dir/realFile
    assertThat("Real file should exist", realFile, exists());
    // dir/symLink
    assertThat("Symlink file should exist without following links", symLink, exists(NOFOLLOW_LINKS));
    // dir/realFile
    assertThat("Target of symlink file should exist", Files.readSymbolicLink(symLink), exists());
    assertEquals(realFile, Files.readSymbolicLink(symLink), "Target of symlink file should be a real file");

    // dir/dir/symLink
    assertThat("Symlink file target in subfolder should exist when following links", symLink, not(exists()));
  }

}
