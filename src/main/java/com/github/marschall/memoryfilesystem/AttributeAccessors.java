package com.github.marschall.memoryfilesystem;

import java.io.IOException;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.DosFileAttributeView;
import java.nio.file.attribute.DosFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileOwnerAttributeView;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

final class AttributeAccessors {

  private static final int DOS_ATTRIBUTE_COUNT = 4;
  private static final int OWNER_ATTRIBUTE_COUNT = 1;
  private static final int BASIC_ATTRIBUTE_COUNT = 9;
  private static final Map<String, Map<String, AttributeAccessor>> ACCESSORS;

  static {
    ACCESSORS = new HashMap<>(4);

    Map<String, AttributeAccessor> basicFileAttributesMap = buildBasicFileAttributesMap();
    Map<String, AttributeAccessor> ownerAttributesMap = buildOwnerAttributesMap();
    Map<String, AttributeAccessor> dosAttributesMap = buildDosAttributesMap();
    Map<String, AttributeAccessor> posixAttributesMap = buildPosixAttributesMap();

    dosAttributesMap.putAll(basicFileAttributesMap);
    posixAttributesMap.putAll(ownerAttributesMap);
    posixAttributesMap.putAll(basicFileAttributesMap);

    ACCESSORS.put(FileAttributeViews.BASIC, basicFileAttributesMap);
    ACCESSORS.put(FileAttributeViews.OWNER, ownerAttributesMap);
    ACCESSORS.put(FileAttributeViews.DOS, dosAttributesMap);
    ACCESSORS.put(FileAttributeViews.POSIX, posixAttributesMap);
  }

  AttributeAccessors() {
    throw new AssertionError("not instantiable");
  }

  private static Map<String, AttributeAccessor> buildOwnerAttributesMap() {
    return Collections.<String, AttributeAccessor>singletonMap("owner", new OwnerAccessor());
  }

  private static Map<String, AttributeAccessor> buildDosAttributesMap() {
    Map<String, AttributeAccessor> map = new HashMap<>(DOS_ATTRIBUTE_COUNT + BASIC_ATTRIBUTE_COUNT);
    map.put("readonly", new IsReadOnlyAccessor());
    map.put("hidden", new IsHiddenAccessor());
    map.put("archive", new IsArchiveAccessor());
    map.put("system", new IsSymbolicLinkAccessor());
    return map;
  }

  private static Map<String, AttributeAccessor> buildPosixAttributesMap() {
    Map<String, AttributeAccessor> map = new HashMap<>(OWNER_ATTRIBUTE_COUNT + 2 + BASIC_ATTRIBUTE_COUNT);
    // owner excluded because it's inherited
    map.put("group", new GroupAccessor());
    map.put("permissions", new PermissionsAccessor());
    return map;
  }

  private static Map<String, AttributeAccessor> buildBasicFileAttributesMap() {
    Map<String, AttributeAccessor> map = new HashMap<>(BASIC_ATTRIBUTE_COUNT);
    map.put("lastModifiedTime", new LastModifiedTimeAccessor());
    map.put("lastAccessTime", new LastAccessTimeAccessor());
    map.put("creationTime", new CreationTimeAccessor());
    map.put("isRegularFile", new IsRegularFileAccessor());
    map.put("isDirectory", new IsDirectoryAccessor());
    map.put("isSymbolicLink", new IsSymbolicLinkAccessor());
    map.put("isOther", new IsOtherAccessor());
    map.put("size", new SizeAccessor());
    map.put("fileKey", new FileKeyAccessor());
    return map;
  }

  static final class LastModifiedTimeAccessor implements AttributeAccessor {

    @Override
    public Object readAttribute(AtributeReadContext context) throws IOException {
      return context.getBasicFileAttributes().lastModifiedTime();
    }

    @Override
    public void writeAttribute(Object value, MemoryEntry entry) throws IOException {
      entry.getBasicFileAttributeView().setTimes((FileTime) value, null, null);
    }

  }

  static final class LastAccessTimeAccessor implements AttributeAccessor {

    @Override
    public Object readAttribute(AtributeReadContext context) throws IOException {
      return context.getBasicFileAttributes().lastAccessTime();
    }

    @Override
    public void writeAttribute(Object value, MemoryEntry entry) throws IOException {
      entry.getBasicFileAttributeView().setTimes(null, (FileTime) value, null);
    }

  }

  static final class CreationTimeAccessor implements AttributeAccessor {

    @Override
    public Object readAttribute(AtributeReadContext context) throws IOException {
      return context.getBasicFileAttributes().creationTime();
    }

    @Override
    public void writeAttribute(Object value, MemoryEntry entry) throws IOException {
      entry.getBasicFileAttributeView().setTimes(null, null, (FileTime) value);
    }

  }

  static final class IsRegularFileAccessor implements AttributeAccessor {

    @Override
    public Object readAttribute(AtributeReadContext context) throws IOException {
      return context.getBasicFileAttributes().isRegularFile();
    }

    @Override
    public void writeAttribute(Object value, MemoryEntry entry) {
      throw new IllegalArgumentException("\"isRegularFile\" can not be written");
    }

  }

  static final class IsDirectoryAccessor implements AttributeAccessor {

    @Override
    public Object readAttribute(AtributeReadContext context) throws IOException {
      return context.getBasicFileAttributes().isDirectory();
    }

    @Override
    public void writeAttribute(Object value, MemoryEntry entry) {
      throw new IllegalArgumentException("\"isDirectory\" can not be written");
    }

  }

  static final class IsOtherAccessor implements AttributeAccessor {

    @Override
    public Object readAttribute(AtributeReadContext context) throws IOException {
      return context.getBasicFileAttributes().isOther();
    }

    @Override
    public void writeAttribute(Object value, MemoryEntry entry) {
      throw new IllegalArgumentException("\"isOther\" can not be written");
    }

  }

  static final class IsSymbolicLinkAccessor implements AttributeAccessor {

    @Override
    public Object readAttribute(AtributeReadContext context) throws IOException {
      return context.getBasicFileAttributes().isSymbolicLink();
    }

    @Override
    public void writeAttribute(Object value, MemoryEntry entry) {
      throw new IllegalArgumentException("\"isSymbolicLink\" can not be written");
    }

  }

  static final class SizeAccessor implements AttributeAccessor {

    @Override
    public Object readAttribute(AtributeReadContext context) throws IOException {
      return context.getBasicFileAttributes().size();
    }

    @Override
    public void writeAttribute(Object value, MemoryEntry entry) {
      throw new IllegalArgumentException("\"size\" can not be written");
    }

  }

  static final class FileKeyAccessor implements AttributeAccessor {

    @Override
    public Object readAttribute(AtributeReadContext context) throws IOException {
      return context.getBasicFileAttributes().fileKey();
    }

    @Override
    public void writeAttribute(Object value, MemoryEntry entry) {
      throw new IllegalArgumentException("\"fileKey\" can not be written");
    }

  }

  static final class OwnerAccessor implements AttributeAccessor {

    @Override
    public Object readAttribute(AtributeReadContext context) throws IOException {
      return context.getFileOwnerAttributeView().getOwner();
    }

    @Override
    public void writeAttribute(Object value, MemoryEntry entry) throws IOException {
      entry.getFileAttributeView(FileOwnerAttributeView.class).setOwner((UserPrincipal) value);
    }

  }

  static final class GroupAccessor implements AttributeAccessor {

    @Override
    public Object readAttribute(AtributeReadContext context) throws IOException {
      return context.getPosixFileAttributes().group();
    }

    @Override
    public void writeAttribute(Object value, MemoryEntry entry) throws IOException {
      entry.getFileAttributeView(PosixFileAttributeView.class).setGroup((GroupPrincipal) value);
    }

  }

  static final class PermissionsAccessor implements AttributeAccessor {

    @Override
    public Object readAttribute(AtributeReadContext context) throws IOException {
      return context.getPosixFileAttributes().permissions();
    }

    @Override
    public void writeAttribute(Object value, MemoryEntry entry) throws IOException {
      entry.getFileAttributeView(PosixFileAttributeView.class).setPermissions((Set<PosixFilePermission>) value);
    }

  }

  static final class IsHiddenAccessor implements AttributeAccessor {

    @Override
    public Object readAttribute(AtributeReadContext context) throws IOException {
      return context.getDosFileAttributes().isHidden();
    }

    @Override
    public void writeAttribute(Object value, MemoryEntry entry) throws IOException {
      entry.getFileAttributeView(DosFileAttributeView.class).setHidden((Boolean) value);
    }

  }

  static final class IsReadOnlyAccessor implements AttributeAccessor {

    @Override
    public Object readAttribute(AtributeReadContext context) throws IOException {
      return context.getDosFileAttributes().isReadOnly();
    }

    @Override
    public void writeAttribute(Object value, MemoryEntry entry) throws IOException {
      entry.getFileAttributeView(DosFileAttributeView.class).setReadOnly((Boolean) value);
    }

  }

  static final class IsSystemAccessor implements AttributeAccessor {

    @Override
    public Object readAttribute(AtributeReadContext context) throws IOException {
      return context.getDosFileAttributes().isSystem();
    }

    @Override
    public void writeAttribute(Object value, MemoryEntry entry) throws IOException {
      entry.getFileAttributeView(DosFileAttributeView.class).setSystem((Boolean) value);
    }

  }

  static final class IsArchiveAccessor implements AttributeAccessor {

    @Override
    public Object readAttribute(AtributeReadContext context) throws IOException {
      return context.getDosFileAttributes().isArchive();
    }

    @Override
    public void writeAttribute(Object value, MemoryEntry entry) throws IOException {
      entry.getFileAttributeView(DosFileAttributeView.class).setArchive((Boolean) value);
    }

  }

  static void setAttribute(MemoryEntry entry, String attribute, Object value) throws IOException {
    getAccessor(attribute).writeAttribute(value, entry);
  }

  static void setAttributes(MemoryEntry entry, FileAttribute<?>... attrs) throws IOException {
    for (FileAttribute<?> attribute : attrs) {
      getAccessor(attribute.name()).writeAttribute(attribute.value(), entry);
    }
  }

  static Map<String, Object> readAttributes(MemoryEntry entry, String viewAndAttribute) throws IOException {
    int colonIndex = viewAndAttribute.indexOf(':');
    if (colonIndex == viewAndAttribute.length() - 1) {
      throw new IllegalArgumentException("\"" + viewAndAttribute + "\" is missing attribute name");
    }

    String view;
    String attribute;
    if (colonIndex == -1) {
      view = FileAttributeViews.BASIC;
      attribute = viewAndAttribute;
    } else {
      view = viewAndAttribute.substring(0, colonIndex);
      attribute = viewAndAttribute.substring(colonIndex + 1);
    }

    Map<String, AttributeAccessor> viewMap = ACCESSORS.get(view);
    if (viewMap == null) {
      throw new UnsupportedOperationException("view \"" + view + "\" is not supported");
    }

    if (attribute.equals("*")) {
      return readAllAttributes(entry, viewMap);
    } else if (attribute.indexOf(',') != -1) {
      String[] attributes = attribute.split(","); // should end up in the fast path
      return readAttributes(entry, attributes, viewMap);
    } else {
      return readSingleAttribute(entry, attribute, viewMap);
    }

  }

  private static Map<String, Object> readSingleAttribute(MemoryEntry entry, String attribute, Map<String, AttributeAccessor> viewMap) throws IOException {
    AttributeAccessor accessor = viewMap.get(attribute);
    if (accessor == null) {
      throw new IllegalArgumentException("attribute \"" + attribute + "\" is not supported");

    }
    AtributeReadContext context = new AtributeReadContext(entry);
    Object value = accessor.readAttribute(context);
    return Collections.singletonMap(attribute, value);
  }

  private static Map<String, Object> readAttributes(MemoryEntry entry, String[] attributes, Map<String, AttributeAccessor> viewMap) throws IOException {
    Map<String, Object> values = new HashMap<>(attributes.length);
    AtributeReadContext context = new AtributeReadContext(entry);
    for (String each : attributes) {
      AttributeAccessor accessor = viewMap.get(each);
      if (accessor == null) {
        throw new IllegalArgumentException("attribute \"" + each + "\" is not supported");
      }
      values.put(each, accessor.readAttribute(context));
    }
    return values;
  }

  private static Map<String, Object> readAllAttributes(MemoryEntry entry, Map<String, AttributeAccessor> viewMap) throws IOException {
    Map<String, Object> values = new HashMap<>(viewMap.size());
    AtributeReadContext context = new AtributeReadContext(entry);
    for (Entry<String, AttributeAccessor> each : viewMap.entrySet()) {
      AttributeAccessor accessor = each.getValue();
      values.put(each.getKey(), accessor.readAttribute(context));
    }
    return values;
  }

  private static AttributeAccessor getAccessor(String viewAndAttribute) {
    int colonIndex = viewAndAttribute.indexOf(':');
    if (colonIndex == viewAndAttribute.length() - 1) {
      throw new IllegalArgumentException("\"" + viewAndAttribute + "\" is missing attribute name");
    }

    String view;
    String attribute;
    if (colonIndex == -1) {
      view = FileAttributeViews.BASIC;
      attribute = viewAndAttribute;
    } else {
      view = viewAndAttribute.substring(0, colonIndex);
      attribute = viewAndAttribute.substring(colonIndex + 1);
    }

    Map<String, AttributeAccessor> viewMap = ACCESSORS.get(view);
    if (viewMap == null) {
      throw new UnsupportedOperationException("view \"" + view + "\" is not supported");
    }
    AttributeAccessor accessor = viewMap.get(attribute);
    if (accessor == null) {
      throw new IllegalArgumentException("view \"" + view + "\" is not supported");

    }
    return accessor;
  }

  static final class AtributeReadContext {

    private final MemoryEntry entry;

    private BasicFileAttributes basicFileAttributes;
    private DosFileAttributes dosFileAttributes;
    private PosixFileAttributes posixFileAttributes;
    private FileOwnerAttributeView fileOwnerAttributeView;

    AtributeReadContext(MemoryEntry entry) {
      this.entry = entry;
    }

    BasicFileAttributes getBasicFileAttributes() throws IOException {
      if (this.basicFileAttributes == null) {
        this.basicFileAttributes = this.entry.getFileAttributeView(BasicFileAttributeView.class).readAttributes();
      }
      return this.basicFileAttributes;
    }

    public DosFileAttributes getDosFileAttributes() throws IOException {
      if (this.dosFileAttributes == null) {
        this.dosFileAttributes = this.entry.getFileAttributeView(DosFileAttributeView.class).readAttributes();
      }
      return this.dosFileAttributes;
    }

    public PosixFileAttributes getPosixFileAttributes() throws IOException {
      if (this.posixFileAttributes == null) {
        this.posixFileAttributes = this.entry.getFileAttributeView(PosixFileAttributeView.class).readAttributes();
      }
      return this.posixFileAttributes;
    }

    public FileOwnerAttributeView getFileOwnerAttributeView() throws IOException {
      if (this.fileOwnerAttributeView == null) {
        this.fileOwnerAttributeView = this.entry.getFileAttributeView(FileOwnerAttributeView.class);
      }
      return this.fileOwnerAttributeView;
    }

  }

  interface AttributeAccessor {

    Object readAttribute(AtributeReadContext context) throws IOException;

    void writeAttribute(Object value, MemoryEntry entry) throws IOException;

  }

}
