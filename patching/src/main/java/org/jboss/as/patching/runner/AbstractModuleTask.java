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

import org.jboss.as.patching.PatchInfo;
import org.jboss.as.patching.PatchLogger;
import org.jboss.as.patching.metadata.ModuleItem;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/**
 * Base {@linkplain PatchingTask} for module updates.
 *
 * @author Emanuel Muckenhuber
 */
abstract class AbstractModuleTask implements PatchingTask {

    static final String MODULE_XML = "module.xml";

    protected final ModuleItem item;
    protected final byte[] expected;

    protected AbstractModuleTask(ModuleItem item, byte[] expected) {
        this.item = item;
        this.expected = expected;
    }

    @Override
    public ModuleItem getContentItem() {
        return item;
    }

    @Override
    public boolean prepare(PatchingContext context) throws IOException {
        // Check the module.xml hash
        final PatchInfo patchInfo = context.getPatchInfo();
        final File[] repoRoots = patchInfo.getPatchingPath();
        //
        final String moduleName = item.getName();
        final String slot = item.getSlot();
        for(final File path : repoRoots) {

            final File modulePath = PatchContentLoader.getModulePath(path, moduleName, slot);
            final File moduleXml = new File(modulePath, MODULE_XML);
            if(moduleXml.exists()) {
                // We only care about the first match
                // Calculate the hash over the complete directory
                final byte[] hash = PatchUtils.calculateHash(modulePath);
                if(Arrays.equals(expected, hash)) {
                    // Only log
                    PatchLogger.ROOT_LOGGER.moduleContentChanged(moduleName);
                }
            }
        }
        return true;
    }

}
