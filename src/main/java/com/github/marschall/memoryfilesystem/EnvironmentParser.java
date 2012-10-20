package com.github.marschall.memoryfilesystem;

import java.text.Collator;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

class EnvironmentParser {

  private final Map<String, ?> env;

  private List<String> roots;

  EnvironmentParser(Map<String, ?> env) {
    this.env = env;
  }

  List<String> getRoots() {
    if (this.roots == null) {
      this.roots = this.parseStringProperty(MemoryFileSystemProperties.ROOTS_PROPERTY, false);
      if (this.roots == null) {
        this.roots = this.getDefaultRoots();
      } else {
        this.validateRoots(this.roots);
      }
    }
    return this.roots;
  }

  List<String> getUserNames() {
    List<String> userNames = this.parseStringProperty(MemoryFileSystemProperties.USERS_PROPERTY, false);
    if (userNames == null) {
      return Collections.singletonList(this.getSystemUserName());
    } else {
      return userNames;
    }
  }

  StringTransformer getStoreTransformer() {
    return this.getStringTranformer(MemoryFileSystemProperties.PATH_STORE_TRANSFORMER_PROPERTY);
  }

  StringTransformer getLookUpTransformer() {
    return this.getStringTranformer(MemoryFileSystemProperties.PATH_LOOKUP_TRANSFORMER_PROPERTY);
  }

  StringTransformer getStringTranformer(String property) {
    Object value = this.env.get(property);
    if (value != null) {
      if (value instanceof StringTransformer) {
        return (StringTransformer) value;
      } else {
        throw new IllegalArgumentException(property + " must be a "
                + StringTransformer.class + " but was " + value.getClass());
      }
    } else {
      return StringTransformers.IDENTIY;
    }
  }


  Collator getCollator() {
    String property = MemoryFileSystemProperties.COLLATOR_PROPERTY;
    Object value = this.env.get(property);
    if (value != null) {
      if (value instanceof Collator) {
        return (Collator) value;
      } else {
        throw new IllegalArgumentException(property + " must be a "
                + Collator.class + " but was " + value.getClass());
      }
    } else {
      return MemoryFileSystemProperties.caseSensitiveCollator(Locale.getDefault());
    }
  }



  StringTransformer getPrincipalNameTransfomer() {
    return this.getStringTranformer(MemoryFileSystemProperties.PRINCIPAL_TRANSFORMER_PROPERTY);
  }

  String getDefaultDirectory() {
    Object value = this.env.get(MemoryFileSystemProperties.CURRENT_WORKING_DIRECTORY_PROPERTY);
    if (value != null) {
      if (value instanceof String) {
        return (String) value;
      } else {
        throw new IllegalArgumentException(MemoryFileSystemProperties.CURRENT_WORKING_DIRECTORY_PROPERTY + " must be a "
                + String.class + " but was " + value.getClass());
      }
    } else {
      return this.getRoots().get(0);
    }
  }

  String getSystemUserName() {
    return System.getProperty("user.name");
  }

  List<String> getGroupNames() {
    List<String> groupNames = this.parseStringProperty(MemoryFileSystemProperties.USERS_PROPERTY, true);
    if (groupNames == null) {
      return Collections.emptyList();
    } else {
      return groupNames;
    }

  }


  private List<String> parseStringProperty(String key, boolean allowEmpty) {
    Object value = this.env.get(key);
    if (value == null) {
      return null;
    }
    if (!(value instanceof List)) {
      throw new IllegalArgumentException("value of " + key
              + " must be an instance of " + List.class + " but was " + value.getClass());
    }
    List<?> values = (List<?>) value;
    if (!allowEmpty && values.isEmpty()) {
      throw new IllegalArgumentException("value of " + key + " must not be empty");
    } else if (values.size() == 1) {
      Object singleValue = values.get(0);
      if (singleValue instanceof String) {
        String stringValue = (String) singleValue;
        return Collections.singletonList(stringValue);
      } else {
        throw new IllegalArgumentException(key + " must be a String but was " + singleValue.getClass());
      }
    } else {
      for (Object each : values) {
        if (each == null) {
          throw new IllegalArgumentException("each value of " + key + " must be a String but was null");
        } else if (!(each instanceof String)) {
          throw new IllegalArgumentException("each value of " + key + " must be a String but was " + each.getClass());
        }
      }
      @SuppressWarnings("unchecked")
      List<String> returnValue = (List<String>) values;
      return returnValue;
    }
  }

  private void validateRoots(List<String> roots)  {
    if (roots.size() == 1) {
      String root = roots.get(0);
      if (!this.isUnixRoot(root) && !this.isWindowsRoot(root)) {
        throw this.invalidRoot(root);
      }
    } else {
      for (String root : roots) {
        if (!this.isWindowsRoot(root)) {
          throw this.invalidRoot(root);
        }
      }
    }
  }

  private IllegalArgumentException invalidRoot(String root) {
    return new IllegalArgumentException(MemoryFileSystemProperties.ROOTS_PROPERTY + " have to either be \"\" or \"[A-Z]:\\\" (mixing is not allowed)"
            + " \"" + root + "\"doesn't fit");
  }

  private boolean isWindowsRoot(String root) {
    return root.length() == 3
            && root.charAt(0) >= 'A' && root.charAt(0) <= 'Z'
            && root.endsWith(":\\");
  }

  private boolean isUnixRoot(String root) {
    return MemoryFileSystemProperties.UNIX_ROOT.equals(root);
  }

  private List<String> getDefaultRoots() {
    return MemoryFileSystemProperties.DEFAULT_ROOTS;
  }


  String getSeparator() {
    Object property = this.env.get(MemoryFileSystemProperties.DEFAULT_NAME_SEPARATOR_PROPERTY);
    if (property == null) {
      return MemoryFileSystemProperties.DEFAULT_NAME_SEPARATOR;
    } else if (property instanceof String) {
      String separator = (String) property;
      this.validateSeparator(separator);
      return separator;
    } else {
      throw new IllegalArgumentException("the value of the property '"
              + MemoryFileSystemProperties.DEFAULT_NAME_SEPARATOR_PROPERTY
              + "' has to be of class " + String.class.getName()
              + " but was " + property.getClass().getName());
    }
  }

  private void validateSeparator(String separator) {
    if (!MemoryFileSystemProperties.UNIX_SEPARATOR.equals(separator)
            && ! MemoryFileSystemProperties.WINDOWS_SEPARATOR.equals(separator)) {
      throw new IllegalArgumentException("only \"" + MemoryFileSystemProperties.UNIX_SEPARATOR
              + "\" and \"" + MemoryFileSystemProperties.WINDOWS_SEPARATOR + "\" are valid separators, \""
              + separator + "\" is invalid");
    }
  }

  boolean isSingleEmptyRoot() {
    return this.getRoots().size() == 1
            && MemoryFileSystemProperties.UNIX_ROOT.equals(this.getRoots().get(0));
  }

}
