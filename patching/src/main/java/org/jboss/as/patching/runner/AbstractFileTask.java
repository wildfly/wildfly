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

import org.jboss.as.patching.PatchMessages;

import org.jboss.as.patching.metadata.ContentModification;
import org.jboss.as.patching.metadata.MiscContentItem;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Base {@linkplain PatchingTask} for misc file updates.
 *
 * @author Emanuel Muckenhuber
 */
abstract class AbstractFileTask extends AbstractPatchingTask<MiscContentItem> {

    final File target; // the target file
    final File backup; // the backup file

    protected AbstractFileTask(PatchingTaskDescription description, File target, File backup) {
        super(description, MiscContentItem.class);
        this.target = target;
        this.backup = backup;
    }

    /**
     * Create the rollback item.
     *
     * @param original the original content modification
     * @param item the new misc content item
     * @param targetHash the new target hash
     * @return the rollback modification item
     */
    abstract ContentModification createRollbackEntry(ContentModification original, MiscContentItem item, byte[] targetHash);

    @Override
    byte[] backup(PatchingContext context) throws IOException {
        if(target.isFile()) {
            // Backup the original in the history directory
            return PatchUtils.copy(target, backup);
        }
        return NO_CONTENT;
    }

    @Override
    byte[] apply(PatchingContext context, PatchContentLoader loader) throws IOException {
        final MiscContentItem item = contentItem;
        if(item.isDirectory()) {
            if(! target.mkdir() && ! target.isDirectory()) {
                throw PatchMessages.MESSAGES.cannotCreateDirectory(target.getAbsolutePath());
            }
            return NO_CONTENT;
        } else {
            final InputStream is = loader.openContentStream(item);
            try {
                // Replace the file
                return PatchUtils.copy(is, target);
            } finally {
                PatchUtils.safeClose(is);
            }
        }
    }

    @Override
    ContentModification createRollbackEntry(ContentModification original, byte[] targetHash, byte[] itemHash) {
        final MiscContentItem item = new MiscContentItem(contentItem.getName(), contentItem.getPath(), itemHash, contentItem.isDirectory(), contentItem.isAffectsRuntime());
        return createRollbackEntry(original, item, targetHash);
    }

}
