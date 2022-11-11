package com.github.marschall.memoryfilesystem;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.attribute.UserPrincipal;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.attribute.UserPrincipalNotFoundException;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class MemoryUserPrincipalLookupServiceTest {

  @Test
  void identity() throws IOException {
    List<String> users = singletonList("user");
    List<String> groups = singletonList("group");
    StringTransformer transformer = StringTransformers.IDENTIY;
    UserPrincipalLookupService lookupService = MemoryUserPrincipalLookupService.newInstance(
            users, groups, transformer, new ClosedFileSystemChecker());

    UserPrincipal user = lookupService.lookupPrincipalByName("user");
    assertEquals("user", user.getName());
    assertEquals(user,lookupService.lookupPrincipalByName("user"));

    UserPrincipalNotFoundException e = assertThrows(UserPrincipalNotFoundException.class,
            () -> lookupService.lookupPrincipalByName("USER"),
            "lookup should fail");
    assertEquals("USER", e.getName());

    e = assertThrows(UserPrincipalNotFoundException.class,
            () -> lookupService.lookupPrincipalByName("group"),
            "lookup should fail");
    assertEquals("group", e.getName());

    UserPrincipal group = lookupService.lookupPrincipalByGroupName("group");
    assertEquals("group", group.getName());
    assertEquals(group,lookupService.lookupPrincipalByGroupName("group"));

    e = assertThrows(UserPrincipalNotFoundException.class,
            () -> lookupService.lookupPrincipalByGroupName("GROUP"),
            "lookup should fail");
    assertEquals("GROUP", e.getName());

    e = assertThrows(UserPrincipalNotFoundException.class,
            () -> lookupService.lookupPrincipalByGroupName("user"),
            "lookup should fail");
    assertEquals("user", e.getName());
  }

  @Test
  void caseInsensitive() throws IOException {
    List<String> users = singletonList("usEr");
    List<String> groups = singletonList("grOup");
    StringTransformer transformer = StringTransformers.caseInsensitive(Locale.US);
    UserPrincipalLookupService lookupService = MemoryUserPrincipalLookupService.newInstance(
            users, groups, transformer, new ClosedFileSystemChecker());

    UserPrincipal user = lookupService.lookupPrincipalByName("user");
    assertEquals("usEr", user.getName());
    assertEquals(user,lookupService.lookupPrincipalByName("USER"));

    UserPrincipalNotFoundException e = assertThrows(UserPrincipalNotFoundException.class,
            () -> lookupService.lookupPrincipalByName("group"),
            "lookup should fail");
    assertEquals("group", e.getName());

    UserPrincipal group = lookupService.lookupPrincipalByGroupName("group");
    assertEquals("grOup", group.getName());
    assertEquals(group,lookupService.lookupPrincipalByGroupName("GROUP"));

    e = assertThrows(UserPrincipalNotFoundException.class,
            () -> lookupService.lookupPrincipalByGroupName("user"),
            "lookup should fail");
    assertEquals("user", e.getName());
  }

}
