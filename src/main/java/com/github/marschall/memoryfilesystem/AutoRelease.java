package com.github.marschall.memoryfilesystem;

@FunctionalInterface
interface AutoRelease extends AutoCloseable {


  @Override
  void close();

}
