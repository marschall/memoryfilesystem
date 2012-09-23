package com.google.code.memoryfilesystem;

interface AutoRelease extends AutoCloseable {


  @Override
  public void close();

}