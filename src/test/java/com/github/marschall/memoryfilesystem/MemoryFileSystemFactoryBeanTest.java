package com.github.marschall.memoryfilesystem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.nio.file.FileSystem;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class MemoryFileSystemFactoryBeanTest implements ApplicationContextAware {

  private FileSystem fileSystem;

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) {
    this.fileSystem = applicationContext.getBean(FileSystem.class);
  }


  @Test
  public void isOpen() {
    assertNotNull(this.fileSystem);
    assertTrue(this.fileSystem.isOpen());
    assertEquals("memory:test:///", this.fileSystem.getPath("").toUri().toString());
  }

}
