package com.github.marschall.memoryfilesystem;

import static com.github.marschall.memoryfilesystem.FileExistsMatcher.exists;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.FileSystem;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ContextConfiguration(locations = "MemoryFileSystemFactoryBeanTest-context-windows.xml")
@ExtendWith(SpringExtension.class)
class MemoryFileSystemFactoryBeanTypeTest implements ApplicationContextAware {

  private FileSystem fileSystem;

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) {
    this.fileSystem = applicationContext.getBean(FileSystem.class);
  }

  @Test
  void isOpen() {
    assertTrue(this.fileSystem.isOpen());
    assertThat(this.fileSystem.getPath("C:"), exists());
  }

}
