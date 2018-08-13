package com.github.marschall.memoryfilesystem;

import java.nio.file.CopyOption;
import java.nio.file.LinkOption;
import java.nio.file.StandardCopyOption;
import java.util.Set;

final class Options {

  static boolean isCopyAttributes(Object[] options) {
    if (options == null || options.length == 0) {
      return false;
    }

    for (Object option : options) {
      if (option == StandardCopyOption.COPY_ATTRIBUTES) {
        return true;
      }
    }
    return false;
  }

  static boolean isFollowSymLinks(Set<?> options) {
    if (options == null || options.isEmpty()) {
      return true;
    }

    for (Object option : options) {
      if (option == LinkOption.NOFOLLOW_LINKS) {
        return false;
      }
    }
    return true;
  }

  static boolean isFollowSymLinks(Object[] options) {
    if (options == null) {
      return true;
    }

    for (Object option : options) {
      if (option == LinkOption.NOFOLLOW_LINKS) {
        return false;
      }
    }
    return true;
  }

  static boolean isReplaceExisting(CopyOption... options) {
    if (options == null) {
      return false;
    }

    for (CopyOption option : options) {
      if (option == StandardCopyOption.REPLACE_EXISTING) {
        return true;
      }
    }
    return false;
  }

}
