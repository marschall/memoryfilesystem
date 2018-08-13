package com.github.marschall.memoryfilesystem;

interface AutoRelease extends AutoCloseable {


  @Override
  void close();

}
