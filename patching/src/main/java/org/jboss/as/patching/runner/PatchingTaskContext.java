package org.jboss.as.patching.runner;

import java.io.File;
import java.io.IOException;

import org.jboss.as.patching.metadata.ContentItem;
import org.jboss.as.patching.metadata.ContentModification;
import org.jboss.as.patching.metadata.MiscContentItem;

/**
 * @author Emanuel Muckenhuber
 */
public interface PatchingTaskContext {

    /**
     * Get the target location for a given content item.
     *
     * @param item the content item
     * @return the target location
     */
    File getTargetFile(ContentItem item);

    /**
     * Get the backup location for a misc file.
     *
     * @param item the misc content item
     * @return the backup file location
     */
    File getBackupFile(MiscContentItem item);

    /**
     * Check the content verification policy whether a given
     * content item can be excluded or not.
     *
     * @param contentItem the content item
     * @return whether the content can be excluded or not
     */
    boolean isExcluded(ContentItem contentItem);

    /**
     * Record a change to be included in the patch history.
     *
     * @param change         the modification
     * @param rollbackAction the rollback action
     */
    void recordChange(ContentModification change, ContentModification rollbackAction);

    /**
     * Store content for further reuse.
     *
     * @param hash the content hash
     * @param file the content root
     * @param move try to move the file
     * @throws IOException
     */
    void store(byte[] hash, File file, boolean move) throws IOException;

    /**
     * Get the current bundle path.
     *
     * @return the bundle path
     */
    File[] getTargetBundlePath();

    /**
     * Get the current module path.
     *
     * @return the module path
     */
    File[] getTargetModulePath();

}
