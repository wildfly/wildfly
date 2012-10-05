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

package org.jboss.as.patching;

import org.jboss.as.boot.DirectoryStructure;
import org.jboss.as.patching.runner.PatchUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Emanuel Muckenhuber
 */
public class LocalPatchInfo implements PatchInfo {

    private final String version;
    private final String cumulativeId;
    private final List<String> patches;
    private final DirectoryStructure environment;

    public LocalPatchInfo(final String version, final String cumulativeId, final List<String> patches, final DirectoryStructure environment) {
        this.version = version;
        this.cumulativeId = cumulativeId;
        this.patches = patches;
        this.environment = environment;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public String getCumulativeID() {
        return cumulativeId;
    }

    @Override
    public List<String> getPatchIDs() {
        return patches;
    }

    @Override
    public DirectoryStructure getEnvironment() {
        return environment;
    }

    /**
     * Load the information from the disk.
     *
     * @param version the current version
     * @param environment the patch environment
     * @return the patches
     * @throws IOException
     */
    static LocalPatchInfo load(final String version, final DirectoryStructure environment) throws IOException {
        if(environment.getCumulativeLink().exists()) {
            final String ref = PatchUtils.readRef(environment.getCumulativeLink());
            final List<String> patches = PatchUtils.readRefs(environment.getCumulativeRefs(ref));
            return new LocalPatchInfo(version, ref, patches, environment);
        } else {
            return new LocalPatchInfo(version, BASE, Collections.<String>emptyList(), environment);
        }
    }

    /**
     * Load the patch history for a given patch info.
     *
     * @param current the current patch info
     * @param structure the directory structure
     * @return the history
     * @throws IOException
     */
    static PatchInfo loadHistory(final PatchInfo current, final DirectoryStructure structure) throws IOException {
        final String currentId = current.getCumulativeID();
        if(BASE.equals(currentId)) {
            return null;
        }
        final File history = structure.getHistoryDir(currentId);
        final File cumulative = new File(history, DirectoryStructure.CUMULATIVE);
        if(cumulative.exists()) {
            // Return the immediate persisted history
            // this does not necessarily mean it is consistent with the .metadata/references/cumulative-id
            final String ref = PatchUtils.readRef(cumulative);
            final File refsFile = new File(history, DirectoryStructure.REFERENCES);
            final List<String> refs = PatchUtils.readRefs(refsFile);
            // TODO perhaps we can use the backed up patch.xml to get the version?
            return new LocalPatchInfo("unknown", ref, refs, structure);
        }
        return null;
    }

    @Override
    public File[] getPatchingPath() {
        final List<File> path = new ArrayList<File>();
        for(final String patch :patches) {
            path.add(environment.getModulePatchDirectory(patch));
        }
        if(cumulativeId != BASE) {
            path.add(environment.getModulePatchDirectory(cumulativeId));
        }
        return path.toArray(new File[path.size()]);
    }
}