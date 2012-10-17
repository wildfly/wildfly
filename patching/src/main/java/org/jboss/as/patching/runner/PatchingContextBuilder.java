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

import org.jboss.as.boot.DirectoryStructure;
import org.jboss.as.patching.PatchInfo;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Emanuel Muckenhuber
 */
class PatchingContextBuilder {

    private PatchInfo info;
    private DirectoryStructure structure;

    private final Map<String, PatchContentLoader> loaders = new HashMap<String, PatchContentLoader>();
    private final Map<Location, PatchingContentLoaderEntry> entries = new HashMap<Location, PatchingContentLoaderEntry>();

    void addResourceLoader(final String patchId, boolean rollback, File workDir) {
        final File miscRoot;
        final File modulesRoot;
        final File bundlesRoot;
        if(rollback) {
            final File historyDir = structure.getHistoryDir(patchId);
            miscRoot = new File(historyDir, PatchContentLoader.MISC);
            modulesRoot = structure.getModulePatchDirectory(patchId);
            bundlesRoot = structure.getBundlesPatchDirectory(patchId);
        } else {
            miscRoot = new File(workDir, PatchContentLoader.MISC);
            bundlesRoot = new File(workDir, PatchContentLoader.BUNDLES);
            modulesRoot = new File(workDir, PatchContentLoader.MODULES);
        }
        //
        loaders.put(patchId, new PatchContentLoader(miscRoot, bundlesRoot, modulesRoot));
    }

    PatchContentLoader getContentLoader(final PatchingTasks.ContentTaskDefinition definition) {

        final PatchingTasks.ContentEntry target = definition.getTarget();
        final PatchContentLoader loader = loaders.get(target.getPatchId());
        return loader;

    }


    PatchingContext createContext() {

        return null;
    };

    static class PatchingContentLoaderEntry {

        private final PatchContentLoader loader;
        PatchingContentLoaderEntry(PatchContentLoader loader) {
            this.loader = loader;
        }

        public PatchContentLoader getLoader() {
            return loader;
        }
    }

}
