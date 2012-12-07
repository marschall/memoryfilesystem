package com.github.marschall.memoryfilesystem;

import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.UserPrincipal;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.attribute.UserPrincipalNotFoundException;
import java.util.Map;
import java.util.Map.Entry;
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
    boolean copyAttribues = Options.isCopyAttribues(copyOptions);
    if (!targetExists) {
      Files.createDirectories(target);
    }

    Files.walkFileTree(source, new DirectoryCopier(source, target, copyOptions, linkOptions, sameFileSystem, copyAttribues));

    if (!targetExists && copyAttribues) {
      copyAttributes(source, target, sameFileSystem, linkOptions);
    }
  }

  private static LinkOption[] linkOptions(CopyOption[] copyOptions) {
    return Options.isFollowSymLinks(copyOptions) ? NO_LINK_OPTIONS : NOFOLLOW_LINKS;
  }

  private static void copyAttributes(Path source, Path target, boolean sameFileSystem, LinkOption[] linkOptions) throws IOException {
    Set<String> supportedFileAttributeViews = source.getFileSystem().supportedFileAttributeViews();
    for (String viewName : supportedFileAttributeViews) {
      // TODO UserDefinedFileAttributeView?
      Map<String, Object> attributes = Files.readAttributes(target, viewName + ":*", linkOptions);
      for (Entry<String, Object> attribute : attributes.entrySet()) {
        String attributeName = attribute.getKey();
        Object attributeValue = attribute.getValue();
        if (!sameFileSystem) {
          if (attributeValue instanceof UserPrincipal) {
            UserPrincipalLookupService userPrincipalLookupService = source.getFileSystem().getUserPrincipalLookupService();
            UserPrincipal user = (UserPrincipal) attributeValue;
            try {
              attributeValue = userPrincipalLookupService.lookupPrincipalByName(user.getName());
            } catch (UserPrincipalNotFoundException e) {
              // user doesn't exist in target file system, ignore
              continue;
            }
          } else if (attributeValue instanceof GroupPrincipal) {
            UserPrincipalLookupService userPrincipalLookupService = source.getFileSystem().getUserPrincipalLookupService();
            GroupPrincipal group = (GroupPrincipal) attributeValue;
            try {
              attributeValue = userPrincipalLookupService.lookupPrincipalByGroupName(group.getName());
            } catch (UserPrincipalNotFoundException e) {
              // group doesn't exist in target file system, ignore
              continue;
            }
          }
        }
        try {
          Files.setAttribute(target, attributeName, attributeValue, linkOptions);
        } catch (UnsupportedOperationException e) {
          // ignore, it's hard to tell in advance which attributes are writable and which not
          // so let's just not set it
        }
      }
    }
  }

  static final class DirectoryCopier extends SimpleFileVisitor<Path> {

    private final Path source;
    private final Path target;
    private final boolean sameProvider;
    private final LinkOption[] linkOptions;
    private final CopyOption[] copyOptions;
    private final boolean sameFileSystem;
    private final boolean copyAttribues;

    DirectoryCopier(Path source, Path target, CopyOption[] copyOptions, LinkOption[] linkOptions, boolean sameFileSystem, boolean copyAttribues) {
      this.source = source;
      this.target = target;
      this.copyOptions = copyOptions;
      this.linkOptions = linkOptions;
      this.sameFileSystem = sameFileSystem;
      this.copyAttribues = copyAttribues;
      this.sameProvider = source.getFileSystem().provider() == target.getFileSystem().provider();
    }

    private Path relativize(Path path) {
      Path relativized = this.source.relativize(path);
      if (this.sameProvider) {
        return this.target.resolve(relativized);
      } else {
        return this.target.resolve(relativized.toString());
      }
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
      Files.copy(file, this.relativize(file), this.copyOptions);
      return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
      Files.createDirectory(this.relativize(dir));
      return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
      if (this.copyAttribues) {
        copyAttributes(this.source, this.relativize(dir), this.sameFileSystem,
                this.linkOptions);
      }
      return FileVisitResult.CONTINUE;
    }
  }

}