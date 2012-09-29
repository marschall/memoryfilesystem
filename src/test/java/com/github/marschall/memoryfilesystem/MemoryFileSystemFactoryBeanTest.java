package com.github.marschall.memoryfilesystem;

import java.nio.file.FileSystem;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class MemoryFileSystemFactoryBeanTest {
  
  @Autowired
  private FileSystem fileSystem;

  @Test
  public void isOpen() {
    assertTrue(this.fileSystem.isOpen());
  }

}
