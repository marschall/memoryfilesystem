module com.github.marschall.memoryfilesystem {

  /*
   * Should work be apparently isn't implemented yet
   *
   * http://openjdk.java.net/projects/jigsaw/spec/issues/#CompileTimeDependences
   */
  requires /* static */ java.annotations.common;

  exports com.github.marschall.memoryfilesystem;

  provides java.nio.file.spi.FileSystemProvider
  with com.github.marschall.memoryfilesystem.MemoryFileSystemProvider;

}