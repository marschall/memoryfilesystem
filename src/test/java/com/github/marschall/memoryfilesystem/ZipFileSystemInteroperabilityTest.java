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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class ZipFileSystemInteroperabilityTest {

  private static final String FS_URI = "jar:";

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
  @Disabled("broken")
  void createNestedZips() throws IOException {
    FileSystem memoryFileSystem = this.extension.getFileSystem();
    Map<String, String> env = Collections.singletonMap("create", "false");
    Path outerZip = memoryFileSystem.getPath("/file.zip");
    try (OutputStream stream = new JarOutputStream(Files.newOutputStream(outerZip, CREATE_NEW, WRITE))) {
      // nothing, just create an empty jar
    }
    URI uri = URI.create(FS_URI + outerZip.toUri());
    try (FileSystem zipfs = FileSystems.newFileSystem(uri, env)) {
      Path innerZip = zipfs.getPath("hello.zip");
      try (OutputStream stream = new JarOutputStream(Files.newOutputStream(innerZip, CREATE_NEW, WRITE))) {
        // nothing, just create an empty jar
      }
      Map<String, String> env2 = Collections.singletonMap("create", "false");
      // locate file system by using the syntax
      // defined in java.net.JarURLConnection
      URI uri2 = URI.create(FS_URI + innerZip.toUri());
      try (FileSystem zipfs2 = FileSystems.newFileSystem(uri2, env2)) {
        try (BufferedWriter writer = Files.newBufferedWriter(zipfs2.getPath("hello.txt"), US_ASCII, CREATE_NEW, WRITE)) {
          writer.write("world");
        }
      }
    }
  }

  @Test
  @Disabled("broken upstream")
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
      Map<String, String> env = Collections.singletonMap("create", "false");
      URI uri = URI.create(FS_URI + jarFile.toUri());
      try (FileSystem jarfs = FileSystems.newFileSystem(uri, env)) {
        Path p = jarfs.getPath("hello.txt");
        assertNotNull(Paths.get(p.toUri()));
      }
    } finally {
      Files.delete(jarFile);
    }
  }

  @Test
  @Disabled("broken upstream")
  void nestesJarsRegression() throws IOException {
    Path outerJar = Files.createTempFile("outer", ".jar");
    try (OutputStream stream = new JarOutputStream(Files.newOutputStream(outerJar))) {
      // nothing, just create an empty jar
    }
    try {
      Map<String, String> outerEnv = Collections.singletonMap("create", "false");
      URI outerUri = URI.create(FS_URI + outerJar.toUri());
      try (FileSystem jarfs = FileSystems.newFileSystem(outerUri, outerEnv)) {
        Path innerJar = jarfs.getPath("inner.jar");
        try (OutputStream stream = new JarOutputStream(Files.newOutputStream(innerJar, CREATE_NEW, WRITE))) {
          // nothing, just create an empty jar
        }
        Map<String, String> innerEnv = Collections.singletonMap("create", "false");
        URI innerUri = URI.create(FS_URI + innerJar.toUri());
        try (FileSystem zipfs2 = FileSystems.newFileSystem(innerUri, innerEnv)) {
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
