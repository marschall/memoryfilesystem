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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.AccessMode;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.attribute.UserPrincipal;
import java.nio.file.spi.FileSystemProvider;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.github.marschall.memoryfilesystem.CurrentGroup.GroupTask;
import com.github.marschall.memoryfilesystem.CurrentUser.UserTask;

class PosixPermissionMemoryFileSystemTest {

  @RegisterExtension
  final PosixPermissionFileSystemExtension extension = new PosixPermissionFileSystemExtension();

  @Test
  void directoryRead() throws IOException {
    FileSystem fileSystem = this.extension.getFileSystem();
    final Path directory = fileSystem.getPath("directory");
    Path file = directory.resolve("file");
    Files.createDirectory(directory);
    Files.createFile(file);

    Files.setAttribute(directory, "posix:permissions", asSet(OWNER_READ, OWNER_WRITE, OWNER_EXECUTE, OTHERS_READ, OTHERS_WRITE));
    UserPrincipal user = fileSystem.getUserPrincipalLookupService().lookupPrincipalByName(PosixPermissionFileSystemExtension.OTHER);
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
  void owner() throws IOException {
    this.checkPermission(READ, OWNER_READ, PosixPermissionFileSystemExtension.OWNER);
    this.checkPermission(WRITE, OWNER_WRITE, PosixPermissionFileSystemExtension.OWNER);
    this.checkPermission(EXECUTE, OWNER_EXECUTE, PosixPermissionFileSystemExtension.OWNER);
  }

  @Test
  void group() throws IOException {
    this.checkPermission(READ, GROUP_READ, PosixPermissionFileSystemExtension.GROUP);
    this.checkPermission(WRITE, GROUP_WRITE, PosixPermissionFileSystemExtension.GROUP);
    this.checkPermission(EXECUTE, GROUP_EXECUTE, PosixPermissionFileSystemExtension.GROUP);
  }

  @Test
  void others() throws IOException {
    this.checkPermission(READ, OTHERS_READ, PosixPermissionFileSystemExtension.OTHER);
    this.checkPermission(WRITE, OTHERS_WRITE, PosixPermissionFileSystemExtension.OTHER);
    this.checkPermission(EXECUTE, OTHERS_EXECUTE, PosixPermissionFileSystemExtension.OTHER);
  }



  /**
   * Only the file owner can chmod
   *
   * @see <a href="https://github.com/marschall/memoryfilesystem/issues/50">Issue 50</a>
   * @throws IOException if the test fails
   */
  @Test
  void issue50() throws IOException {
    Path path = this.extension.getFileSystem().getPath("readable-at-first");
    Files.createFile(path,  PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("r--r--r--")));

    assertTrue(Files.isReadable(path));
    assertFalse(Files.isWritable(path));
    assertFalse(Files.isExecutable(path));

    Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rw-r--r--")); // FAIL.

    assertTrue(Files.isReadable(path)); // ok
    assertTrue(Files.isWritable(path)); // (should be) ok
    assertFalse(Files.isExecutable(path)); // ok
  }

  /**
   * The owner should be able to read current permissions for its own file.
   */
  @Test
  void issue51() throws IOException {
    Path path = this.extension.getFileSystem().getPath("readable-at-first");
    Files.createFile(path,  PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("---------")));

    assertFalse(Files.isReadable(path));
    assertFalse(Files.isWritable(path));
    assertFalse(Files.isExecutable(path));

    Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rw-r--r--")); // FAIL.

    assertTrue(Files.isReadable(path)); // ok
    assertTrue(Files.isWritable(path)); // (should be) ok
    assertFalse(Files.isExecutable(path)); // ok
  }

  /**
   * You should be able to read the attributes of a file depending on the directory permissions.
   */
  @Test
  void issue112() throws IOException {
    final Path path = this.extension.getFileSystem().getPath("not-mine");
    Files.createFile(path);
    UserPrincipal other = this.extension.getFileSystem().getUserPrincipalLookupService().lookupPrincipalByName(PosixPermissionFileSystemExtension.OTHER);
    Files.setOwner(path, other);
    CurrentUser.useDuring(other, new UserTask<Object>() {
      @Override
      public Object call() throws IOException {
        Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("---------"));
        return null;
      }
    });

    assertTrue(Files.exists(path));
    BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class);
    assertNotNull(attributes);
    assertTrue(attributes.isRegularFile());
    assertFalse(attributes.isDirectory());
    assertFalse(attributes.isSymbolicLink());
    assertFalse(attributes.isOther());
  }

  private static Set<PosixFilePermission> asSet(PosixFilePermission... permissions) {
    return new HashSet<>(Arrays.asList(permissions));
  }

  @Test
  void toSet() {
    assertEquals(asSet(OWNER_READ), MemoryEntryAttributes.toSet(0b1));
    assertEquals(asSet(OTHERS_EXECUTE), MemoryEntryAttributes.toSet(0b100000000));
    assertEquals(asSet(OWNER_READ, OTHERS_EXECUTE), MemoryEntryAttributes.toSet(0b100000001));
  }

  @Test
  void toMask() {
    assertEquals(0b1, MemoryEntryAttributes.toMask(asSet(OWNER_READ)));
    assertEquals(0b100000000, MemoryEntryAttributes.toMask(asSet(OTHERS_EXECUTE)));
    assertEquals(0b100000001, MemoryEntryAttributes.toMask(asSet(OWNER_READ, OTHERS_EXECUTE)));
  }


  private void checkPermission(AccessMode mode, PosixFilePermission permission, String userName) throws IOException {
    FileSystem fileSystem = this.extension.getFileSystem();
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
    FileSystem fileSystem = this.extension.getFileSystem();
    GroupPrincipal fileGroup = fileSystem.getUserPrincipalLookupService().lookupPrincipalByGroupName(PosixPermissionFileSystemExtension.GROUP);
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
    FileSystem fileSystem = this.extension.getFileSystem();
    GroupPrincipal fileGroup = fileSystem.getUserPrincipalLookupService().lookupPrincipalByGroupName(PosixPermissionFileSystemExtension.GROUP);
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

  @Test
  void deletePermission() throws IOException {
    FileSystem fileSystem = this.extension.getFileSystem();
    Path sourceDirectory = Files.createDirectory(fileSystem.getPath("source-directory"));
    final Path file = Files.createFile(sourceDirectory.resolve("file.txt"));

    PosixFileAttributeView fileView = Files.getFileAttributeView(sourceDirectory, PosixFileAttributeView.class);
    fileView.setPermissions(asSet(OWNER_READ, OWNER_WRITE, OWNER_EXECUTE));

    PosixFileAttributeView directoryView = Files.getFileAttributeView(sourceDirectory, PosixFileAttributeView.class);
    directoryView.setPermissions(asSet(OWNER_READ, OWNER_WRITE, OWNER_EXECUTE));

    UserPrincipal other = fileSystem.getUserPrincipalLookupService().lookupPrincipalByName(PosixPermissionFileSystemExtension.OTHER);

    CurrentUser.useDuring(other, new UserTask<Void>() {

      @Override
      public Void call() throws IOException {
        try {
          Files.delete(file);
          fail("should not be delete to create files");
        } catch (AccessDeniedException e) {
          // should reach here
        }
        return null;
      }
    });


    directoryView.setPermissions(asSet(OWNER_READ, OWNER_WRITE, OWNER_EXECUTE, OTHERS_EXECUTE, OTHERS_WRITE));
    CurrentUser.useDuring(other, new UserTask<Void>() {

      @Override
      public Void call() throws IOException {
        Files.delete(file);
        return null;
      }
    });
  }

  @Test
  void movePermission() throws IOException {
    FileSystem fileSystem = this.extension.getFileSystem();
    Path sourceDirectory = Files.createDirectory(fileSystem.getPath("source-directory"));
    Path targetDirectory = Files.createDirectory(fileSystem.getPath("target-directory"));
    final Path source = Files.createFile(sourceDirectory.resolve("file.txt"));
    final Path target = targetDirectory.resolve("file.txt");
    UserPrincipal user = fileSystem.getUserPrincipalLookupService().lookupPrincipalByName(PosixPermissionFileSystemExtension.OTHER);

    PosixFileAttributeView sourceView = Files.getFileAttributeView(sourceDirectory, PosixFileAttributeView.class);
    sourceView.setPermissions(asSet(OWNER_READ, OWNER_WRITE, OWNER_EXECUTE));

    PosixFileAttributeView targetView = Files.getFileAttributeView(targetDirectory, PosixFileAttributeView.class);
    targetView.setPermissions(asSet(OWNER_READ, OWNER_WRITE, OWNER_EXECUTE));

    // no write permission is not enough
    CurrentUser.useDuring(user, new UserTask<Void>() {

      @Override
      public Void call() throws IOException {
        try {
          Files.move(source, target);
          fail("should not be allowed to move files");
        } catch (AccessDeniedException e) {
          // should reach here
        }
        return null;
      }
    });

    sourceView.setPermissions(asSet(OWNER_READ, OWNER_WRITE, OWNER_EXECUTE, OTHERS_WRITE));
    targetView.setPermissions(asSet(OWNER_READ, OWNER_WRITE, OWNER_EXECUTE));

    // write and execute permission on source only is not enough
    CurrentUser.useDuring(user, new UserTask<Void>() {

      @Override
      public Void call() throws IOException {
        try {
          Files.move(source, target);
          fail("should not be allowed to move files");
        } catch (AccessDeniedException e) {
          // should reach here
        }
        return null;
      }
    });

    sourceView.setPermissions(asSet(OWNER_READ, OWNER_WRITE, OWNER_EXECUTE));
    targetView.setPermissions(asSet(OWNER_READ, OWNER_WRITE, OWNER_EXECUTE, OTHERS_WRITE));

    // write permission on target only is not enough
    CurrentUser.useDuring(user, new UserTask<Void>() {

      @Override
      public Void call() throws IOException {
        try {
          Files.move(source, target);
          fail("should not be allowed to move files");
        } catch (AccessDeniedException e) {
          // should reach here
        }
        return null;
      }
    });

    sourceView.setPermissions(asSet(OWNER_READ, OWNER_WRITE, OWNER_EXECUTE, OTHERS_EXECUTE, OTHERS_WRITE));
    targetView.setPermissions(asSet(OWNER_READ, OWNER_WRITE, OWNER_EXECUTE, OTHERS_EXECUTE, OTHERS_WRITE));

    // write and execute permission on source and target is enough
    CurrentUser.useDuring(user, new UserTask<Void>() {

      @Override
      public Void call() throws IOException {
        Files.move(source, target);
        return null;
      }
    });
  }

  @Test
  void createPermission() throws IOException {
    FileSystem fileSystem = this.extension.getFileSystem();
    Path sourceDirectory = Files.createDirectory(fileSystem.getPath("source-directory"));
    final Path file = sourceDirectory.resolve("file.txt");

    PosixFileAttributeView view = Files.getFileAttributeView(sourceDirectory, PosixFileAttributeView.class);
    view.setPermissions(asSet(OWNER_READ, OWNER_WRITE, OWNER_EXECUTE));

    UserPrincipal user = fileSystem.getUserPrincipalLookupService().lookupPrincipalByName(PosixPermissionFileSystemExtension.OTHER);

    CurrentUser.useDuring(user, new UserTask<Void>() {

      @Override
      public Void call() throws IOException {
        try {
          Files.createFile(file);
          fail("should not be allowed to create files");
        } catch (AccessDeniedException e) {
          // should reach here
        }
        return null;
      }
    });


    view.setPermissions(asSet(OWNER_READ, OWNER_WRITE, OWNER_EXECUTE, OTHERS_WRITE));
    CurrentUser.useDuring(user, new UserTask<Void>() {

      @Override
      public Void call() throws IOException {
        Files.createFile(file);
        return null;
      }
    });
  }

  @Test
  void issue135() throws IOException {
    FileSystem fileSystem = this.extension.getFileSystem();
    Path path = fileSystem.getPath("/test.txt");
    FileUtility.createAndSetContents(path, "Test");
    Files.setPosixFilePermissions(path, EnumSet.noneOf(PosixFilePermission.class));
    assertThrows(AccessDeniedException.class, () -> Files.readAllBytes(path));
  }

  @Test
  void readFromFileWithNoPermission() throws IOException {
    try (FileSystem fs = MemoryFileSystemBuilder.newLinux().build()) {
      Path path = fs.getPath("/test.txt");
      FileUtility.createAndSetContents(path, "Test");
      Files.setPosixFilePermissions(path, EnumSet.noneOf(PosixFilePermission.class));
      assertThrows(AccessDeniedException.class, () -> Files.readAllBytes(path));
    }
  }

}
