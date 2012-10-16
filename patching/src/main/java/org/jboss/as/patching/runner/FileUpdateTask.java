/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
import org.jboss.as.patching.metadata.ContentModification;
import org.jboss.as.patching.metadata.MiscContentItem;
import org.jboss.as.patching.metadata.ModificationType;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Task modifying an existing file.
 *
 * @author Emanuel Muckenhuber
 */
final class FileUpdateTask extends AbstractFileTask {

    FileUpdateTask(MiscContentItem item, File target, File backup, ContentModification modification) {
        super(target, backup, item, modification);
    }

    public void execute(final PatchingContext context) throws IOException {

        final InputStream is = context.getLoader().openContentStream(item);
        try {
            // Replace the file
            final byte[] hash = copy(is, target);
            final MiscContentItem backupItem = new MiscContentItem(item.getName(), item.getPath(), backupHash);
            final ContentModification rollback = createRollback(context, item, backupItem, hash);
            context.recordRollbackAction(rollback);
        } finally {
            PatchUtils.safeClose(is);
        }
    }

    protected ContentModification createRollback(PatchingContext context, MiscContentItem item, MiscContentItem backupItem, byte[] targetHash) {
        final byte[] expected = item.getContentHash();
        // TODO Ignored resources
        if(! targetHash.equals(expected)) {
            // TODO rollback if the content hash is different than in the metadata?
            PatchLogger.ROOT_LOGGER.warnf("wrong content has for item (%s) ", item); // TODO i18n
        }
        return new ContentModification(backupItem, targetHash, ModificationType.MODIFY);
    }

}
