package com.github.marschall.memoryfilesystem;

import static java.nio.file.AccessMode.EXECUTE;
import static java.nio.file.AccessMode.READ;
import static java.nio.file.AccessMode.WRITE;
import static java.nio.file.attribute.PosixFilePermission.GROUP_EXECUTE;
import static java.nio.file.attribute.PosixFilePermission.GROUP_READ;
import static java.nio.file.attribute.PosixFilePermission.GROUP_WRITE;
import static java.nio.file.attribute.PosixFilePermission.OTHERS_EXECUTE;
import static java.nio.file.attribute.PosixFilePermission.OTHERS_READ;
import static java.nio.file.attribute.PosixFilePermission.OTHERS_WRITE;
import static java.nio.file.attribute.PosixFilePermission.OWNER_EXECUTE;
import static java.nio.file.attribute.PosixFilePermission.OWNER_READ;
import static java.nio.file.attribute.PosixFilePermission.OWNER_WRITE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.AccessMode;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.nio.file.spi.FileSystemProvider;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;

import com.github.marschall.memoryfilesystem.CurrentGroup.GroupTask;
import com.github.marschall.memoryfilesystem.CurrentUser.UserTask;

public class PosixPermissionMemoryFileSystemTest {

  @Rule
  public final PosixPermissionFileSystemRule rule = new PosixPermissionFileSystemRule();

  @Test
  public void directoryRead() throws IOException {
    FileSystem fileSystem = this.rule.getFileSystem();
    final Path directory = fileSystem.getPath("directory");
    Path file = directory.resolve("file");
    Files.createDirectory(directory);
    Files.createFile(file);

    Files.setAttribute(directory, "posix:permissions", asSet(OWNER_READ, OWNER_WRITE, OWNER_EXECUTE));
    UserPrincipal user = fileSystem.getUserPrincipalLookupService().lookupPrincipalByName(PosixPermissionFileSystemRule.OTHER);
    CurrentUser.useDuring(user, new UserTask<Void>() {

      @Override
      public Void call() throws IOException {
        try (DirectoryStream<?> stream = Files.newDirectoryStream(directory)) {
          fail("should not be allowd to open a directory stram");
        } catch (AccessDeniedException e) {
          // should reach here
        }
        return null;
      }
    });

  }

  @Test
  public void owner() throws IOException {
    this.checkPermission(READ, OWNER_READ, PosixPermissionFileSystemRule.OWNER);
    this.checkPermission(WRITE, OWNER_WRITE, PosixPermissionFileSystemRule.OWNER);
    this.checkPermission(EXECUTE, OWNER_EXECUTE, PosixPermissionFileSystemRule.OWNER);
  }

  @Test
  public void group() throws IOException {
    this.checkPermission(READ, GROUP_READ, PosixPermissionFileSystemRule.GROUP);
    this.checkPermission(WRITE, GROUP_WRITE, PosixPermissionFileSystemRule.GROUP);
    this.checkPermission(EXECUTE, GROUP_EXECUTE, PosixPermissionFileSystemRule.GROUP);
  }

  @Test
  public void others() throws IOException {
    this.checkPermission(READ, OTHERS_READ, PosixPermissionFileSystemRule.OTHER);
    this.checkPermission(WRITE, OTHERS_WRITE, PosixPermissionFileSystemRule.OTHER);
    this.checkPermission(EXECUTE, OTHERS_EXECUTE, PosixPermissionFileSystemRule.OTHER);
  }


  private static Set<PosixFilePermission> asSet(PosixFilePermission... permissions) {
    return new HashSet<>(Arrays.asList(permissions));
  }

  @Test
  public void toSet() {
    assertEquals(asSet(OWNER_READ), MemoryEntry.toSet(0b1));
    assertEquals(asSet(OTHERS_EXECUTE), MemoryEntry.toSet(0b100000000));
    assertEquals(asSet(OWNER_READ, OTHERS_EXECUTE), MemoryEntry.toSet(0b100000001));
  }

  @Test
  public void toMask() {
    assertEquals(0b1, MemoryEntry.toMask(asSet(OWNER_READ)));
    assertEquals(0b100000000, MemoryEntry.toMask(asSet(OTHERS_EXECUTE)));
    assertEquals(0b100000001, MemoryEntry.toMask(asSet(OWNER_READ, OTHERS_EXECUTE)));
  }


  private void checkPermission(AccessMode mode, PosixFilePermission permission, String userName) throws IOException {
    FileSystem fileSystem = this.rule.getFileSystem();
    final Path positive = fileSystem.getPath(userName + "-" + mode + "-" + permission + "-positive.txt");
    Files.createFile(positive);
    final Path negative = fileSystem.getPath(userName + "-" + mode + "-" + permission + "-negative.txt");
    Files.createFile(negative);

    UserPrincipal user = fileSystem.getUserPrincipalLookupService().lookupPrincipalByName(userName);
    GroupPrincipal group = fileSystem.getUserPrincipalLookupService().lookupPrincipalByGroupName(userName);
    this.checkPermissionPositive(mode, permission, positive, user, group);
    this.checkPermissionNegative(mode, permission, negative, user, group);
  }

  private void checkPermissionPositive(final AccessMode mode, PosixFilePermission permission, final Path file, UserPrincipal user, final GroupPrincipal group) throws IOException {
    PosixFileAttributeView view = Files.getFileAttributeView(file, PosixFileAttributeView.class);
    FileSystem fileSystem = this.rule.getFileSystem();
    GroupPrincipal fileGroup = fileSystem.getUserPrincipalLookupService().lookupPrincipalByGroupName(PosixPermissionFileSystemRule.GROUP);
    view.setGroup(fileGroup); // change group before changing permissions
    view.setPermissions(Collections.singleton(permission));

    CurrentUser.useDuring(user, new UserTask<Void>() {

      @Override
      public Void call() throws IOException {
        return CurrentGroup.useDuring(group, new GroupTask<Void>() {

          @Override
          public Void call() throws IOException {
            FileSystemProvider provider = file.getFileSystem().provider();
            provider.checkAccess(file, mode);
            return null;
          }
        });
      }

    });
  }

  private void checkPermissionNegative(final AccessMode mode, PosixFilePermission permission, final Path file, UserPrincipal user, final GroupPrincipal group) throws IOException {
    PosixFileAttributeView view = Files.getFileAttributeView(file, PosixFileAttributeView.class);

    Set<PosixFilePermission> permissions = EnumSet.allOf(PosixFilePermission.class);
    FileSystem fileSystem = this.rule.getFileSystem();
    GroupPrincipal fileGroup = fileSystem.getUserPrincipalLookupService().lookupPrincipalByGroupName(PosixPermissionFileSystemRule.GROUP);
    view.setGroup(fileGroup); // change group before changing permissions
    permissions.remove(permission);
    view.setPermissions(permissions);

    CurrentUser.useDuring(user, new UserTask<Void>() {

      @Override
      public Void call() throws IOException {
        return CurrentGroup.useDuring(group, new GroupTask<Void>() {

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

    });
  }

}
