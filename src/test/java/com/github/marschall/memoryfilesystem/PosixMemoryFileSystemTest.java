package com.github.marschall.memoryfilesystem;

import static java.nio.file.attribute.PosixFilePermission.OWNER_READ;
import static java.nio.file.attribute.PosixFilePermission.OWNER_WRITE;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

@Ignore("permissions not yet read")
public class PosixMemoryFileSystemTest {

  @Rule
  public final PosixFileSystemRule rule = new PosixFileSystemRule();

  @Test
  public void defaultAttributes() throws IOException {
    FileSystem fileSystem = this.rule.getFileSystem();
    Path file = fileSystem.getPath("file.txt");

    Files.createDirectories(file.toAbsolutePath().getParent());
    Files.createFile(file);

    PosixFileAttributeView sourcePosixFileAttributeView = Files.getFileAttributeView(file, PosixFileAttributeView.class);
    PosixFileAttributes sourcePosixAttributes = sourcePosixFileAttributeView.readAttributes();
    assertNotNull("permissions", sourcePosixAttributes.permissions());
    assertNotNull("owner", sourcePosixAttributes.owner());
    assertNotNull("group", sourcePosixAttributes.group());
  }

  @Test
  public void copyAttributes() throws IOException {
    FileSystem fileSystem = this.rule.getFileSystem();
    Path source = fileSystem.getPath("source.txt");
    Path target = fileSystem.getPath("target.txt");

    Files.createDirectories(source.toAbsolutePath().getParent());
    Files.createFile(source);

    PosixFileAttributeView sourcePosixFileAttributeView = Files.getFileAttributeView(source, PosixFileAttributeView.class);

    EnumSet<PosixFilePermission> permissions = EnumSet.of(OWNER_READ, OWNER_WRITE);
    sourcePosixFileAttributeView.setPermissions(permissions);

    Files.copy(source, target, StandardCopyOption.COPY_ATTRIBUTES);

    PosixFileAttributeView targetPosixFileAttributeView = Files.getFileAttributeView(target, PosixFileAttributeView.class);
    PosixFileAttributes targetDosAttributes = targetPosixFileAttributeView.readAttributes();
    assertEquals(permissions, targetDosAttributes.permissions());
    assertNotSame(permissions, targetDosAttributes.permissions());
  }

  @Test
  public void dontCopyAttributes() throws IOException {
    FileSystem fileSystem = this.rule.getFileSystem();
    Path source = fileSystem.getPath("source.txt");
    Path target = fileSystem.getPath("target.txt");

    Files.createDirectories(source.toAbsolutePath().getParent());
    Files.createFile(source);

    PosixFileAttributeView sourcePosixFileAttributeView = Files.getFileAttributeView(source, PosixFileAttributeView.class);

    EnumSet<PosixFilePermission> permissions = EnumSet.of(OWNER_READ, OWNER_WRITE);
    sourcePosixFileAttributeView.setPermissions(permissions);

    Files.copy(source, target);

    PosixFileAttributeView targetPosixFileAttributeView = Files.getFileAttributeView(target, PosixFileAttributeView.class);
    PosixFileAttributes targetDosAttributes = targetPosixFileAttributeView.readAttributes();
    assertThat(targetDosAttributes.permissions(), empty());
  }

}
