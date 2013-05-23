package org.jboss.as.patching.installation;

/**
 * @author Emanuel Muckenhuber
 */
public interface Identity extends PatchableTarget {

    /**
     * Get the identity name.
     *
     * @return the identity name
     */
    String getName();

    /**
     * Get the identity version.
     *
     * @return the identity version
     */
    String getVersion();

}
