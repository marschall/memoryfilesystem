package com.github.marschall.memoryfilesystem;

import static java.nio.file.FileVisitResult.CONTINUE;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.CopyOption;
import java.nio.file.FileSystemException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.DosFileAttributeView;
import java.nio.file.attribute.DosFileAttributes;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.UserDefinedFileAttributeView;
import java.nio.file.attribute.UserPrincipal;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.attribute.UserPrincipalNotFoundException;
import java.util.HashSet;
import java.util.Set;

/**
 * Implements recursive copy missing in {@link Files}.
 */
public final class Directories {

  private static final LinkOption[] NO_LINK_OPTIONS = new LinkOption[0];
  private static final LinkOption[] NOFOLLOW_LINKS = new LinkOption[]{LinkOption.NOFOLLOW_LINKS};


  private Directories() {
    throw new AssertionError("not instantiable");
  }


  /**
   * Copy a directory to a target directory recursively.
   *
   * <p>This method performs a copy much like
   * {@link Files#copy(Path, Path, CopyOption...)}. Unlike
   * {@link Files#copy(Path, Path, CopyOption...)} is can also copy non-empty
   * directories.</p>
   *
   * <p>This method makes a best effort to copy attributes across
   * different file system providers.</p>
   *
   * <h2>Known Issues:</h2>
   * <ul>
   *  <li>hard links will not be handled correctly</li>
   * </ul>
   *
   * @see Files#copy(Path, Path, CopyOption...)
   *
   * @param source the path to the file to copy
   * @param target the path to the target file (may be associated with a different
   *               provider to the source path)
   * @param copyOptions options specifying how the copy should be done
   * @throws IOException if an I/O error occurs
   */
  public static void copyRecursive(Path source, Path target, CopyOption... copyOptions) throws IOException {
    boolean sameFileSystem = source.getFileSystem() == target.getFileSystem();
    LinkOption[] linkOptions = linkOptions(copyOptions);
    boolean targetExists = Files.exists(target, linkOptions);
    boolean copyAttributes = Options.isCopyAttributes(copyOptions);
    Set<Class<? extends FileAttributeView>> supportedAttributeViews = supportedAttributeViews(source, target, sameFileSystem);

    if (!targetExists) {
      Files.createDirectories(target);
    }

    FileVisitor<Path> copier = new DirectoryCopier(source, target, copyOptions, linkOptions, supportedAttributeViews, sameFileSystem, copyAttributes);
    Files.walkFileTree(source, copier);

    if (!targetExists && copyAttributes) {
      copyAttributes(source, target, sameFileSystem, supportedAttributeViews, linkOptions);
    }
  }

  private static LinkOption[] linkOptions(CopyOption[] copyOptions) {
    return Options.isFollowSymLinks(copyOptions) ? NO_LINK_OPTIONS : NOFOLLOW_LINKS;
  }

  private static Set<Class<? extends FileAttributeView>> supportedAttributeViews(Path source, Path target, boolean sameFileSystem) {
    Set<String> viewNames = source.getFileSystem().supportedFileAttributeViews();
    if (!sameFileSystem) {
      viewNames = new HashSet<>(viewNames); // can be unmodifyable
      viewNames.retainAll(target.getFileSystem().supportedFileAttributeViews());
    }
    Set<Class<? extends FileAttributeView>> supportedAttribueViews = new HashSet<>(viewNames.size());
    for (String string : viewNames) {
      supportedAttribueViews.add(FileAttributeViews.mapAttributeViewName(string));
    }
    return supportedAttribueViews;
  }

  private static void copyAttributes(Path source, Path target, boolean sameFileSystem, Set<Class<? extends FileAttributeView>> attributeViews, LinkOption[] linkOptions) throws IOException {

    BasicFileAttributes basicAttributes = Files.readAttributes(target, BasicFileAttributes.class, linkOptions);
    BasicFileAttributeView basicView = Files.getFileAttributeView(target, BasicFileAttributeView.class, linkOptions);
    basicView.setTimes(basicAttributes.lastModifiedTime(), basicAttributes.lastAccessTime(), basicAttributes.creationTime());

    if (attributeViews.contains(PosixFileAttributeView.class)) {
      PosixFileAttributes posixAttributes = Files.readAttributes(target, PosixFileAttributes.class, linkOptions);
      PosixFileAttributeView posixView = Files.getFileAttributeView(target, PosixFileAttributeView.class, linkOptions);
      posixView.setPermissions(posixAttributes.permissions());
      copyOwner(source, target, sameFileSystem, posixAttributes, posixView, linkOptions);
      copyGroup(source, target, sameFileSystem, posixAttributes, posixView, linkOptions);
    }

    if (attributeViews.contains(UserDefinedFileAttributeView.class)) {
      UserDefinedFileAttributeView sourceAttributes = Files.getFileAttributeView(source, UserDefinedFileAttributeView.class, linkOptions);
      UserDefinedFileAttributeView targetAttributes = Files.getFileAttributeView(target, UserDefinedFileAttributeView.class, linkOptions);

      // try to reuse the buffer
      // TODO reuse buffer across files
      ByteBuffer buffer = null;
      for (String each : sourceAttributes.list()) {
        int size = sourceAttributes.size(each);
        if (buffer == null) {
          buffer = ByteBuffer.allocate(size);
        } else {
          buffer.reset();
          if (buffer.capacity() < size) {
            buffer = ByteBuffer.allocate(size);
          }
        }
        int read = sourceAttributes.read(each, buffer);
        if (read != size) {
          throw new FileSystemException(source.toString(), null,"could not read attribute: " + each);
        }
        buffer.flip();
        int written = targetAttributes.write(each, buffer);
        if (written != size) {
          throw new FileSystemException(target.toString(), null, "could not read attribute: " + each);
        }

      }
    }

    if (attributeViews.contains(DosFileAttributeView.class)) {
      DosFileAttributes dosAttributes = Files.readAttributes(target, DosFileAttributes.class, linkOptions);
      DosFileAttributeView dosView = Files.getFileAttributeView(target, DosFileAttributeView.class, linkOptions);
      dosView.setArchive(dosAttributes.isArchive());
      dosView.setHidden(dosAttributes.isHidden());
      dosView.setSystem(dosAttributes.isSystem());
      dosView.setReadOnly(dosAttributes.isReadOnly());
    }
  }


  private static void copyOwner(Path source, Path target, boolean sameFileSystem, PosixFileAttributes posixAttributes, PosixFileAttributeView posixView, LinkOption[] linkOptions) throws IOException {
    UserPrincipal owner = posixAttributes.owner();
    if (!sameFileSystem) {
      UserPrincipalLookupService userPrincipalLookupService = source.getFileSystem().getUserPrincipalLookupService();
      try {
        owner = userPrincipalLookupService.lookupPrincipalByName(owner.getName());
      } catch (UserPrincipalNotFoundException e) {
        // user doesn't exist in target file system, ignore
        return;
      }
    }
    posixView.setOwner(owner);
  }

  private static void copyGroup(Path source, Path target, boolean sameFileSystem, PosixFileAttributes posixAttributes, PosixFileAttributeView posixView, LinkOption[] linkOptions) throws IOException {
    GroupPrincipal group = posixAttributes.group();
    if (!sameFileSystem) {
      UserPrincipalLookupService userPrincipalLookupService = source.getFileSystem().getUserPrincipalLookupService();
      try {
        group = userPrincipalLookupService.lookupPrincipalByGroupName(group.getName());
      } catch (UserPrincipalNotFoundException e) {
        // user doesn't exist in target file system, ignore
        return;
      }
    }
    posixView.setGroup(group);
  }

  static final class DirectoryCopier extends SimpleFileVisitor<Path> {

    private final Path source;
    private final Path target;
    private final LinkOption[] linkOptions;
    private final CopyOption[] copyOptions;
    private final boolean sameFileSystem;
    private final boolean copyAttributes;
    private final Set<Class<? extends FileAttributeView>> supportedAttributeViews;

    DirectoryCopier(Path source, Path target, CopyOption[] copyOptions, LinkOption[] linkOptions, Set<Class<? extends FileAttributeView>> supportedAttributeViews, boolean sameFileSystem, boolean copyAttributes) {
      this.source = source;
      this.target = target;
      this.copyOptions = copyOptions;
      this.linkOptions = linkOptions;
      this.supportedAttributeViews = supportedAttributeViews;
      this.sameFileSystem = sameFileSystem;
      this.copyAttributes = copyAttributes;
    }

    private Path relativize(Path path) {
      Path relativized = this.source.relativize(path);
      if (this.sameFileSystem) {
        // TODO would same provider be enough?
        return this.target.resolve(relativized);
      } else {
        return this.target.resolve(relativized.toString());
      }
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
      Files.copy(file, this.relativize(file), this.copyOptions);
      return CONTINUE;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
      if (!dir.equals(dir.getRoot())) {
        // skip creating root on target file system
        Files.createDirectory(this.relativize(dir));
      }
      return CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
      if (this.copyAttributes) {
        copyAttributes(this.source, this.relativize(dir), this.sameFileSystem, this.supportedAttributeViews, this.linkOptions);
      }
      return CONTINUE;
    }
  }

}
