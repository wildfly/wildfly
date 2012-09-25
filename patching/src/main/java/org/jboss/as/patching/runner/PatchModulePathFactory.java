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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Emanuel Muckenhuber
 */
class PatchModulePathFactory {

    /**
     * Get the patching module path.
     *
     * @param jbossHome the jboss home directory
     * @param modulePath the original module path, which is getting appended
     * @return the module path
     */
    protected static File[] load(final File jbossHome, final File[] modulePath) throws IOException {
        final DirectoryStructure structure = DirectoryStructure.createDefault(jbossHome);
        if(structure.getCumulativeLink().exists()) {
            final String ref = PatchUtils.readRef(structure.getCumulativeLink());
            final List<String> patches = PatchUtils.readRefs(structure.getCumulativeRefs(ref));
            final List<File> path = new ArrayList<File>(patches.size() + modulePath.length + 1);
            for(final String patch : patches) {
                // one-off patches
                path.add(structure.getPatchDirectory(patch));
            }
            // CP
            path.add(structure.getPatchDirectory(ref));
            // User defined module path
            for(final File file : modulePath) {
                path.add(file);
            }
            return path.toArray(new File[path.size()]);
        } else {
            return modulePath;
        }
    }

}
