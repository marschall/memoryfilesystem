Memory File System [![Build Status](https://travis-ci.org/marschall/memoryfilesystem.png?branch=master)](https://travis-ci.org/marschall/memoryfilesystem)
==================
An in memory implementation of a [JSR-203](http://jcp.org/en/jsr/detail?id=203) (Java 7) file system for testing purposes.

```xml
<dependency>
    <groupId>com.github.marschall</groupId>
    <artifactId>memoryfilesystem</artifactId>
    <version>0.1.0</version>
</dependency>
```


Supported
---------
* <code>SeekableByteChannel</code>
* <code>FileChannel</code>
* <code>AsynchronousFileChannel</code>
* <code>UserDefinedFileAttributeView</code>
* <code>InputStream</code>
* <code>OutputStream</code>
* <code>BasicFileAttributeView</code>, <code>BasicFileAttributes</code>
* <code>DosFileAttributeView</code>, <code>DosFileAttributes</code>
* <code>FileOwnerAttributeView</code>
* <code>PosixFileAttributeView</code>, <code>PosixFileAttributes</code>
* <code>UserDefinedFileAttributeView</code>
* <code>FileLock</code>
* <code>PathMatcher</code>
  * glob
  * regex

Not Supported
-------------
* <code>FileChannel#map</code>, <code>MappedByteBuffer</code> has final methods that call native methods
* <code>WatchService</code>
* <code>FileTypeDetector</code>
* faked DOS attribute view under Linux, totally unspecified
* <code>UnixFileAttributeView</code>, [sun package](http://www.oracle.com/technetwork/java/faq-sun-packages-142232.html), totally unspecified
* any meaningful access checks
* files larger than 16MB

FAQ
---
### Does it have bugs?
Quite likely.

### What license is it?
MIT

### Does it support concurrent access?
Yes, but hasn't been subject much scrutiny so bugs are likely. 

### Does it work with the zipfs provider?
No, see http://bugs.sun.com/view_bug.do?bug_id=8004789

### Is it production ready?
No, it's only intended for testing purposes.

### Does it scale?
No

### Does it have any dependencies?
No

### Does it work with Spring?
Yes, there is a POJO factory bean. It has been tested with Spring 3.1.3 but since it doesn't have any dependencies on Spring it should work with every ⩾ 2.x version. You can of course also use Java configuration or any other IoC container.

### Does it work with OSGi?
Yes, it's a bundle and there's an activator that prevents class loader leaks. You should use the `MemoryFileSystemBuilder` instead of `FileSystems#newFileSystem` because `ServiceLoader` uses the thread context class loader. `MemoryFileSystemBuilder` avoids this by passing in the correct class loader.

### Does it do any logging?
No

### But I want all my file access logged
A logging file system that wraps an other file system is the best way to do this.

Usage
-----
### Getting Started
The easiest way to get started is to use the `MemoryFileSystemBuilder`

```java
try (FileSystem fileSystem = MemoryFileSystemBuilder.newEmpty().build("test")) {
  Path p = fileSystem.getPath("p");
  System.out.println(Files.exists(p));
}
```

### Next Steps
You probably want to create a JUnit `TestRule` that sets up and tears down a file system for you.

### Spring
The `com.github.marschall.memoryfilesystem.MemoryFileSystemFactoryBean` provides integration with Spring.

```xml
  <bean id="memoryFileSystemFactory"
      class="com.github.marschall.memoryfilesystem.MemoryFileSystemFactoryBean">
    <property name="name" value="test" />
  </bean>

  <bean id="memoryFileSystem" destroy-method="close"
    factory-bean="memoryFileSystemFactory" factory-method="getObject" />
```

