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
import org.jboss.as.patching.metadata.MiscContentItem;
import org.jboss.as.patching.metadata.ModificationType;

import java.io.File;
import java.io.IOException;

/**
 * A generic patching task.
 *
 * @author Emanuel Muckenhuber
 */
public interface PatchingTask {

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

        static PatchingTask create(final PatchingTaskDescription description, final PatchingContext context) {
            final ContentItem item = description.getContentItem();
            switch (item.getContentType()) {
                case BUNDLE:
                    return createBundleTask(description);
                case MISC:
                    return createMiscTask(description, (MiscContentItem) item, context);
                case MODULE:
                    return createModuleTask(description);
                default:
                    throw new IllegalStateException();
            }
        }

        static PatchingTask createBundleTask(final PatchingTaskDescription description) {
            return new BundlePatchingTask(description);
        }

        static PatchingTask createModuleTask(final PatchingTaskDescription description) {
            final ModificationType type = description.getModificationType();
            if(type == ModificationType.REMOVE) {
                return new ModuleRemoveTask(description);
            } else {
                return new ModuleUpdateTask(description);
            }
        }

        static PatchingTask createMiscTask(final PatchingTaskDescription description, final MiscContentItem item, final PatchingContext context) {
            // Create the task
            final File target = context.getTargetFile(item);
            final File backup = context.getBackupFile(item);
            final ModificationType type = description.getModificationType();
            switch (type) {
                case ADD:
                    return new FileAddTask(description, target, backup);
                case MODIFY:
                    return new FileUpdateTask(description, target, backup);
                case REMOVE:
                    return new FileRemoveTask(description, target, backup);
                default:
                    throw new IllegalStateException();
            }
        }
    }

}
