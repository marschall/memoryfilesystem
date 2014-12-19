package com.github.marschall.memoryfilesystem;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public final class UmaskTest
{
    private static final Set<PosixFilePermission> UMASK
        = PosixFilePermissions.fromString("----w-rwx"); // ie, 027

    private static FileSystem FILESYSTEM;

    @BeforeClass
    public static void initfs()
        throws IOException
    {
        FILESYSTEM = MemoryFileSystemBuilder.newLinux().setUmask(UMASK)
            .build("UmaskTest");
    }

    @Test
    public void creatingFileWithoutPermissionsTakesUmaskIntoAccount()
        throws IOException
    {
        final Set<PosixFilePermission> expected
            = PosixFilePermissions.fromString("rwxr-x---");

        final Path path = FILESYSTEM.getPath("/fileWithoutPerms");

        final Path created = Files.createFile(path);

        final Set<PosixFilePermission> actual
            = Files.getPosixFilePermissions(created);

        assertEquals(expected, actual);
    }


    @Test
    public void creatingFileWithPermissionsTakesUmaskIntoAccount()
        throws IOException
    {
        final Set<PosixFilePermission> perms
            = PosixFilePermissions.fromString("rw-rw-rw-");
        final FileAttribute<?> attr
            = PosixFilePermissions.asFileAttribute(perms);

        final Path file = FILESYSTEM.getPath("/fileWithPerms");

        final Path created = Files.createFile(file, attr);

        final Set<PosixFilePermission> actual
            = Files.getPosixFilePermissions(created);
        final Set<PosixFilePermission> expected
            = PosixFilePermissions.fromString("rw-r-----");

        assertEquals(expected, actual);
    }

    @Test
    public void creatingDirectoryWithoutPermissionsTakesUmaskIntoAccount()
        throws IOException
    {
        final Set<PosixFilePermission> expected
            = PosixFilePermissions.fromString("rwxr-x---");

        final Path path = FILESYSTEM.getPath("/dirWithoutPerms");

        final Path created = Files.createDirectory(path);

        final Set<PosixFilePermission> actual
            = Files.getPosixFilePermissions(created);

        assertEquals(expected, actual);
    }


    @Test
    public void creatingDirectoryWithPermissionsTakesUmaskIntoAccount()
        throws IOException
    {
        final Set<PosixFilePermission> perms
            = PosixFilePermissions.fromString("rwxrwx-w-");
        final FileAttribute<?> attr
            = PosixFilePermissions.asFileAttribute(perms);

        final Path file = FILESYSTEM.getPath("/dirWithPerms");

        final Path created = Files.createDirectory(file, attr);

        final Set<PosixFilePermission> actual
            = Files.getPosixFilePermissions(created);
        final Set<PosixFilePermission> expected
            = PosixFilePermissions.fromString("rwxr-x---");

        assertEquals(expected, actual);
    }

    @AfterClass
    public static void closefs()
        throws IOException
    {
        FILESYSTEM.close();
    }
}
