package org.jboss.as.patching.runner;

/**
 * @author Emanuel Muckenhuber
 */
public interface PatchContentProvider {

    /**
     * Get the patch content loader for a given patch.
     *
     * @param patchId the patch id
     * @return the content loader
     */
    PatchContentLoader getLoader(String patchId);

    /**
     * Cleanup
     */
    void cleanup();

}