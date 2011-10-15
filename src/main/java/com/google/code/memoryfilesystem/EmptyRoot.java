package com.google.code.memoryfilesystem;

import java.net.URI;
import java.net.URISyntaxException;

class EmptyRoot extends Root {
	
	EmptyRoot(MemoryFileSystem fileSystem) {
		super(fileSystem);
	}
	


	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean startsWith(String other) {
		// intentionally trigger NPE if other is null (default file system behaves the same way)
		return other.equals("/");
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean endsWith(String other) {
		// intentionally trigger NPE if other is null (default file system behaves the same way)
		return other.equals("/");
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return "/";
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public URI toUri() {
		try {
			return new URI(MemoryFileSystemProvider.SCHEME, getMemoryFileSystem().getKey() + ":///", null);
		} catch (URISyntaxException e) {
			throw new AssertionError("could not create URI");
		}
	}

}
