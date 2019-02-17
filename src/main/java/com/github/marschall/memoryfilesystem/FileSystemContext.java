package com.github.marschall.memoryfilesystem;

import java.nio.file.attribute.UserPrincipal;
import java.time.Instant;

interface FileSystemContext {

  UserPrincipal getDefaultUser();

  Instant truncate(Instant instant);

}
