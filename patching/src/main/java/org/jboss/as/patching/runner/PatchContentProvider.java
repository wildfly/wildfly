package org.jboss.as.patching.runner;

import java.io.File;

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
     * @return the root directory of the patch content
     */
    File getPatchContentRootDir();

    /**
     * Cleanup
     */
    void cleanup();

}