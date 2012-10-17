/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.patching.runner;

import org.jboss.as.patching.PatchLogger;
import org.jboss.as.patching.metadata.ContentItem;
import org.jboss.as.patching.metadata.ContentModification;
import org.jboss.as.patching.metadata.ModificationType;

import java.io.IOException;
import java.util.Arrays;

/**
 * Basic patching task implementation.
 *
 * @author Emanuel Muckenhuber
 */
abstract class AbstractPatchingTask<T extends ContentItem> implements PatchingTask {

    protected final T contentItem;
    protected final PatchingTaskDescription description;

    private boolean skipExecute; // Skip the execution step
    private byte[] backupHash = NO_CONTENT; // The backup hash

    AbstractPatchingTask(PatchingTaskDescription description, Class<T> expected) {
        this.description = description;
        this.contentItem = description.getContentItem(expected);
    }

    @Override
    public T getContentItem() {
        return contentItem;
    }

    /**
     * Backup the content.
     *
     * @param context the patching context
     * @return the hash of the content
     * @throws IOException
     */
    abstract byte[] backup(PatchingContext context) throws IOException;

    /**
     * Apply the modification.
     *
     * @param context the patching context
     * @param loader the patch content loader
     * @return the actual copied content hash
     * @throws IOException
     */
    abstract byte[] apply(final PatchingContext context, final PatchContentLoader loader) throws IOException;

    /**
     * Create the rollback entry.
     *
     * @param original the original content modification
     * @param targetHash the new target hash code (current content)
     * @param itemHash the new content item hash (backup content)
     * @return the rollback modification information
     */
    abstract ContentModification createRollbackEntry(ContentModification original, byte[] targetHash, byte[] itemHash);

    @Override
    public boolean prepare(final PatchingContext context) throws IOException {
        // Backup
        backupHash = backup(context);
        // If the content is already present just resolve any conflict automatically
        final byte[] contentHash = contentItem.getContentHash();
        if(Arrays.equals(backupHash, contentHash)) {
            skipExecute = true; // Skip the execute
            return true;
        }
        // See if the content matches our expected target
        final byte[] expected = description.getModification().getTargetHash();
        if(Arrays.equals(backupHash, expected)) {
            // Don't resolve conflicts from the history
            return ! description.hasConflicts();
        }
        return false;
    }

    @Override
    public void execute(final PatchingContext context) throws IOException {
        final PatchContentLoader contentLoader = description.getLoader();
        final ContentModification original = description.getModification();
        final boolean skip = skipExecute | context.isExcluded(contentItem);
        final byte[] contentHash;
        if(skip) {
            contentHash = backupHash; // Reuse the backup hash
        } else {
            contentHash = apply(context, contentLoader); // Copy the content
        }
        if(! Arrays.equals(contentHash, contentItem.getContentHash())) {
            PatchLogger.ROOT_LOGGER.debugf("wrong hash for content item (%s)", contentItem);
        }
        final ContentModification rollbackAction = createRollbackEntry(original, contentHash, backupHash);
        context.recordRollbackAction(rollbackAction);
    }

}
