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

import static org.jboss.as.patching.runner.PatchUtils.copy;

import org.jboss.as.patching.metadata.ContentModification;
import org.jboss.as.patching.metadata.MiscContentItem;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SyncFailedException;
import java.util.Arrays;

/**
 * Base {@linkplain PatchingTask} for misc file updates.
 *
 * @author Emanuel Muckenhuber
 */
abstract class AbstractFileTask implements PatchingTask {

    static final byte[] NO_CONTENT = new byte[0];

    final File target; // the target file
    final File backup; // the backup file
    final MiscContentItem item;
    final ContentModification modification;

    byte[] backupHash = NO_CONTENT;

    protected AbstractFileTask(File target, File backup, MiscContentItem item, ContentModification modification) {
        this.target = target;
        this.backup = backup;
        this.modification = modification;
        this.item = item;
    }

    /**
     * Create the rollback modification item.
     *
     * @param context the patching context
     * @param item the content item
     * @param backupItem the backup content item
     * @param targetHash the target hash for the modification
     * @return the rollback modification item
     */
    protected abstract ContentModification createRollback(PatchingContext context, MiscContentItem item, MiscContentItem backupItem, byte[] targetHash);

    public MiscContentItem getContentItem() {
        return item;
    }

    public boolean prepare(final PatchingContext context) throws IOException {
        if(target.isFile()) {
            // Backup the original in the history directory
            backupHash = PatchUtils.copy(target, backup);
        }
        // See if the hash matches the metadata
        final byte[] expected = modification.getTargetHash();
        return Arrays.equals(expected, backupHash);
    }

    public void execute(final PatchingContext context) throws IOException {
        if(item.isDirectory()) {
            if(! target.mkdir()) {
                throw new IOException();
            }
            final MiscContentItem backupItem = new MiscContentItem(item.getName(), item.getPath(), backupHash, item.isDirectory(), item.isAffectsRuntime());
            final ContentModification rollback = createRollback(context, item, backupItem, NO_CONTENT);
            context.recordRollbackAction(rollback);
        } else {
            final InputStream is = context.getLoader().openContentStream(item);
            try {
                // Replace the file
                final byte[] hash = PatchUtils.copy(is, target);
                final MiscContentItem backupItem = new MiscContentItem(item.getName(), item.getPath(), backupHash, item.isDirectory(), item.isAffectsRuntime());
                final ContentModification rollback = createRollback(context, item, backupItem, hash);
                context.recordRollbackAction(rollback);
            } finally {
                PatchUtils.safeClose(is);
            }
        }
    }

}
