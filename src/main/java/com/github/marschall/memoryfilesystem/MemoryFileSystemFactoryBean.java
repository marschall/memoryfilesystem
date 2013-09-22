package com.github.marschall.memoryfilesystem;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.util.UUID;

/**
 * A POJO factory bean to create memory file systems.
 * 
 * <p>This class is intended to be used with Spring XML configuration.
 * However it is not tied to Spring XML configuration. You can use it with
 * Java configuration as well as any other dependency injection framework
 * or even without one.</p>
 * 
 * <p>You can optionally configure the type file system that should be created
 * (Windows, Linux, MacOS) and the name. The name shows up only when a path is
 * converted to a URI.</p>
 * 
 * <p>A minimal Spring configuration can look something like this:</p>
 * <pre><code>
 * &lt;bean id="memoryFileSystemFactory"
 *    class="com.github.marschall.memoryfilesystem.MemoryFileSystemFactoryBean"/&gt;
 *
 * &lt;bean id="memoryFileSystem" destroy-method="close"
 *     factory-bean="memoryFileSystemFactory" factory-method="getObject"/&gt;
 * 
 * </code></pre>
 * 
 * <p>You can also save the <tt>destroy-method</tt> enable {@code @PreDestroy} with:</p>
 * <pre><code>
 * &lt;bean id="memoryFileSystemFactory"
 *    class="com.github.marschall.memoryfilesystem.MemoryFileSystemFactoryBean"/&gt;
 *
 * &lt;bean id="memoryFileSystem"
 *     factory-bean="memoryFileSystemFactory" factory-method="getObject"/&gt;
 * 
 * &lt;context:annotation-config/&gt;
 * </code></pre>
 */
public class MemoryFileSystemFactoryBean {

  private String name;

  private String type;

  public static final String WINDOWS = "windows";

  public static final String LINUX = "linux";

  public static final String MACOS = "macos";

  /**
   * Sets the name that identifies the file system to create.
   * 
   * <p>The name must be unique across all memory file system instances.</p>
   * 
   * <p>If the name is not set, a random one will be generated.</p>
   * 
   * @param name the name of the file system, this should be a purely
   *  alpha numeric string
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Sets what type of file system should be created.
   * 
   * @see #WINDOWS
   * @see #LINUX
   * @see #MACOS
   * 
   * @param type the file system type, one of {@value #WINDOWS},
   *  {@value #LINUX}, {@value #MACOS}
   */
  public void setType(String type) {
    this.type = type;
  }

  private String getName() {
    if (this.name != null) {
      return this.name;
    } else {
      return UUID.randomUUID().toString();
    }
  }

  private MemoryFileSystemBuilder getBuilder() {
    if (this.type == null) {
      return MemoryFileSystemBuilder.newEmpty();
    }
    switch (this.type) {
      case WINDOWS:
        return MemoryFileSystemBuilder.newWindows();
      case LINUX:
        return MemoryFileSystemBuilder.newLinux();
      case MACOS:
        return MemoryFileSystemBuilder.newMacOs();
      default:
        throw new IllegalArgumentException("unknown file system type: " + this.type);
    }
  }

  /**
   * Factory method that creates the file system.
   * 
   * <p>Make sure you invoke {@link FileSystem#close()} after you're done
   * using it, otherwise you risk a resource leak. The easiest way to do is
   * is to use {@code <context:annotation-config/>}.</p>
   * 
   * @return the file system
   */
  public FileSystem getObject() {
    try {
      return this.getBuilder().build(this.getName());
    } catch (IOException e) {
      throw new IllegalArgumentException("could not create file system", e);
    }
  }

}
