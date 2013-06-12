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

    static class DefaultContentProvider implements PatchContentProvider {

        private final File workDir;
        private DefaultContentProvider(File tempDir) {
            this.workDir = tempDir;
        }

        @Override
        public PatchContentLoader getLoader(final String patchId) {
            final File root = new File(workDir, patchId);
            return PatchContentLoader.create(root);
        }

        @Override
        public File getPatchContentRootDir() {
            return workDir;
        }

        @Override
        public void cleanup() {
            // Nothing by default
        }

        /**
         * Create a default content provider.
         *
         * @param tempDir the temp dir
         * @return the content provider
         */
        static DefaultContentProvider create(final File tempDir) {
            return new DefaultContentProvider(tempDir);
        }

    }

}