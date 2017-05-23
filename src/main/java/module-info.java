module com.github.marschall.memoryfilesystem {

  requires static java.xml.ws.annotation;

  exports com.github.marschall.memoryfilesystem;

  provides java.nio.file.spi.FileSystemProvider
  with com.github.marschall.memoryfilesystem.MemoryFileSystemProvider;

}