package com.github.marschall.memoryfilesystem;

import java.nio.file.Path;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.util.Set;

public final class EntryCreationContext {

  final Set<Class<? extends FileAttributeView>> additionalViews;
  final Set<PosixFilePermission> permissions;
  final UserPrincipal user;
  final GroupPrincipal group;
  final FileSystemContext fileSystem;
  final Path path;

  EntryCreationContext(Set<Class<? extends FileAttributeView>> additionalViews,
          Set<PosixFilePermission> permissions, UserPrincipal user,
          GroupPrincipal group, FileSystemContext fileSystem, Path path) {
    this.additionalViews = additionalViews;
    this.permissions = permissions;
    this.user = user;
    this.group = group;
    this.fileSystem = fileSystem;
    this.path = path;
  }

  Class<? extends FileAttributeView> firstView() {
    return this.additionalViews.iterator().next();
  }

}
