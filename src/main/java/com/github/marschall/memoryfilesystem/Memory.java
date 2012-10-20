package com.github.marschall.memoryfilesystem;

import java.io.IOException;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.DosFileAttributeView;
import java.nio.file.attribute.DosFileAttributes;
import java.nio.file.attribute.FileTime;

class MemoryDosFileAttributeView implements DosFileAttributeView {
  
  private final BasicFileAttributeView delegate;
  
  MemoryDosFileAttributeView(BasicFileAttributeView delegate) {
    this.delegate = delegate;
  }

  @Override
  public void setTimes(FileTime lastModifiedTime, FileTime lastAccessTime, FileTime createTime) throws IOException {
    this.delegate.setTimes(lastModifiedTime, lastAccessTime, createTime);
  }

  @Override
  public String name() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public DosFileAttributes readAttributes() throws IOException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void setReadOnly(boolean value) throws IOException {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void setHidden(boolean value) throws IOException {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void setSystem(boolean value) throws IOException {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void setArchive(boolean value) throws IOException {
    // TODO Auto-generated method stub
    
  }

}
