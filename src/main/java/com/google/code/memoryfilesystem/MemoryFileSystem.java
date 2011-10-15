package com.google.code.memoryfilesystem;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.WatchService;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.Collections;
import java.util.List;
import java.util.Set;

class MemoryFileSystem extends FileSystem {
	
	private final String separator;

	private final MemoryFileSystemProvider provider;
	
	private final MemoryFileStore store;
	
	private final Iterable<FileStore> stores;
	
	private final ClosedFileSystemChecker checker;

	private volatile List<Path> rootDirectories;

	private final MemoryUserPrincipalLookupService userPrincipalLookupService;

	MemoryFileSystem(String separator, MemoryFileSystemProvider provider, MemoryFileStore store,
			MemoryUserPrincipalLookupService userPrincipalLookupService, ClosedFileSystemChecker checker) {
		this.separator = separator;
		this.provider = provider;
		this.store = store;
		this.userPrincipalLookupService = userPrincipalLookupService;
		this.checker = checker;
		this.stores = Collections.<FileStore>singletonList(store);
	}
	
	

	/**
	 * Sets the root directories.
	 * 
	 * <p>This is a bit annoying.</p>
	 * 
	 * @param rootDirectories the root directories, not {@code null}, should
	 * 	not be modified, no defensive copy will be made
	 */
	void setRootDirectories(List<Path> rootDirectories) {
		this.rootDirectories = rootDirectories;
	}



	/**
	 * {@inheritDoc}
	 */
	@Override
	public FileSystemProvider provider() {
		this.checker.check();
		return this.provider;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void close() throws IOException {
		this.checker.close();
		this.provider.close(this);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isOpen() {
		return this.checker.isOpen();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isReadOnly() {
		this.checker.check();
		return this.store.isReadOnly();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getSeparator() {
		this.checker.check();
		return this.separator;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Iterable<Path> getRootDirectories() {
		this.checker.check();
		return this.rootDirectories;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Iterable<FileStore> getFileStores() {
		this.checker.check();
		return this.stores;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Set<String> supportedFileAttributeViews() {
		this.checker.check();
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Path getPath(String first, String... more) {
		this.checker.check();
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PathMatcher getPathMatcher(String syntaxAndPattern) {
		this.checker.check();
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public UserPrincipalLookupService getUserPrincipalLookupService() {
		this.checker.check();
		return this.userPrincipalLookupService;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public WatchService newWatchService() throws IOException {
		this.checker.check();
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	String getKey() {
		return this.store.getKey();
	}


	FileStore getFileStore() {
		return this.store;
	}


}
