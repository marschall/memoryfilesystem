package com.github.marschall.memoryfilesystem;

import static java.util.Collections.emptySet;

import java.nio.file.attribute.UserPrincipal;
import java.time.Instant;

import com.github.marschall.memoryfilesystem.MemoryUserPrincipalLookupService.MemoryGroup;
import com.github.marschall.memoryfilesystem.MemoryUserPrincipalLookupService.MemoryUser;

final class EntryCreationContextUtil {

  private EntryCreationContextUtil() {
    throw new AssertionError("not instantiable");
  }

  static EntryCreationContext empty() {
    String userId = "dummy";
    MemoryUser user = new MemoryUser(userId);
    FileSystemContext fileSystem = new StubFileSystemContext(user);
    return new EntryCreationContext(emptySet(), emptySet(), user, new MemoryGroup(userId), fileSystem, null);
  }

  static final class StubFileSystemContext implements FileSystemContext {

    private final UserPrincipal defaultUser;

    StubFileSystemContext(UserPrincipal defaultUser) {
      this.defaultUser = defaultUser;
    }

    @Override
    public UserPrincipal getDefaultUser() {
      return this.defaultUser;
    }

    @Override
    public Instant truncate(Instant instant) {
      return instant;
    }

  }

}
