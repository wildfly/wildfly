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
import org.jboss.as.patching.LocalPatchInfo;
import org.jboss.as.patching.PatchInfo;
import org.jboss.as.patching.PatchLogger;
import org.jboss.as.patching.api.Patch;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Emanuel Muckenhuber
 */
class PatchingContext {

    private final PatchInfo info;
    private final PatchContentLoader loader;
    private final DirectoryStructure structure;
    private final List<PatchingRecord> records = new ArrayList<PatchingRecord>();
    private final List<CleanupTask> tasks = new ArrayList<CleanupTask>();

    private boolean rollbackOnly;

    PatchingContext(final PatchInfo info, final DirectoryStructure structure, final PatchContentLoader loader) {
        this.info = info;
        this.structure = structure;
        this.loader = loader;
    }

    public PatchContentLoader getLoader() {
        return loader;
    }

    public DirectoryStructure getStructure() {
        return structure;
    }

    File newFile(final File parent, final String name) {
        final File f = new File(parent, name);
        cleanup(f);
        return f;
    }

    void cleanup(File file) {
        add(new CleanupTask.FileCleanupTask(file));
    }

    void add(final CleanupTask task) {
        tasks.add(task);
    }

    void cleanup() {
        for(final CleanupTask task : tasks) {
            try {
                task.cleanup();
            } catch (Exception e) {
                PatchLogger.ROOT_LOGGER.debugf(e, "exception when trying to cleanup up (%s)", task);
            }
        }
    }

    void record(PatchingRecord record) {
        records.add(record);
    }

    PatchInfo finish(Patch patch) throws PatchingException {
        assert ! rollbackOnly;
        // Create the new info
        final String patchId = patch.getPatchId();
        final PatchInfo newInfo;
        if(Patch.PatchType.ONE_OFF == patch.getPatchType()) {
            final List<String> patches = new ArrayList<String>(info.getPatchIDs());
            patches.add(0, patchId);
            newInfo = new LocalPatchInfo("undefined", info.getCumulativeID(), patches, info.getEnvironment());
        } else {
            newInfo = new LocalPatchInfo("undefined", patchId, Collections.<String>emptyList(), info.getEnvironment());
        }
        try {
            return persist(newInfo);
        } catch (Exception e) {
            throw new PatchingException(e);
        }
    }

    void rollbackOnly() {
        rollbackOnly = true;
    }

    PatchInfo undo(final Patch patch) {
        for(final PatchingRecord record : records) {
            try {
                record.undo(patch, this);
            } catch (Exception e) {
                // TODO i18n
                PatchLogger.ROOT_LOGGER.warnf(e, "exception when trying rolling back (%s)", patch);
            }
        }
        return info;
    }

    /**
     * Persist the changes.
     *
     * @param patch the patch
     * @return the new patch info
     */
    PatchInfo persist(PatchInfo patch) throws IOException {
        // TODO persist records...

        final DirectoryStructure environment = info.getEnvironment();
        final String cumulativeID = info.getCumulativeID();
        PatchUtils.writeRef(environment.getCumulativeLink(), info.getCumulativeID());
        PatchUtils.writeRefs(environment.getCumulativeRefs(cumulativeID), info.getPatchIDs());
        return patch;
    }

}
