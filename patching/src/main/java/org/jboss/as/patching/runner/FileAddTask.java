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

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.jboss.as.patching.PatchLogger;
import org.jboss.as.patching.metadata.ContentModification;
import org.jboss.as.patching.metadata.MiscContentItem;
import org.jboss.as.patching.metadata.ModificationType;

/**
 * Task adding new a file.
 *
 * @author Emanuel Muckenhuber
 */
class FileAddTask extends AbstractFileTask {

    FileAddTask(PatchingTaskDescription description, File target, File backup) {
        super(description, target, backup);
    }

    @Override
    ContentModification createRollbackEntry(ContentModification original, MiscContentItem item, byte[] targetHash) {
        return new ContentModification(item, targetHash, ModificationType.REMOVE);
    }

    @Override
    byte[] backup(PatchingTaskContext context) throws IOException {
        final byte[] backupHash = super.backup(context);
        if(! Arrays.equals(backupHash, NO_CONTENT)) {
            PatchLogger.ROOT_LOGGER.debugf("item should not exist (%s)", contentItem);
        }
        return backupHash;
    }

}
