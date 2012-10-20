package com.github.marschall.memoryfilesystem;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

final class AttributeAccessors {

  private static final Map<String, Map<String, AttributeAccessor>> ACCESSORS;

  static {
    ACCESSORS = new HashMap<>();
  }

  AttributeAccessors() {
    throw new AssertionError("not instantiable");
  }

  static void setAttribute(MemoryEntry entry, String attribute, Object value) {
    getAccessor(attribute).writeAttribute(value, entry);
  }

  static Map<String, Object> readAttributes(MemoryEntry entry, String viewAndAttribute) {
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
      attribute = viewAndAttribute.substring(colonIndex + 1, viewAndAttribute.length());
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

  private static Map<String, Object> readSingleAttribute(MemoryEntry entry, String attribute, Map<String, AttributeAccessor> viewMap) {
    AttributeAccessor accessor = viewMap.get(attribute);
    if (accessor == null) {
      throw new IllegalArgumentException("attribute \"" + attribute + "\" is not supported");

    }
    return Collections.singletonMap(attribute, accessor.readAttribute(entry));
  }

  private static Map<String, Object> readAttributes(MemoryEntry entry, String[] attributes, Map<String, AttributeAccessor> viewMap) {
    Map<String, Object> values = new HashMap<>(attributes.length);
    for (String each : attributes) {
      AttributeAccessor accessor = viewMap.get(each);
      if (accessor == null) {
        throw new IllegalArgumentException("attribute \"" + each + "\" is not supported");
      }
      values.put(each, accessor.readAttribute(entry));
    }
    return values;
  }

  private static Map<String, Object> readAllAttributes(MemoryEntry entry, Map<String, AttributeAccessor> viewMap) {
    Map<String, Object> values = new HashMap<>(viewMap.size());
    for (Entry<String, AttributeAccessor> each : viewMap.entrySet()) {
      AttributeAccessor accessor = each.getValue();
      values.put(each.getKey(), accessor.readAttribute(entry));
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
      attribute = viewAndAttribute.substring(colonIndex + 1, viewAndAttribute.length());
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

  interface AttributeAccessor {

    Object readAttribute(MemoryEntry entry);

    void writeAttribute(Object value, MemoryEntry entry);

  }

}
