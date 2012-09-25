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

import org.jboss.as.patching.metadata.ContentItem;
import org.jboss.as.patching.metadata.ContentModification;
import org.jboss.as.patching.metadata.ModificationType;

import java.io.File;
import java.io.IOException;

/**
 * @author Emanuel Muckenhuber
 */
public class FileRemoveTask extends AbstractFileTask {

    public FileRemoveTask(File target, File backup, ContentModification modification) {
        super(target, backup, modification);
    }

    @Override
    public ContentModification execute(PatchingContext context) throws IOException {

        final ContentItem item = modification.getItem();

        // delete the file
        target.delete();

        final ContentItem backupItem = new ContentItem(item.getName(), item.getPath(), backupHash);
        return createRollback(context, item, backupItem, NO_CONTENT);
    }

    @Override
    protected ContentModification createRollback(PatchingContext context, ContentItem item, ContentItem backupItem, byte[] targetHash) {
        return new ContentModification(backupItem, NO_CONTENT, ModificationType.ADD);
    }

}
