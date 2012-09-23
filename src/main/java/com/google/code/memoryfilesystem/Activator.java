package com.google.code.memoryfilesystem;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * Ensures smooth operation in an OSGi&trade; container.
 * 
 * <p>Once this bundle is stopped it will uninstall the the provider
 * preventing any class leaks from happening. See
 * {@link MemoryFileSystemUninstaller} for more information.</p>
 * 
 * <p>This class should only be used by the OSGi&trade; runtime.</p>
 */
public final class Activator implements BundleActivator {


  @Override
  public void start(BundleContext context) {
    // do nothing
  }


  @Override
  public void stop(BundleContext context) {
    //REVIEW close all file systems?
    MemoryFileSystemUninstaller.uninstall();
  }

}
