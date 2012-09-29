package com.github.marschall.memoryfilesystem;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;

/**
 * A POJO factory bean to create memory file systems.
 * 
 * <p>This class is intended to be used with Spring XML configuration.
 * However it is not tied to Spring XML configuration. You can use it with
 * Java configuration as well as any other dependency injection framework
 * or even without one.</p>
 * 
 * <p>The only method that has to be invoked before {@link #getObject()} is
 * {@link #setName(String)}.</p>
 * 
 * <p>A minimal Spring configuration can look something like this:</p>
 * <pre><code>
 * &lt;bean id="memoryFileSystemFactory"
 *    class="com.github.marschall.memoryfilesystem.MemoryFileSystemFactoryBean"&gt;
 *   &lt;property name="name" value="test"/&gt;
 * &lt;/bean&gt;
 *
 * &lt;bean id="memoryFileSystem"
 *     factory-bean="memoryFileSystemFactory" factory-method="getObject" /&gt;
 *  
 * &lt;!-- enable @PreDestroy on MemoryFileSystem#close() --&gt;
 * &lt;context:annotation-config/&gt;
 * </pre></code>
 */
public class MemoryFileSystemFactoryBean {

  private EnvironmentBuilder builder;
  private String name;

  public MemoryFileSystemFactoryBean() {
    this.builder = EnvironmentBuilder.newEmpty();
  }
  
  /**
   * Sets the unique name that identifies the file system to create.
   * 
   * <p>This method method <strong>has</strong> to be invoked and the
   * argument must not be {@code null}.</p>
   * 
   * @param name the name of the file system, this should be a purely
   *  alpha numeric string
   */
  public void setName(String name) {
    this.name = name;
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
    if (this.name == null) {
      // TODO generate random name?
      throw new IllegalArgumentException("name must be set");
    }
    URI uri = URI.create("memory:" + this.name);
    try {
      return FileSystems.newFileSystem(uri, this.builder.build(), MemoryFileSystemFactoryBean.class.getClassLoader());
    } catch (IOException e) {
      throw new IllegalArgumentException("could not create file system", e);
    }
  }

}
