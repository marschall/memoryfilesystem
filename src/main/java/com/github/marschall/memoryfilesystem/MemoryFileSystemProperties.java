package com.github.marschall.memoryfilesystem;

import java.util.Collections;
import java.util.List;

public class MemoryFileSystemProperties {

  static final String UNIX_SEPARATOR = "/";

  static final String WINDOWS_SEPARATOR = "\\";

  public static final String DEFAULT_NAME_SEPARATOR = UNIX_SEPARATOR;

  public static final String DEFAULT_NAME_SEPARATOR_PROPERTY = "file.separator";

  public static final String CURRENT_WORKING_DIRECTORY_PROPERTY = "user.dir";

  public static final String ROOTS_PROPERTY = "roots";

  public static final String USERS_PROPERTY = "users";

  public static final String GROUPS_PROPERTY = "groups";

  public static final String PATH_TRANSFORMER_PROPERTY = "path.transformer";

  static final String UNIX_ROOT = "/";

  static final List<String> DEFAULT_ROOTS = Collections.singletonList(UNIX_ROOT);
  
  static final String BASIC_FILE_ATTRIBUTE_VIEW_NAME = "basic";

}
