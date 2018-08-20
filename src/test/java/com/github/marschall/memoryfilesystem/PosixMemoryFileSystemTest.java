package com.github.marschall.memoryfilesystem;

import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.WRITE;
import static java.nio.file.attribute.PosixFilePermission.OWNER_READ;
import static java.nio.file.attribute.PosixFilePermission.OWNER_WRITE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.channels.ByteChannel;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;


public class PosixMemoryFileSystemTest {

  @Rule
  public final PosixFileSystemRule rule = new PosixFileSystemRule();

  @Test
  public void defaultAttributes() throws IOException {
    FileSystem fileSystem = this.rule.getFileSystem();
    Path file = fileSystem.getPath("file.txt");

    Files.createFile(file);

    PosixFileAttributeView sourcePosixFileAttributeView = Files.getFileAttributeView(file, PosixFileAttributeView.class);
    PosixFileAttributes sourcePosixAttributes = sourcePosixFileAttributeView.readAttributes();
    assertNotNull("permissions", sourcePosixAttributes.permissions());
    assertNotNull("owner", sourcePosixAttributes.owner());
    assertNotNull("group", sourcePosixAttributes.group());
  }

  @Test
  public void getOwner() throws IOException {
    FileSystem fileSystem = this.rule.getFileSystem();
    UserPrincipal owner = Files.getOwner(fileSystem.getPath("/"));
    assertNotNull(owner);
  }

  @Test
  public void supportedFileAttributeViews() {
    FileSystem fileSystem = this.rule.getFileSystem();
    Set<String> actual = fileSystem.supportedFileAttributeViews();
    Set<String> expected = new HashSet<>(Arrays.asList("basic", "owner", "posix"));
    assertEquals(expected, actual);
  }

  @Test
  public void copyAttributes() throws IOException {
    FileSystem fileSystem = this.rule.getFileSystem();
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
  public void dontCopyAttributes() throws IOException {
    FileSystem fileSystem = this.rule.getFileSystem();
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
  public void jdk8066915() throws IOException {
    FileSystem fileSystem = this.rule.getFileSystem();
    Path directory = fileSystem.getPath("directory");
    Files.createDirectory(directory);

    try (ByteChannel channel = Files.newByteChannel(directory)) {
      fail("should not be able to create channel on directory");
    } catch (FileSystemException e) {
      // should reach here
      assertEquals("file", directory.toAbsolutePath().toString(), e.getFile());
    }

    try (ByteChannel channel = Files.newByteChannel(directory, READ)) {
      fail("should not be able to create channel on directory");

    } catch (FileSystemException e) {
      // should reach here
      assertEquals("file", directory.toAbsolutePath().toString(), e.getFile());
    }

    try (ByteChannel channel = Files.newByteChannel(directory, WRITE)) {
      fail("should not be able to create channel on directory");
    } catch (FileSystemException e) {
      // should reach here
      assertEquals("file", directory.toAbsolutePath().toString(), e.getFile());
    }
  }

}
