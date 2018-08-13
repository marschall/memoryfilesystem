package com.github.marschall.memoryfilesystem;

import java.io.IOException;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.UserPrincipal;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.attribute.UserPrincipalNotFoundException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class MemoryUserPrincipalLookupService extends UserPrincipalLookupService {

  private final Map<String, UserPrincipal> users;
  private final Map<String, GroupPrincipal> groups;
  private final StringTransformer stringTransformer;
  private final ClosedFileSystemChecker checker;

  private final UserPrincipal defaultUser;

  private MemoryUserPrincipalLookupService(Map<String, UserPrincipal> users,
          Map<String, GroupPrincipal> groups, UserPrincipal defaultUser,
          StringTransformer stringTransformer, ClosedFileSystemChecker checker) {
    this.checker = checker;
    this.users = users;
    this.groups = groups;
    this.defaultUser = defaultUser;
    this.stringTransformer = stringTransformer;
  }

  static MemoryUserPrincipalLookupService newInstance(List<String> userNames, List<String> groupNames,
          StringTransformer stringTransformer, ClosedFileSystemChecker checker) {

    Map<String, UserPrincipal> users;
    Map<String, GroupPrincipal> groups;

    UserPrincipal defaultUser = null;

    if (userNames.size() == 1) {
      String userName = userNames.get(0);
      UserPrincipal user = new MemoryUser(userName);
      defaultUser = user;
      String key = stringTransformer.transform(userName);
      users = Collections.singletonMap(key, user);
    } else {
      users = new HashMap<>(userNames.size());
      for (String userName : userNames) {
        UserPrincipal user = new MemoryUser(userName);
        if (defaultUser == null) {
          defaultUser = user;
        }
        String key = stringTransformer.transform(userName);
        users.put(key, user);
      }
    }

    if (groupNames.size() == 1) {
      String groupName = groupNames.get(0);
      GroupPrincipal group = new MemoryGroup(groupName);
      String key = stringTransformer.transform(groupName);
      groups = Collections.singletonMap(key, group);
    } else {
      groups = new HashMap<>(groupNames.size());
      for (String groupName : groupNames) {
        GroupPrincipal group = new MemoryGroup(groupName);
        String key = stringTransformer.transform(groupName);
        groups.put(key, group);
      }
    }
    return new MemoryUserPrincipalLookupService(users, groups, defaultUser, stringTransformer, checker);
  }

  UserPrincipal getDefaultUser() {
    return this.defaultUser;
  }


  @Override
  public UserPrincipal lookupPrincipalByName(String name) throws IOException {
    this.checker.check();
    String key = this.stringTransformer.transform(name);
    UserPrincipal user = this.users.get(key);
    if (user != null) {
      return user;
    } else {
      throw new UserPrincipalNotFoundException(name);
    }
  }


  @Override
  public GroupPrincipal lookupPrincipalByGroupName(String groupName) throws IOException {
    this.checker.check();
    String key = this.stringTransformer.transform(groupName);
    GroupPrincipal group = this.groups.get(key);
    if (group != null) {
      return group;
    } else {
      throw new UserPrincipalNotFoundException(groupName);
    }
  }

  static abstract class MemoryPrincipal implements UserPrincipal {

    private final String name;


    MemoryPrincipal(String name) {
      this.name = name;
    }


    @Override
    public String getName() {
      return this.name;
    }

    @Override
    public String toString() {
      return this.getName();
    }
  }

  static final class MemoryUser extends MemoryPrincipal {

    MemoryUser(String name) {
      super(name);
    }
  }

  static final class MemoryGroup extends MemoryPrincipal implements GroupPrincipal {


    MemoryGroup(String name) {
      super(name);
    }

  }

}
