package com.github.marschall.memoryfilesystem;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.nio.file.FileSystem;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@ContextConfiguration(locations = "MemoryFileSystemFactoryBeanTest-context-generated.xml")
@RunWith(SpringJUnit4ClassRunner.class)
public class MemoryFileSystemFactoryBeanGeneratedNameTest {

  @Autowired
  private FileSystem fileSystem;

  @Test
  public void isOpen() {
    assertTrue(this.fileSystem.isOpen());
    assertNotEquals("memory:test:///", this.fileSystem.getPath("").toUri().toString());
  }

}
