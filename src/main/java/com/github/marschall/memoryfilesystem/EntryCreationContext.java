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
  final Set<PosixFilePermission> umask;
  final UserPrincipal user;
  final GroupPrincipal group;

  EntryCreationContext(Set<Class<? extends FileAttributeView>> additionalViews,
          Set<PosixFilePermission> umask, UserPrincipal user,
          GroupPrincipal group) {
    this.additionalViews = additionalViews;
    this.umask = umask;
    this.user = user;
    this.group = group;
  }

  Class<? extends FileAttributeView> firstView() {
    return this.additionalViews.iterator().next();
  }

  static EntryCreationContext empty() {
    return new EntryCreationContext(Collections.<Class<? extends FileAttributeView>>emptySet(),
            Collections.<PosixFilePermission>emptySet(), new MemoryUser("dummy"), new MemoryGroup("dummy"));
  }

}
