package com.github.marschall.memoryfilesystem;

import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.util.Collections;
import java.util.Set;

import com.github.marschall.memoryfilesystem.MemoryUserPrincipalLookupService.MemoryGroup;
import com.github.marschall.memoryfilesystem.MemoryUserPrincipalLookupService.MemoryUser;

final class EntryCreationContext {

  final Set<Class<? extends FileAttributeView>> additionalViews;
  final Set<PosixFilePermission> permissions;
  final UserPrincipal user;
  final GroupPrincipal group;
  final MemoryFileSystem fileSystem;

  EntryCreationContext(Set<Class<? extends FileAttributeView>> additionalViews,
          Set<PosixFilePermission> permissions, UserPrincipal user,
          GroupPrincipal group, MemoryFileSystem fileSystem) {
    this.additionalViews = additionalViews;
    this.permissions = permissions;
    this.user = user;
    this.group = group;
    this.fileSystem = fileSystem;
  }

  Class<? extends FileAttributeView> firstView() {
    return this.additionalViews.iterator().next();
  }

  static EntryCreationContext empty() {
    // REVIEW can be fixed with Java 8 source
    Set<Class<? extends FileAttributeView>> noViews = Collections.emptySet();
    Set<PosixFilePermission> noPermissions = Collections.emptySet();
    return new EntryCreationContext(noViews, noPermissions, new MemoryUser("dummy"), new MemoryGroup("dummy"), null);
  }

}
