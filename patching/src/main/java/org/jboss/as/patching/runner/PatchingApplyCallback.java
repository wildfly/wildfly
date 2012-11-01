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
import org.jboss.as.patching.Constants;
import org.jboss.as.patching.IoUtils;
import org.jboss.as.patching.LocalPatchInfo;
import org.jboss.as.patching.PatchInfo;
import org.jboss.as.patching.metadata.Patch;
import org.jboss.as.patching.metadata.PatchXml;
import static org.jboss.as.patching.runner.PatchUtils.generateTimestamp;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Callback for finishing a forward patch.
 *
 * @author Emanuel Muckenhuber
 */
public class PatchingApplyCallback implements PatchingContext.TaskFinishCallback {

    private final Patch patch;
    private final String patchId;
    private final DirectoryStructure structure;

    public PatchingApplyCallback(Patch patch, String patchId, DirectoryStructure structure) {
        this.patch = patch;
        this.patchId = patchId;
        this.structure = structure;
    }

    /**
     * Finalize the patch instructions.
     *
     * @param rollbackPatch the rollback instructions
     * @param context the patching context
     * @throws IOException for any error
     */
    public PatchInfo finalizePatch(final Patch rollbackPatch, final PatchingContext context) throws IOException {

        final PatchInfo patchInfo = context.getPatchInfo();
        final File historyDir = structure.getHistoryDir(patchId);
        final File patchDir = structure.getPatchDirectory(patchId);

        // Backup the current active patch Info
        final File cumulativeBackup = new File(historyDir, DirectoryStructure.CUMULATIVE);
        final File referencesBackup = new File(historyDir, DirectoryStructure.REFERENCES);
        final File timestamp = new File(historyDir, Constants.TIMESTAMP);

        PatchUtils.writeRef(cumulativeBackup, patchInfo.getCumulativeID());
        PatchUtils.writeRefs(referencesBackup, patchInfo.getPatchIDs());
        PatchUtils.writeRef(timestamp, generateTimestamp());

        // Persist the patch.xml in the patch directory
        final File backupPatchXml = new File(patchDir, PatchXml.PATCH_XML);
        PatchingContext.writePatch(patch, backupPatchXml);
        // Persist the rollback.xml in the history directory
        final File rollbackPatchXml = new File(historyDir, PatchingContext.ROLLBACK_XML);
        PatchingContext.writePatch(rollbackPatch, rollbackPatchXml);

        // Backup the configuration
        context.backupConfiguration();

        // Create the new patchInfo
        if(context.getPatchType() == Patch.PatchType.ONE_OFF) {
            final List<String> patches = new ArrayList<String>(patchInfo.getPatchIDs());
            patches.add(0, patchId);
            final String resultingVersion = patchInfo.getVersion();
            return new LocalPatchInfo(resultingVersion, patchInfo.getCumulativeID(), patches, structure);
        } else {
            final String resultingVersion = patch.getResultingVersion();
            return new LocalPatchInfo(resultingVersion, patchId, Collections.<String>emptyList(), structure);
        }
    }

    @Override
    public void commitCallback() {
        // Nothing when everything went fine
    }

    @Override
    public void rollbackCallback() {
        // In case we rollback delete the patch directory and history
        // TODO this might remove existing information
        IoUtils.recursiveDelete(structure.getPatchDirectory(patchId));
        IoUtils.recursiveDelete(structure.getHistoryDir(patchId));
    }

}
