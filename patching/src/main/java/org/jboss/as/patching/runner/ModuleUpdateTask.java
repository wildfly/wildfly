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

import java.io.File;
import java.io.IOException;

import org.jboss.as.patching.HashUtils;
import org.jboss.as.patching.IoUtils;
import org.jboss.as.patching.metadata.ContentModification;
import org.jboss.as.patching.metadata.ModificationType;
import org.jboss.as.patching.metadata.ModuleItem;

/**
 * Adding or updating a module will add a module in the patch overlay directory {@linkplain org.jboss.as.patching.DirectoryStructure#getModulePatchDirectory(String)}.
 *
 * @author Emanuel Muckenhuber
 */
class ModuleUpdateTask extends AbstractModuleTask {

    ModuleUpdateTask(PatchingTaskDescription description) {
        super(description);
    }

    @Override
    byte[] apply(PatchingTaskContext context, PatchContentLoader loader) throws IOException {
        // Copy the new module resources to the patching directory
        final File targetDir = context.getTargetFile(contentItem);
        final File sourceDir = loader.getFile(contentItem);
        // Recursively copy module contents (incl. native libs)
        IoUtils.copyFile(sourceDir, targetDir);
        // return contentItem.getContentHash();
        return HashUtils.hashFile(targetDir);
    }

    @Override
    ContentModification createRollbackEntry(ContentModification original, byte[] targetHash, byte[] itemHash) {
        // Although modules are ignored for rollback, we still keep track of our changes
        final ModuleItem item = createContentItem(contentItem, itemHash);
        final ModificationType type;
        // Check if the module did not exist before. Invalidated patches might include the module already
        // and we need to track that they can be rolled back to the last state
        if (original.getType() != ModificationType.MODIFY && itemHash.length == 0) {
            type = ModificationType.REMOVE;
        } else {
            type = ModificationType.MODIFY;
        }
        return new ContentModification(item, targetHash, type);
    }

}
