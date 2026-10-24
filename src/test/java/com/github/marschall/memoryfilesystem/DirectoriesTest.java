package com.github.marschall.memoryfilesystem;

import static java.nio.file.StandardCopyOption.COPY_ATTRIBUTES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.UserDefinedFileAttributeView;

import org.junit.jupiter.api.Test;

class DirectoriesTest {

  @Test
  void basicCopy() throws IOException {
    try (FileSystem sourceFileSystem = MemoryFileSystemBuilder.newEmpty().build("source")) {
      try (FileSystem targetFileSystem = MemoryFileSystemBuilder.newEmpty().build("target")) {

        Files.createDirectory(sourceFileSystem.getPath("sub"));
        FileUtility.createAndSetContents(sourceFileSystem.getPath("aaa"), "bbb");
        FileUtility.createAndSetContents(sourceFileSystem.getPath("sub/ccc"), "ddd");

        Directories.copyRecursive(sourceFileSystem.getPath("/"), targetFileSystem.getPath("/"));

        assertThat(targetFileSystem.getPath("aaa")).exists();
        assertThat(targetFileSystem.getPath("aaa")).hasContent("bbb");

        assertThat(targetFileSystem.getPath("sub/ccc")).exists();
        assertThat(targetFileSystem.getPath("sub/ccc")).hasContent("ddd");
      }
    }
  }


  @Test
  void copyUserDefinedAttributes() throws IOException {
    try (FileSystem sourceFileSystem = MemoryFileSystemBuilder.newEmpty().addFileAttributeView(UserDefinedFileAttributeView.class).build("source")) {
      try (FileSystem targetFileSystem = MemoryFileSystemBuilder.newEmpty().addFileAttributeView(UserDefinedFileAttributeView.class).build("target")) {
        Path sourceFile = Files.createFile(sourceFileSystem.getPath("file"));
        UserDefinedFileAttributeView attributeView = Files.getFileAttributeView(sourceFile, UserDefinedFileAttributeView.class);
        attributeView.write("1", ByteBuffer.wrap(new byte[]{1}));
        attributeView.write("2", ByteBuffer.wrap(new byte[]{2, 2}));
        assertThat(attributeView.list()).containsExactlyInAnyOrder("1", "2");

        Directories.copyRecursive(sourceFileSystem.getPath("/"), targetFileSystem.getPath("/"), COPY_ATTRIBUTES);

        Path targetFile = targetFileSystem.getPath("file");
        attributeView = Files.getFileAttributeView(targetFile, UserDefinedFileAttributeView.class);

        assertThat(attributeView.list()).containsExactlyInAnyOrder("1", "2");
        assertArrayEquals(new byte[]{1}, readAttribute("1", attributeView));
        assertArrayEquals(new byte[]{2, 2}, readAttribute("2", attributeView));
      }
    }
  }

  private static byte[] readAttribute(String name, UserDefinedFileAttributeView attributeView) throws IOException {
    byte[] data = new byte[attributeView.size(name)];
    ByteBuffer buffer = ByteBuffer.wrap(data);
    attributeView.read(name, buffer);
    return data;
  }

}
