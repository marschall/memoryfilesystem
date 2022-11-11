Memory File System [![Build Status](https://app.travis-ci.com/marschall/memoryfilesystem.svg?branch=master)](https://app.travis-ci.com/marschall/memoryfilesystem) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.marschall/memoryfilesystem/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.marschall/memoryfilesystem) [![Javadocs](https://www.javadoc.io/badge/com.github.marschall/memoryfilesystem.svg)](https://www.javadoc.io/doc/com.github.marschall/memoryfilesystem)
==================
An in memory implementation of a [JSR-203](http://jcp.org/en/jsr/detail?id=203) (Java 7) file system for testing purposes.

```xml
<dependency>
    <groupId>com.github.marschall</groupId>
    <artifactId>memoryfilesystem</artifactId>
    <version>2.4.0</version>
</dependency>
```

ToC
---
* [Supported Features](#supported)
* [Not Supported Features](#not-supported)
* [FAQ](#faq)
* [Usage](#usage)
* [Guidelines for Testable File Code](#guidelines-for-testable-file-code)

Supported
---------
* <code>SeekableByteChannel</code>
* <code>FileChannel</code>
* <code>AsynchronousFileChannel</code>
* <code>InputStream</code>
* <code>OutputStream</code>
* <code>BasicFileAttributeView</code>, <code>BasicFileAttributes</code>
* <code>DosFileAttributeView</code>, <code>DosFileAttributes</code>
* <code>PosixFileAttributeView</code>, <code>PosixFileAttributes</code>
* <code>UserDefinedFileAttributeView</code>
* <code>FileLock</code>
* <code>DirectoryStream</code>
* <code>PathMatcher</code>
  * glob
  * regex
* <code>StandardCopyOption</code>
  * REPLACE_EXISTING
  * COPY_ATTRIBUTES
  * ATOMIC_MOVE
* <code>StandardOpenOption</code>
  * READ
  * WRITE
  * TRUNCATE_EXISTING
  * CREATE
  * DELETE_ON_CLOSE
* symbolic links
* symbolic link loop detection
* hard links
* switching the current user
* switching the current group
* DOS access checks
* POSIX access checks
* [umask](http://en.wikipedia.org/wiki/Umask)

Not Supported
-------------
* `FileChannel#map`, `MappedByteBuffer` has final methods that call native methods
* `SecureDirectoryStream`
* `WatchService`
* `FileTypeDetector`, has to be accessible by system classloader
* faked DOS attribute view under Linux, totally unspecified
* `UnixFileAttributeView`, [sun package](http://www.oracle.com/technetwork/java/faq-sun-packages-142232.html), totally unspecified
* `AclFileAttributeView`
* files larger than 16MB
* `StandardOpenOption`
  * SPARSE
  * SYNC
  * DSYNC
* `URL` interoperability, needs a custom `URLStreamHandler` which [ins't very nice](http://www.unicon.net/node/776). That means you can't for example create an `URLClassLoader` on a memory file system. However if you really want to create a `ClassLoader` on a memory file system you can use [path-classloader](https://github.com/marschall/path-classloader) which is completely portable across Java 7 file systems.
* maximum path length checks
* hard link count checks

Version History
---------------

Version 2 requires Java 8 and supports nanosecond time resolution. Automatically set mtime, atime and ctime will have nanosecond resolution only with Java 9+.

Version 1 requires Java 7.

FAQ
---
### Does it have bugs?
Quite likely.

### What license is it?
MIT

### Does it support concurrent access?
Yes, but hasn't been subject to much scrutiny so bugs are likely. 

### Does it work with the zipfs provider?
Not with the one that ships with the JDK 7 because of [bug 8004789](http://bugs.sun.com/view_bug.do?bug_id=8004789). However there's a [repackaged version](https://github.com/marschall/zipfilesystem-standalone) that fixes this bug and is compatible. It should work fine in JDK 8.

### Is it production ready?
No, it's only intended for testing purposes.

### Does it scale?
No

### Does it have any dependencies?
No

### Does it support JDK 9?
Yes, starting from version 0.9.2 the JAR is a [modular](https://github.com/marschall/memoryfilesystem/blob/master/src/main/java/module-info.java) JAR with the name `com.github.marschall.memoryfilesystem`. The only module required besides `java.base` is `java.annotation` which is [optional](http://openjdk.java.net/projects/jigsaw/spec/issues/#CompileTimeDependences).

### Does it work with Spring?
Yes, there is a POJO factory bean. It has been tested with Spring 3.2.4 but since it doesn't have any dependencies on Spring it should work with every ⩾ 2.x version. You can of course also use Java configuration or any other IoC container.

### Does it work with OSGi?
Yes, it's a bundle and there's an activator that prevents class loader leaks. You should use the `MemoryFileSystemBuilder` instead of `FileSystems#newFileSystem` because `ServiceLoader` uses the thread context class loader. `MemoryFileSystemBuilder` avoids this by passing in the correct class loader.

### Does it do any logging?
No

### But I want all my file access logged
A logging file system that wraps an other file system is the best way to do this.

### How can I set the current user?
Use `CurrentUser#useDuring`

### How can I set the current group?
Use `CurrentGroup#useDuring`

### Can I run Lucene?
Yes, starting with version 2.1 running Lucene is supported, see [LuceneRegressionTest](https://github.com/marschall/memoryfilesystem/blob/master/src/test/java/com/github/marschall/memoryfilesystem/LuceneRegressionTest.java). It is important you use the `#newLinux()` method on `MemoryFileSystemBuilder`.

### Are there other similar projects?
Yes, [google/jimfs](https://github.com/google/jimfs), [openCage/memoryfs](https://github.com/openCage/memoryfs) and [sbridges/ephemeralfs](https://github.com/sbridges/ephemeralfs) seem similar.

### How does this compare to ShrinkWrap NIO.2?
[ShrinkWrap NIO.2](http://exitcondition.alrubinger.com/2012/08/17/shrinkwrap-nio2/) seems to be mainly targeted at interacting with a ShrinkWrap archive instead of simulating a file system.

Usage
-----
### Getting Started
The easiest way to get started is to use the `MemoryFileSystemBuilder`

```java
try (FileSystem fileSystem = MemoryFileSystemBuilder.newEmpty().build()) {
  Path p = fileSystem.getPath("p");
  System.out.println(Files.exists(p));
}
```

It's important to know that at any given time there can only be one memory file system with a given name. Any attempt to create a memory file system with the name of an existing one will throw an exception. 

There are other `new` methods on `MemoryFileSystemBuilder` that allow you to create different file systems and other methods that allow you to customize the file system.

### Next Steps JUnit 4
You probably want to create a JUnit `TestRule` that sets up and tears down a file system for you. A rule can look like this

```java
final class FileSystemRule implements TestRule {

  private FileSystem fileSystem;

  FileSystem getFileSystem() {
    return this.fileSystem;
  }

  @Override
  public Statement apply(final Statement base, Description description) {
    return new Statement() {

      @Override
      public void evaluate() throws Throwable {
        fileSystem = MemoryFileSystemBuilder.newEmpty().build();
        try {
          base.evaluate();
        } finally {
          fileSystem.close();
        }
      }

    };
  }

}
```

and is used like this
```java
public class FileSystemTest {

  @Rule
  public final FileSystemRule rule = new FileSystemRule();

  @Test
  public void lockAsyncChannel() throws IOException {
    FileSystem fileSystem = this.rule.getFileSystem();

    Path path = fileSystem.getPath("sample.txt");
    assertFalse(Files.exists(path));
  }

}
```

It's important to note that the field holding the rule must be public.

### Next Steps JUnit 5
You probably want to create a JUnit extension that sets up and tears down a file system for you. A rule can look like this

```java
class FileSystemExtension implements BeforeEachCallback, AfterEachCallback {

  private FileSystem fileSystem;

  FileSystem getFileSystem() {
    return this.fileSystem;
  }

  @Override
  public void beforeEach(ExtensionContext context) throws Exception {
    this.fileSystem = MemoryFileSystemBuilder.newEmpty().build("name");
  }

  @Override
  public void afterEach(ExtensionContext context) throws Exception {
    if (this.fileSystem != null) {
      this.fileSystem.close();
    }
  }
}
```

and is used like this
```java
class FileSystemTest {

  @RegisterExtension
  final FileSystemExtension extension = new FileSystemExtension();

  @Test
  public void lockAsyncChannel() throws IOException {
    FileSystem fileSystem = this.extension.getFileSystem();

    Path path = fileSystem.getPath("sample.txt");
    assertFalse(Files.exists(path));
  }

}
```

If you're using an IoC container for integration tests check out the section below.

### Spring
The `com.github.marschall.memoryfilesystem.MemoryFileSystemFactoryBean` provides integration with Spring.

```xml
  <bean id="memoryFileSystemFactory"
      class="com.github.marschall.memoryfilesystem.MemoryFileSystemFactoryBean"/>

  <bean id="memoryFileSystem" destroy-method="close"
    factory-bean="memoryFileSystemFactory" factory-method="getObject"/>
```

You can of course also write a [Java Configuration](http://static.springsource.org/spring/docs/4.0.x/spring-framework-reference/html/beans.html#beans-java) class and a `@Bean` method that uses `MemoryFileSystemBuilder` to create a new file system. Or a CDI class with a `@Produces` method that uses `MemoryFileSystemBuilder` to create a new file system. 

By setting the "type" attribute to "windows", "linux" or "macos" you can control the semantics of the created file system.

For more information check out the [Javadoc](http://www.javadoc.io/doc/com.github.marschall/memoryfilesystem).

Guidelines for Testable File Code
================================

The following guidelines are designed to help you write code that can easily be tested using this project. In general code using the old `File` API has to moved over to the new Java 7 API.

* Inject a `Path` or `FileSystem` instance into the object doing the file handling. This allows you to pass in an instance of a memory file system when testing and an instance of the default file system when running in production. You can always the the file system of a path by using `Path#getFileSystem()`.
* Don't use `File`, `FileInputStream`, `FileOutputStream`, `RandomAccessFile` and `Path#toFile()`. These classes are hard wired to the default file system.
  * Use `Path` instead of `File`.
  * Use `SeekableByteChannel` instead of `RandomAccessFile`. Use `Files#newByteChannel` to create an instance of `SeekableByteChannel`.
  * Use `Files#newInputStream` and `Files#newOutputStream` to create `InputStream`s and `OutputStream`s on files.
  * Use `FileChannel#open` instead of `FileInputStream#getChannel()`, `FileOutputStream#getChannel()`, or `RandomAccessFile#getChannel()` to create a `FileChannel`
* Use `FileSystem#getPath(String, String...)` instead of `Paths#get(String, String...)` to create a `Path` instance because the latter creates an instance on the default file system.


Building
--------

The project requires that JAVA_HOME is set to a JDK 11 or a [toolchain](https://maven.apache.org/guides/mini/guide-using-toolchains.html) with version 11 is set up.
