package com.github.marschall.memoryfilesystem;

import static java.nio.file.AccessMode.EXECUTE;
import static java.nio.file.AccessMode.READ;
import static java.nio.file.AccessMode.WRITE;
import static java.nio.file.attribute.PosixFilePermission.OTHERS_EXECUTE;
import static java.nio.file.attribute.PosixFilePermission.OTHERS_READ;
import static java.nio.file.attribute.PosixFilePermission.OTHERS_WRITE;
import static java.nio.file.attribute.PosixFilePermission.OWNER_EXECUTE;
import static java.nio.file.attribute.PosixFilePermission.OWNER_READ;
import static java.nio.file.attribute.PosixFilePermission.OWNER_WRITE;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.AccessMode;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.nio.file.spi.FileSystemProvider;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import com.github.marschall.memoryfilesystem.CurrentUser.UserTask;

public class PosixPermissionMemoryFileSystemTest {

  @Rule
  public final PosixPermissionFileSystemRule rule = new PosixPermissionFileSystemRule();

  @Test
  public void owner() throws IOException {
    this.checkPermission(READ, OWNER_READ, PosixPermissionFileSystemRule.OWNER);
    this.checkPermission(WRITE, OWNER_WRITE, PosixPermissionFileSystemRule.OWNER);
    this.checkPermission(EXECUTE, OWNER_EXECUTE, PosixPermissionFileSystemRule.OWNER);
  }

  @Test
  @Ignore()
  public void group() {
    // TODO implement
    fail("implement");
  }

  @Test
  public void others() throws IOException {
    this.checkPermission(READ, OTHERS_READ, PosixPermissionFileSystemRule.OTHER);
    this.checkPermission(WRITE, OTHERS_WRITE, PosixPermissionFileSystemRule.OTHER);
    this.checkPermission(EXECUTE, OTHERS_EXECUTE, PosixPermissionFileSystemRule.OTHER);
  }


  private void checkPermission(AccessMode mode, PosixFilePermission permission, String userName) throws IOException {
    FileSystem fileSystem = this.rule.getFileSystem();
    final Path positive = fileSystem.getPath(userName + "-" + mode + "-" + permission + "-positive.txt");
    Files.createFile(positive);
    final Path negative = fileSystem.getPath(userName + "-" + mode + "-" + permission + "-negative.txt");
    Files.createFile(negative);

    UserPrincipal user = fileSystem.getUserPrincipalLookupService().lookupPrincipalByName(userName);
    this.checkPermissionPositive(mode, permission, positive, user);
    this.checkPermissionNegative(mode, permission, negative, user);
  }

  private void checkPermissionPositive(final AccessMode mode, PosixFilePermission permission, final Path file, UserPrincipal user) throws IOException {
    PosixFileAttributeView view = Files.getFileAttributeView(file, PosixFileAttributeView.class);
    view.setPermissions(Collections.singleton(permission));

    CurrentUser.useDuring(user, new UserTask<Void>() {

      @Override
      public Void call() throws IOException {
        FileSystemProvider provider = file.getFileSystem().provider();
        provider.checkAccess(file, mode);
        return null;
      }

    });
  }

  private void checkPermissionNegative(final AccessMode mode, PosixFilePermission permission, final Path file, UserPrincipal user) throws IOException {
    PosixFileAttributeView view = Files.getFileAttributeView(file, PosixFileAttributeView.class);

    Set<PosixFilePermission> permissions = EnumSet.allOf(PosixFilePermission.class);
    permissions.remove(permission);
    view.setPermissions(permissions);

    CurrentUser.useDuring(user, new UserTask<Void>() {

      @Override
      public Void call() throws IOException {
        FileSystemProvider provider = file.getFileSystem().provider();
        try {
          provider.checkAccess(file, mode);
          fail("should not be able to access");
        } catch (AccessDeniedException e) {
          // should reach here
        }
        return null;
      }

    });
  }

}
