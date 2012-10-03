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

import org.jboss.as.patching.metadata.BundleItem;
import org.jboss.as.patching.metadata.ContentItem;
import org.jboss.as.patching.metadata.ContentModification;
import org.jboss.as.patching.metadata.MiscContentItem;
import org.jboss.as.patching.metadata.ModificationType;
import org.jboss.as.patching.metadata.ModuleItem;

import java.io.File;
import java.io.IOException;

/**
 * @author Emanuel Muckenhuber
 */
public interface PatchingTask {

    byte[] NO_CONTENT = new byte[0];

    /**
     * Get the content item modified by this task.
     *
     * @return the content item
     */
    ContentItem getContentItem();

    /**
     * Prepare the content modification. This will backup the current target file and check
     * if the file was modified.
     *
     * @param  context the patching context
     * @return whether it meets the modification tasks expectation
     * @throws IOException
     */
    boolean prepare(PatchingContext context) throws IOException;

    /**
     * Execute.
     *
     * @param context the patching context
     * @return the rollback action
     * @throws IOException
     */
    void execute(final PatchingContext context) throws IOException;

    static final class Factory {

        static PatchingTask create(final ContentModification modification, final PatchingContext context) {
            final ContentItem item = modification.getItem();
            switch (item.getContentType()) {
                case BUNDLE:
                    return createBundleTask(modification, (BundleItem)item, context);
                case MISC:
                    return createMiscTask(modification, (MiscContentItem) item, context);
                case MODULE:
                    return createModuleTask(modification, (ModuleItem) item, context);
                default:
                    throw new IllegalStateException();
            }
        }

        static PatchingTask createBundleTask(final ContentModification modification, final BundleItem item, final PatchingContext context) {
            if(modification.getType() == ModificationType.REMOVE) {
                return new ModuleRemoveTask(item, modification.getTargetHash());
            } else {
                return new ModuleUpdateTask(item, modification.getTargetHash());
            }
        }

        static PatchingTask createModuleTask(final ContentModification modification, final ModuleItem item, final PatchingContext context) {
            if(modification.getType() == ModificationType.REMOVE) {
                return new ModuleRemoveTask(item, modification.getTargetHash());
            } else {
                return new ModuleUpdateTask(item, modification.getTargetHash());
            }
        }

        static PatchingTask createMiscTask(final ContentModification modification, final MiscContentItem item, final PatchingContext context) {
            // Create the task
            final File target = context.getTargetFile(item);
            final File backup = context.getBackupFile(item);
            final ModificationType type = modification.getType();
            switch (type) {
                case ADD:
                    return new FileAddTask(item, target, backup, modification);
                case MODIFY:
                    return new FileModifyTask(item, target, backup, modification);
                case REMOVE:
                    return new FileRemoveTask(item, target, backup, modification);
                default:
                    throw new IllegalStateException();
            }
        }
    }

}
