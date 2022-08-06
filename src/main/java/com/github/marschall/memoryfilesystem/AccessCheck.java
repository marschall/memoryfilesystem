package com.github.marschall.memoryfilesystem;

import java.nio.file.AccessDeniedException;
import java.nio.file.AccessMode;

interface AccessCheck {

  void checkAccess(AccessMode[] modes) throws AccessDeniedException;

  void checkAccess(AccessMode mode) throws AccessDeniedException;

  boolean canRead();

}
