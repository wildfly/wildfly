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
import java.io.IOException;

import org.jboss.as.patching.HashUtils;
import org.jboss.as.patching.logging.PatchLogger;
import org.jboss.as.patching.metadata.BundleItem;
import org.jboss.as.patching.metadata.ContentType;
import org.jboss.as.patching.metadata.ModuleItem;

/**
 * Base {@linkplain PatchingTask} for module updates.
 *
 * @author Emanuel Muckenhuber
 */
abstract class AbstractModuleTask extends AbstractPatchingTask<ModuleItem> {

    static final String MODULE_XML = "module.xml";

    AbstractModuleTask(PatchingTaskDescription description) {
        super(description, ModuleItem.class);
    }

    @Override
    byte[] backup(PatchingTaskContext context) throws IOException {
        final File[] repoRoots = context.getTargetModulePath();
        final String moduleName = contentItem.getName();
        final String slot = contentItem.getSlot();
        for(final File path : repoRoots) {
            // Find the first module and calculate the hash
            final File modulePath = PatchContentLoader.getModulePath(path, moduleName, slot);
            final File moduleXml = new File(modulePath, MODULE_XML);
            if(moduleXml.exists()) {
                PatchLogger.ROOT_LOGGER.debugf("found in path (%s)", moduleXml.getAbsolutePath());
                context.invalidateRoot(modulePath);
                return HashUtils.hashFile(modulePath);
            }
        }
        return notFound(contentItem);
    }

    protected byte[] notFound(final ModuleItem contentItem) throws IOException{
        return NO_CONTENT;
    }

    static ModuleItem createContentItem(final ModuleItem original, final byte[] contentHash) {
        final ContentType type = original.getContentType();
        if(type == ContentType.BUNDLE) {
            return new BundleItem(original.getName(), original.getSlot(), contentHash);
        } else {
            return new ModuleItem(original.getName(), original.getSlot(), contentHash);
        }
    }

}
