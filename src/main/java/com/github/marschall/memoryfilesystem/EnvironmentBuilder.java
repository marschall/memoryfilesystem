package com.github.marschall.memoryfilesystem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EnvironmentBuilder {

  //TODO principal transfomer

  private final List<String> roots;
  
  private final List<String> users;
  
  private final List<String> groups;

  private String separator;

  private String currentWorkingDirectory;


  private EnvironmentBuilder() {
    this.roots = new ArrayList<>();
    this.users = new ArrayList<>();
    this.groups = new ArrayList<>();
  }

  public EnvironmentBuilder addRoot(String root) {
    this.roots.add(root);
    return this;
  }

  public EnvironmentBuilder setSeprator(String separator) {
    this.separator = separator;
    return this;
  }
  
  public EnvironmentBuilder addUser(String userName) {
    this.users.add(userName);
    return this;
  }
  
  public EnvironmentBuilder addGroup(String groupName) {
    this.groups.add(groupName);
    return this;
  }

  public EnvironmentBuilder setCurrentWorkingDirectory(String currentWorkingDirectory) {
    this.currentWorkingDirectory = currentWorkingDirectory;
    return this;
  }

  public static EnvironmentBuilder newEmpty() {
    return new EnvironmentBuilder();
  }

  public static EnvironmentBuilder newUnix() {
    return new EnvironmentBuilder()
    .addRoot(MemoryFileSystemProperties.UNIX_ROOT)
    .setSeprator(MemoryFileSystemProperties.UNIX_SEPARATOR)
    .addUser(getSystemUserName())
    .addGroup(getSystemUserName())
    .setCurrentWorkingDirectory("/home/" + getSystemUserName());
  }
  
  public static EnvironmentBuilder newMacOs() {
    return new EnvironmentBuilder()
    .addRoot(MemoryFileSystemProperties.UNIX_ROOT)
    .setSeprator(MemoryFileSystemProperties.UNIX_SEPARATOR)
    .addUser(getSystemUserName())
    .addGroup(getSystemUserName())
    .setCurrentWorkingDirectory("/Users/" + getSystemUserName());
  }

  public static EnvironmentBuilder newWindows() {
    return new EnvironmentBuilder()
      .addRoot("C:\\")
      .setSeprator(MemoryFileSystemProperties.WINDOWS_SEPARATOR)
      .addUser(getSystemUserName())
      .addGroup(getSystemUserName())
      .setCurrentWorkingDirectory("C:\\Users\\" + getSystemUserName());
  }

  static String getSystemUserName() {
    return System.getProperty("user.name");
  }
  
  public Map<String, ?> build() {
    Map<String, Object> env = new HashMap<>();
    if (!this.roots.isEmpty()) {
      env.put(MemoryFileSystemProperties.ROOTS_PROPERTY, this.roots);
    }
    if (this.separator != null) {
      env.put(MemoryFileSystemProperties.DEFAULT_NAME_SEPARATOR_PROPERTY, this.separator);
    }
    if (this.currentWorkingDirectory != null) {
      env.put(MemoryFileSystemProperties.CURRENT_WORKING_DIRECTORY_PROPERTY, this.currentWorkingDirectory);
    }
    return env;
  }

}
