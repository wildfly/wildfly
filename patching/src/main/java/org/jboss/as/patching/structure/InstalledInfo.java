package org.jboss.as.patching.structure;

/**
 * @author Emanuel Muckenhuber
 */
public interface InstalledInfo {

    /**
     * Get a descriptive name for the installation.
     *
     * @return the installation name
     */
    String getInstallationName();

    /**
     * Get the currently installed version.
     *
     * @return the version
     */
    String getVersion();

}
