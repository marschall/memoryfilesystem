package com.google.code.memoryfilesystem;

interface AutoRelease extends AutoCloseable {
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void close();
	
}