package com.github.marschall.memoryfilesystem;

import java.nio.file.FileSystem;
import java.nio.file.FileSystems;

import org.junit.Assume;
import static org.junit.Assume.assumeTrue;
import org.junit.runner.RunWith;
import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;

@RunWith(Theories.class)
public class UnixFileSystemComptiblityTest {
  
  @DataPoint
  public static final FileSystem defaultFileSystem = FileSystems.getDefault();

  @Theory
  public void s() {
    assumeTrue(defaultFileSystem.supportedFileAttributeViews().contains("posix"));
    
  }

}
