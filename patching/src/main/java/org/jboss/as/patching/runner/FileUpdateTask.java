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

import static org.jboss.as.patching.IoUtils.NO_CONTENT;

import java.io.File;
import java.util.Arrays;

import org.jboss.as.patching.metadata.ContentModification;
import org.jboss.as.patching.metadata.MiscContentItem;
import org.jboss.as.patching.metadata.ModificationType;

/**
 * Task modifying an existing file.
 *
 * @author Emanuel Muckenhuber
 */
final class FileUpdateTask extends AbstractFileTask {

    FileUpdateTask(PatchingTaskDescription description, File target, File backup) {
        super(description, target, backup);
    }

    @Override
    ContentModification createRollbackEntry(ContentModification original, MiscContentItem item, byte[] targetHash) {
        final ModificationType type;
        if (Arrays.equals(NO_CONTENT, item.getContentHash()) && !backup.exists()) {
            type = ModificationType.REMOVE;
        } else {
            type = ModificationType.MODIFY;
        }
        return new ContentModification(item, targetHash, type);
    }

    protected ContentModification getOriginalModification(byte[] targetHash, byte[] itemHash) {
        final ContentModification original = super.getOriginalModification(targetHash, itemHash);
        final ModificationType type;
        if (Arrays.equals(NO_CONTENT, itemHash) && !backup.exists()) {
            type = ModificationType.ADD;
        } else {
            type = ModificationType.MODIFY;
        }
        return new ContentModification(original.getItem(), original.getTargetHash(), type);
    }
}
