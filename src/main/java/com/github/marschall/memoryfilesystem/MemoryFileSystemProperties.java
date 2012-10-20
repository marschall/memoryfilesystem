package com.github.marschall.memoryfilesystem;

import java.text.Collator;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class MemoryFileSystemProperties {

  static final String UNIX_SEPARATOR = "/";

  static final String WINDOWS_SEPARATOR = "\\";

  public static final String DEFAULT_NAME_SEPARATOR = UNIX_SEPARATOR;

  public static final String DEFAULT_NAME_SEPARATOR_PROPERTY = "file.separator";

  public static final String CURRENT_WORKING_DIRECTORY_PROPERTY = "user.dir";

  public static final String ROOTS_PROPERTY = "roots";

  public static final String FILE_ATTRIBUTE_VIEWS_PROPERTY = "file.attrs";

  public static final String USERS_PROPERTY = "users";

  public static final String GROUPS_PROPERTY = "groups";

  public static final String PATH_STORE_TRANSFORMER_PROPERTY = "path.store.transformer";

  public static final String PATH_LOOKUP_TRANSFORMER_PROPERTY = "path.lookup.transformer";

  public static final String PRINCIPAL_TRANSFORMER_PROPERTY = "princial.transformer";

  public static final String COLLATOR_PROPERTY = "collator";

  static final String UNIX_ROOT = "/";

  static final List<String> DEFAULT_ROOTS = Collections.singletonList(UNIX_ROOT);

  static Collator caseSensitiveCollator(Locale locale) {
    Collator collator = Collator.getInstance(locale);
    collator.setDecomposition(Collator.NO_DECOMPOSITION);
    collator.setStrength(Collator.IDENTICAL);
    return collator;
  }

  static Collator caseInsensitiveCollator(Locale locale) {
    Collator collator = Collator.getInstance(locale);
    collator.setDecomposition(Collator.NO_DECOMPOSITION);
    collator.setStrength(Collator.SECONDARY);
    return collator;
  }

}
