package com.github.marschall.memoryfilesystem;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.file.attribute.UserPrincipal;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.attribute.UserPrincipalNotFoundException;
import java.util.List;
import java.util.Locale;

import org.junit.Test;

public class MemoryUserPrincipalLookupServiceTest {

  @Test
  public void identiy() throws IOException {
    List<String> users = singletonList("user");
    List<String> groups = singletonList("group");
    StringTransformer transformer = StringTransformers.IDENTIY;
    UserPrincipalLookupService lookupService = new MemoryUserPrincipalLookupService(users, groups, transformer,
            new ClosedFileSystemChecker());

    UserPrincipal user = lookupService.lookupPrincipalByName("user");
    assertEquals("user", user.getName());
    assertEquals(user,lookupService.lookupPrincipalByName("user"));

    try {
      lookupService.lookupPrincipalByName("USER");
      fail("lookup should fail");
    } catch (UserPrincipalNotFoundException e) {
      assertEquals("USER", e.getName());
    }

    try {
      lookupService.lookupPrincipalByName("group");
      fail("lookup should fail");
    } catch (UserPrincipalNotFoundException e) {
      assertEquals("group", e.getName());
    }

    UserPrincipal group = lookupService.lookupPrincipalByGroupName("group");
    assertEquals("group", group.getName());
    assertEquals(group,lookupService.lookupPrincipalByGroupName("group"));

    try {
      lookupService.lookupPrincipalByGroupName("GROUP");
      fail("lookup should fail");
    } catch (UserPrincipalNotFoundException e) {
      assertEquals("GROUP", e.getName());
    }

    try {
      lookupService.lookupPrincipalByGroupName("user");
      fail("lookup should fail");
    } catch (UserPrincipalNotFoundException e) {
      assertEquals("user", e.getName());
    }
  }

  @Test
  public void caseInsenstive() throws IOException {
    List<String> users = singletonList("usEr");
    List<String> groups = singletonList("grOup");
    StringTransformer transformer = StringTransformers.caseInsensitive(Locale.US);
    UserPrincipalLookupService lookupService = new MemoryUserPrincipalLookupService(users, groups, transformer,
            new ClosedFileSystemChecker());

    UserPrincipal user = lookupService.lookupPrincipalByName("user");
    assertEquals("usEr", user.getName());
    assertEquals(user,lookupService.lookupPrincipalByName("USER"));

    try {
      lookupService.lookupPrincipalByName("group");
      fail("lookup should fail");
    } catch (UserPrincipalNotFoundException e) {
      assertEquals("group", e.getName());
    }

    UserPrincipal group = lookupService.lookupPrincipalByGroupName("group");
    assertEquals("grOup", group.getName());
    assertEquals(group,lookupService.lookupPrincipalByGroupName("GROUP"));

    try {
      lookupService.lookupPrincipalByGroupName("user");
      fail("lookup should fail");
    } catch (UserPrincipalNotFoundException e) {
      assertEquals("user", e.getName());
    }
  }

}
