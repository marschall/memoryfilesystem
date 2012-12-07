package com.github.marschall.memoryfilesystem;

import static java.nio.file.StandardOpenOption.CREATE_NEW;
import static java.nio.file.StandardOpenOption.WRITE;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

@Ignore("zipfs is broken")
public class ZipFileSystemInteropabilityTest {

  @Rule
  public final FileSystemRule rule = new FileSystemRule();

  @Test
  public void createZipFileSystem() throws IOException {
    FileSystem memoryFileSystem = this.rule.getFileSystem();
    Map<String, String> env = Collections.singletonMap("create", "true");
    URI uri = URI.create("jar:" + memoryFileSystem.getPath("/file.zip").toUri());
    try (FileSystem zipfs = FileSystems.newFileSystem(uri, env)) {
      try (BufferedWriter writer = Files.newBufferedWriter(zipfs.getPath("hello.txt"), StandardCharsets.US_ASCII, CREATE_NEW, WRITE)) {
        writer.write("world");
      }
    }
  }

}
