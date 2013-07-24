package org.jboss.as.patching.runner;

import java.io.File;

import org.jboss.as.patching.metadata.ContentItem;

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

    PatchContentProvider ROLLBACK_PROVIDER = new PatchContentProvider() {
        @Override
        public PatchContentLoader getLoader(String patchId) {
            return new PatchContentLoader() {
                @Override
                public File getFile(ContentItem item) {
                    throw new IllegalStateException();
                }
            };
        }

        @Override
        public void cleanup() {
            //
        }
    };

}