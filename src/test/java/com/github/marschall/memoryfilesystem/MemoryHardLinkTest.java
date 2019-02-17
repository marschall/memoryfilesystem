package com.github.marschall.memoryfilesystem;

import static com.github.marschall.memoryfilesystem.FileContentsMatcher.hasContents;
import static com.github.marschall.memoryfilesystem.FileExistsMatcher.exists;
import static com.github.marschall.memoryfilesystem.FileUtility.setContents;
import static com.github.marschall.memoryfilesystem.IsSameFileMatcher.isSameFile;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.DELETE_ON_CLOSE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class MemoryHardLinkTest {

  @RegisterExtension
  final FileSystemExtension extension = new FileSystemExtension();

  @Test
  void sameContents() throws IOException {
    FileSystem fileSystem = this.extension.getFileSystem();
    Path target = Files.createFile(fileSystem.getPath("target"));
    String initialContents = "initial";
    setContents(target, initialContents);

    Path link = Files.createLink(fileSystem.getPath("link"), target);

    assertThat(target, hasContents(initialContents));
    assertThat(link, hasContents(initialContents));
    assertThat(target, isSameFile(link));
    assertThat(link, isSameFile(target));

    try (Writer writer = Files.newBufferedWriter(link, US_ASCII, APPEND)) {
      writer.write(" first");
    }

    try (Writer writer = Files.newBufferedWriter(target, US_ASCII, APPEND)) {
      writer.write(" second");
    }

    String expected = "initial first second";

    assertThat(target, hasContents(expected));
    assertThat(link, hasContents(expected));
  }

  @Test
  void explicitDeletePreserves() throws IOException {
    FileSystem fileSystem = this.extension.getFileSystem();
    Path target = Files.createFile(fileSystem.getPath("target"));
    String initialContents = "initial";
    setContents(target, initialContents);

    Path link = Files.createLink(fileSystem.getPath("link"), target);

    Files.delete(target);
    assertThat(target, not(exists()));
    assertThat(link, exists());

    assertThat(link, hasContents(initialContents));
  }

  @Test
  void implicitDeletePreserves() throws IOException {
    FileSystem fileSystem = this.extension.getFileSystem();
    Path target = Files.createFile(fileSystem.getPath("target"));
    String initialContents = "initial";
    setContents(target, initialContents);

    Path link = Files.createLink(fileSystem.getPath("link"), target);

    try (OutputStream outputStream = Files.newOutputStream(target, DELETE_ON_CLOSE)) {
      // nothing
    }
    assertThat(target, not(exists()));
    assertThat(link, exists());

    assertThat(link, hasContents(initialContents));
  }

  @Test
  void noLinksOnDirectories() throws IOException {
    FileSystem fileSystem = this.extension.getFileSystem();
    Path target = Files.createDirectory(fileSystem.getPath("target"));
    Path link = fileSystem.getPath("link");

    try {
      Files.createLink(link, target);
      fail("hard links on directories not supported");
    } catch (FileSystemException e) {
      assertEquals(link.toString(), e.getFile());
      assertEquals(target.toString(), e.getOtherFile());
    }

  }

  @Test
  void sameAttributes() throws IOException {
    FileSystem fileSystem = this.extension.getFileSystem();
    Path target = Files.createFile(fileSystem.getPath("target"));
    Path link = Files.createLink(fileSystem.getPath("link"), target);


    BasicFileAttributeView view = Files.getFileAttributeView(target, BasicFileAttributeView.class);

    FileTime mTime = FileTime.from(Instant.parse("2004-07-25T18:18:00.111111111Z"));
    FileTime aTime = FileTime.from(Instant.parse("2001-04-21T12:00:00.222222222Z"));
    FileTime cTime = FileTime.from(Instant.parse("1997-08-04T02:04:00.333333333Z"));
    view.setTimes(mTime, aTime, cTime);

    BasicFileAttributes attributes = Files.getFileAttributeView(link, BasicFileAttributeView.class).readAttributes();
    assertEquals(mTime, attributes.lastModifiedTime());
    assertEquals(aTime, attributes.lastAccessTime());
    assertEquals(cTime, attributes.creationTime());
  }

}
