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
import org.jboss.as.patching.IoUtils;
import org.jboss.as.patching.LocalPatchInfo;
import org.jboss.as.patching.PatchInfo;
import org.jboss.as.patching.metadata.Patch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Callback handler when rolling back a patch.
 *
 * @author Emanuel Muckenhuber
 */
public class PatchingRollbackCallback implements PatchingContext.TaskFinishCallback {

    private final Patch patch;
    private final String patchId;
    private final List<String> patches;
    private final String cumulativeTarget;
    private final DirectoryStructure structure;
    private final boolean restoreConfiguration;

    public PatchingRollbackCallback(String patchId, Patch patch, List<String> patches, String cumulativeTarget, boolean restoreConfiguration, DirectoryStructure structure) {
        this.patchId = patchId;
        this.patch = patch;
        this.patches = patches;
        this.cumulativeTarget = cumulativeTarget;
        this.structure = structure;
        this.restoreConfiguration = restoreConfiguration;
    }

    @Override
    public PatchInfo finalizePatch(final Patch rollbackPatch, final PatchingContext context) throws IOException {

        final PatchInfo patchInfo = context.getPatchInfo();
        // Restore the configuration of the original given patch-id
        if(restoreConfiguration) {
            context.restoreConfiguration(patchId);
        }
        // Create the patch info
        if(context.getPatchType() == Patch.PatchType.CUMULATIVE) {
            // Use the cumulative version from the history
            final String resultingVersion = patch.getResultingVersion();
            return new LocalPatchInfo(resultingVersion, cumulativeTarget, Collections.<String>emptyList(), structure);
        } else {
            final List<String> oneOffs = new ArrayList<String>(patchInfo.getPatchIDs());
            for(final String patch : patches) {
                oneOffs.remove(patch);
            }
            // Remove all the patches we rolled back
            final String resultingVersion = patchInfo.getVersion();
            return new LocalPatchInfo(resultingVersion, patchInfo.getCumulativeID(), oneOffs, structure);
        }
    }

    @Override
    public void commitCallback() {
        // delete all resources associated to the rolled back patches
        for(final String rollback : patches) {
            IoUtils.recursiveDelete(structure.getHistoryDir(rollback));
            // Leave the patch dir to for the GC operation
            // recursiveDelete(structure.getPatchDirectory(rollback));
        }
        IoUtils.recursiveDelete(structure.getHistoryDir(patchId));
        // Leave the patch dir to for the GC operation
        // recursiveDelete(structure.getPatchDirectory(patchId));
        if(patch.getPatchType() == Patch.PatchType.CUMULATIVE) {
            // Only remove the refs for rolling back a CP
            IoUtils.recursiveDelete(structure.getCumulativeRefs(patchId));
        }
    }

    @Override
    public void rollbackCallback() {
        // nothing for a failed rollback
    }
}
