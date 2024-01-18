package com.github.marschall.memoryfilesystem;


import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.file.StandardOpenOption.CREATE_NEW;
import static java.nio.file.StandardOpenOption.WRITE;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;
import java.util.jar.JarOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;
import org.junit.jupiter.api.extension.RegisterExtension;

class ZipFileSystemInteroperabilityTest {

  private static final String FS_URI = "jar:";

  // Avoid casts in FileSystems.newFileSystem(Path, ClassLoader)
  private static final ClassLoader NULL_CLASS_LOADER = null;
  private static final Map<String, ?> CREATE_ENV = Collections.singletonMap("create", "false");

  @RegisterExtension
  final FileSystemExtension extension = new FileSystemExtension();

  @Test
  void createZipFileSystem() throws IOException {
    FileSystem memoryFileSystem = this.extension.getFileSystem();
    Map<String, String> env = Collections.singletonMap("create", "true");
    URI uri = URI.create(FS_URI + memoryFileSystem.getPath("/file.zip").toUri());
    try (FileSystem zipfs = FileSystems.newFileSystem(uri, env)) {
      try (BufferedWriter writer = Files.newBufferedWriter(zipfs.getPath("hello.txt"), US_ASCII, CREATE_NEW, WRITE)) {
        writer.write("world");
      }
    }
  }

  @Test
  @EnabledForJreRange(min = JRE.JAVA_12, disabledReason = "nested zips only available on JDK 12+")
  void createNestedZips() throws IOException {
    FileSystem memoryFileSystem = this.extension.getFileSystem();
    Path outerZip = memoryFileSystem.getPath("/file.zip");
    try (OutputStream stream = new JarOutputStream(Files.newOutputStream(outerZip, CREATE_NEW, WRITE))) {
      // nothing, just create an empty jar
    }
    try (FileSystem zipfs = FileSystems.newFileSystem(outerZip, NULL_CLASS_LOADER)) {
      Path innerZip = zipfs.getPath("hello.zip");
      try (OutputStream stream = new JarOutputStream(Files.newOutputStream(innerZip, CREATE_NEW, WRITE))) {
        // nothing, just create an empty jar
      }
      // locate file system by using the syntax
      // defined in java.net.JarURLConnection
      try (FileSystem zipfs2 = FileSystems.newFileSystem(innerZip, NULL_CLASS_LOADER)) {
        try (BufferedWriter writer = Files.newBufferedWriter(zipfs2.getPath("hello.txt"), US_ASCII, CREATE_NEW, WRITE)) {
          writer.write("world");
        }
      }
    }
  }

  @Test
  @EnabledForJreRange(min = JRE.JAVA_9, disabledReason = "zip FS path to URI conversion works on JDK 9+")
  void jarToUriRegression() throws IOException {
    Path jarFolder = Files.createTempDirectory("jartest");
    try {
      Path jarFile = jarFolder.resolve("test.jar");
      try {
        Map<String, String> env = Collections.singletonMap("create", "true");
        URI uri = URI.create(FS_URI + jarFile.toUri());
        try (FileSystem jarfs = FileSystems.newFileSystem(uri, env)) {
          Path p = jarfs.getPath("hello.txt");
          assertNotNull(Paths.get(p.toUri()));
        }
      } finally {
        Files.delete(jarFile);
      }
    } finally {
      Files.delete(jarFolder);
    }
  }

  @Test
  void jarToUriRegressionFixed() throws IOException {
    Path jarFile = Files.createTempFile(null, ".jar");
    try (OutputStream stream = new JarOutputStream(Files.newOutputStream(jarFile))) {
      // nothing, just create an empty jar
    }
    try {
      URI uri = URI.create(FS_URI + jarFile.toUri());
      try (FileSystem jarfs = FileSystems.newFileSystem(uri, CREATE_ENV)) {
        Path p = jarfs.getPath("hello.txt");
        assertNotNull(Paths.get(p.toUri()));
      }
    } finally {
      Files.delete(jarFile);
    }
  }

  @Test
  @EnabledForJreRange(min = JRE.JAVA_12, disabledReason = "nested zips only available on JDK 12+")
  void nestedJarsRegression() throws IOException {
    Path outerJar = Files.createTempFile("outer", ".jar");
    try (OutputStream stream = new JarOutputStream(Files.newOutputStream(outerJar))) {
      // nothing, just create an empty jar
    }
    try {
      try (FileSystem jarfs = FileSystems.newFileSystem(outerJar, NULL_CLASS_LOADER)) {
        Path innerJar = jarfs.getPath("inner.jar");
        try (OutputStream stream = new JarOutputStream(Files.newOutputStream(innerJar, CREATE_NEW, WRITE))) {
          // nothing, just create an empty jar
        }
        try (FileSystem zipfs2 = FileSystems.newFileSystem(innerJar, NULL_CLASS_LOADER)) {
          try (BufferedWriter writer = Files.newBufferedWriter(zipfs2.getPath("hello.txt"), US_ASCII, CREATE_NEW, WRITE)) {
            writer.write("world");
          }
        }
      }
    } finally {
      Files.delete(outerJar);
    }
  }

}
