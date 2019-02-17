package com.github.marschall.memoryfilesystem;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.UserPrincipal;
import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class PosixAclMemoryFileSystemTest {

  @RegisterExtension
  final PosixAclFileSystemExtension extension = new PosixAclFileSystemExtension();

  @Test
  @Disabled("not ready yet")
  void defaultAttributes() throws IOException {
    FileSystem fileSystem = this.extension.getFileSystem();
    Path file = fileSystem.getPath("file.txt");

    Files.createFile(file);

    AclFileAttributeView aclFileAttributeView = Files.getFileAttributeView(file, AclFileAttributeView.class);
    List<AclEntry> acl = aclFileAttributeView.getAcl();
    assertThat(acl, not(empty()));

    UserPrincipal owner = aclFileAttributeView.getOwner();
    assertEquals("", owner.getName());
  }

}
