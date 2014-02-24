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

import static org.jboss.as.patching.IoUtils.NO_CONTENT;
import static org.jboss.as.patching.IoUtils.copy;
import static org.jboss.as.patching.IoUtils.safeClose;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.jboss.as.patching.IoUtils;
import org.jboss.as.patching.PatchMessages;
import org.jboss.as.patching.metadata.ContentModification;
import org.jboss.as.patching.metadata.MiscContentItem;

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
    byte[] backup(PatchingTaskContext context) throws IOException {
        if(target.isFile()) {
            // Backup the original in the history directory
            final byte[] backupHash = IoUtils.copy(target, backup);
            return backupHash;
        } else if (contentItem.isDirectory() && target.isDirectory()) {
            // Completely ignore the apply step if the directory already exists
            // This will basically skip the creation of the rollback item
            setIgnoreApply();
        }
        return NO_CONTENT;
    }

    @Override
    byte[] apply(PatchingTaskContext context, PatchContentLoader loader) throws IOException {
        final MiscContentItem item = contentItem;
        if(item.isDirectory()) {
            if(! target.mkdirs() && ! target.isDirectory()) {
                throw PatchMessages.MESSAGES.cannotCreateDirectory(target.getAbsolutePath());
            }
            return NO_CONTENT;
        } else {
            if (!target.exists()) {
                createParentDirectories(target, item.getPath(), item.getPath().length, context);
            }
            final InputStream is = loader.openContentStream(item);
            try {
                // Replace the file
                return copy(is, target);
            } finally {
                safeClose(is);
            }
        }
    }

    @Override
    ContentModification createRollbackEntry(ContentModification original, byte[] targetHash, byte[] itemHash) {
        final MiscContentItem item = new MiscContentItem(contentItem.getName(), contentItem.getPath(), itemHash, contentItem.isDirectory(), contentItem.isAffectsRuntime());
        return createRollbackEntry(original, item, targetHash);
    }

    static void createParentDirectories(final File target, String[] path, int depth, final PatchingTaskContext context) throws IOException {
        if (depth > 0) {
            final File parent = target.getParentFile();
            if (! parent.exists()) {
                createParentDirectories(parent, path, depth - 1, context);
            }
            if(! parent.mkdir() && ! parent.isDirectory()) {
                throw PatchMessages.MESSAGES.cannotCreateDirectory(target.getAbsolutePath());
            }
            // TODO record changes
//            final String[] newPath = Arrays.copyOf(path, depth - 1);
//            final MiscContentItem item = new MiscContentItem(parent.getName(), newPath, NO_CONTENT);
//            context.recordChange(new ContentModification(item, NO_CONTENT, ModificationType.ADD), new ContentModification(item, NO_CONTENT, ModificationType.REMOVE));
        }
    }

}
