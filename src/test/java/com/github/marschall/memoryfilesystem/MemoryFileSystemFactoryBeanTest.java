package com.github.marschall.memoryfilesystem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.FileSystem;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ContextConfiguration
@ExtendWith(SpringExtension.class)
class MemoryFileSystemFactoryBeanTest implements ApplicationContextAware {

  private FileSystem fileSystem;

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) {
    this.fileSystem = applicationContext.getBean(FileSystem.class);
  }


  @Test
  void isOpen() {
    assertNotNull(this.fileSystem);
    assertTrue(this.fileSystem.isOpen());
    assertEquals("memory:test:///", this.fileSystem.getPath("").toUri().toString());
  }

}
