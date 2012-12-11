Memory File System
=================
An in memory implementation of a [JSR-203](http://jcp.org/en/jsr/detail?id=203) (Java 7) file system for testing purposes.

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
* <code>FileChannel#map</code>, </code>MappedByteBuffer<code> has final methods that call native methods
* <code>WatchService</code>
* <code>FileTypeDetector</code>
* cross filesystem copy
* faked DOS view under Linux, totally unspecified
* <code>UnixFileAttributeView</code>, sun package, totally unspecified
* any meaningful access checks
* file system size
* files larger than 16MB

FAQ
---
### Does it have bugs?
Quite likely.

### Does it support concurrent access?
Yes, but hasn't been subject much scrutiny to bugs are likely. 

### Does it work with the zipfs provider?
No, see http://bugs.sun.com/view_bug.do?bug_id=8004789

### Is it production ready?
No, it's only intended for testing purposes.

### Does it scale?
No