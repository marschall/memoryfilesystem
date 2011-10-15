package com.google.code.memoryfilesystem;

import java.io.IOException;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.UserPrincipal;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.attribute.UserPrincipalNotFoundException;
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class MemoryUserPrincipalLookupService extends UserPrincipalLookupService {
	
	private final Map<String, UserPrincipal> users;
	private final Map<String, GroupPrincipal> groups;
	private final StringTransformer stringTransformer;
	private final ClosedFileSystemChecker checker;
	
	public MemoryUserPrincipalLookupService(List<String> userNames, List<String> groupNames,
			StringTransformer stringTransformer, ClosedFileSystemChecker checker) {
		this.checker = checker;
		this.users = new HashMap<>(userNames.size());
		this.groups = new HashMap<>(groupNames.size());
		for (String userName : userNames) {
			UserPrincipal user = new MemoryUser(userName);
			String key = stringTransformer.tranform(userName);
			this.users.put(key, user);
		}
		for (String groupName : groupNames) {
			GroupPrincipal group = new MemoryGroup(groupName);
			String key = stringTransformer.tranform(groupName);
			this.groups.put(key, group);
		}
		this.stringTransformer = stringTransformer;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public UserPrincipal lookupPrincipalByName(String name) throws IOException {
		this.checker.check();
		String key = this.stringTransformer.tranform(name);
		UserPrincipal user = this.users.get(key);
		if (user != null) {
			return user;
		} else {
			throw new UserPrincipalNotFoundException(name);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public GroupPrincipal lookupPrincipalByGroupName(String groupName) throws IOException {
		this.checker.check();
		String key = this.stringTransformer.tranform(groupName);
		GroupPrincipal group = this.groups.get(key);
		if (group != null) {
			return group;
		} else {
			throw new UserPrincipalNotFoundException(groupName);
		}
	}
	
	static abstract class MemoryPrincial implements Principal {
		
		private final String name;
		
		
		MemoryPrincial(String name) {
			this.name = name;
		}
		
		
		/**
		 * {@inheritDoc}
		 */
		@Override
		public String getName() {
			return this.name;
		}
		
		/**
		 * {@inheritDoc}
		 */
		@Override
		public String toString() {
			return this.getName();
		}
	}
	
	static final class MemoryUser extends MemoryPrincial implements UserPrincipal {
		
		MemoryUser(String name) {
			super(name);
		}
	}
	
	static final class MemoryGroup extends MemoryPrincial implements GroupPrincipal {
		

		MemoryGroup(String name) {
			super(name);
		}

	}

}
