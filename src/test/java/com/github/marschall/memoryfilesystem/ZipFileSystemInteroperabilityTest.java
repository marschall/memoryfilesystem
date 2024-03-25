package com.github.marschall.memoryfilesystem;


import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.file.StandardOpenOption.CREATE_NEW;
import static java.nio.file.StandardOpenOption.WRITE;
import static org.junit.jupiter.api.condition.JRE.JAVA_12;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.jar.JarOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.extension.RegisterExtension;

class ZipFileSystemInteroperabilityTest {

  private static final String FS_URI = "jar:";

  // Avoid casts in FileSystems.newFileSystem(Path, ClassLoader)
  private static final ClassLoader NULL_CLASS_LOADER = null;

  // https://docs.oracle.com/en/java/javase/21/docs/api/jdk.zipfs/module-summary.html#zip-file-system-properties-heading
  private static final Map<String, ?> DO_CREATE_FILE_ENV = Collections.singletonMap("create", "true");

  @RegisterExtension
  final FileSystemExtension extension = new FileSystemExtension();

  @Test
  void createZipFileSystem() throws IOException {
    FileSystem memoryFileSystem = this.extension.getFileSystem();
    URI uri = URI.create(FS_URI + memoryFileSystem.getPath("/file.zip").toUri());
    try (FileSystem zipfs = FileSystems.newFileSystem(uri, DO_CREATE_FILE_ENV)) {
      try (BufferedWriter writer = Files.newBufferedWriter(zipfs.getPath("hello.txt"), US_ASCII, CREATE_NEW, WRITE)) {
        writer.write("world");
      }
    }
  }

  @Test
  @EnabledForJreRange(min = JAVA_12, disabledReason = "nested zips only available on JDK 12+")
  void createNestedZips() throws IOException {
    FileSystem memoryFileSystem = this.extension.getFileSystem();
    Path outerZip = memoryFileSystem.getPath("/file.zip");
    try (OutputStream stream = new JarOutputStream(Files.newOutputStream(outerZip, CREATE_NEW, WRITE))) {
      // nothing, just create an empty jar
    }
    try (FileSystem outerZipFs = FileSystems.newFileSystem(outerZip, NULL_CLASS_LOADER)) {
      Path innerZip = outerZipFs.getPath("hello.zip");
      try (OutputStream stream = new JarOutputStream(Files.newOutputStream(innerZip, CREATE_NEW, WRITE))) {
        // nothing, just create an empty jar
      }
      // locate file system by using the syntax
      // defined in java.net.JarURLConnection
      try (FileSystem innerZipFs = FileSystems.newFileSystem(innerZip, NULL_CLASS_LOADER)) {
        try (BufferedWriter writer = Files.newBufferedWriter(innerZipFs.getPath("hello.txt"), US_ASCII, CREATE_NEW, WRITE)) {
          writer.write("world");
        }
      }
    }
  }

}
