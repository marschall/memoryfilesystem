/**
 * Provides an in-memory implementation of a
 * <a href="http://jcp.org/en/jsr/detail?id=203">JSR-203</a> file system.
 *
 * <p>All public classes in this package except
 * {@link com.github.marschall.memoryfilesystem.MemoryFileSystemProvider}
 * can be used by client code.</p>
 *
 * <p>Using {@link com.github.marschall.memoryfilesystem.MemoryFileSystemBuilder}
 * is the recommended way to create instances. For example like this:</p>
 *
 * <pre><code>
 * try (FileSystem fs = MemoryFileSystemBuilder.newEmpty().build("name")) {
 *   Path path = fs.getPath("/");
 * }
 * </code></pre>
 */
package com.github.marschall.memoryfilesystem;
