package com.github.marschall.memoryfilesystem;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.UserPrincipal;
import java.util.List;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

public class PosixAclMemoryFileSystemTest {

  @Rule
  public final PosixAclFileSystemRule rule = new PosixAclFileSystemRule();

  @Test
  @Ignore("not ready yet")
  public void defaultAttributes() throws IOException {
    FileSystem fileSystem = this.rule.getFileSystem();
    Path file = fileSystem.getPath("file.txt");

    Files.createFile(file);

    AclFileAttributeView aclFileAttributeView = Files.getFileAttributeView(file, AclFileAttributeView.class);
    List<AclEntry> acl = aclFileAttributeView.getAcl();
    assertThat(acl, not(empty()));

    UserPrincipal owner = aclFileAttributeView.getOwner();
    assertEquals("", owner.getName());
  }

}
