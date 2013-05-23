package org.jboss.as.patching.runner;

import static org.jboss.as.patching.IoUtils.recursiveDelete;

import java.util.Collection;

import org.jboss.as.patching.DirectoryStructure;
import org.jboss.as.patching.installation.InstalledImage;
import org.jboss.as.patching.metadata.IdentityPatch;
import org.jboss.as.patching.metadata.Patch;

/**
 * @author Emanuel Muckenhuber
 */
public class IdentityRollbackCallback implements IdentityPatchContext.FinalizeCallback {

    private final String patchId;
    private final Collection<String> patches;
    private final boolean restoreConfiguration;
    private final DirectoryStructure directoryStructure;

    public IdentityRollbackCallback(final String patchId, final Collection<String> patches, boolean restoreConfiguration, final DirectoryStructure directoryStructure) {
        this.patchId = patchId;
        this.patches = patches;
        this.directoryStructure = directoryStructure;
        this.restoreConfiguration = restoreConfiguration;
    }

    @Override
    public String getPatchId() {
        return patchId;
    }

    @Override
    public Patch.PatchType getPatchType() {
        return Patch.PatchType.ONE_OFF; // does not matter
    }

    @Override
    public void finishPatch(final IdentityPatch patch, final IdentityPatchContext context) throws Exception {
        if (restoreConfiguration) {
            context.restoreConfiguration(patchId);
        }
    }

    @Override
    public void commit() {
        final InstalledImage installedImage = directoryStructure.getInstalledImage();
        // delete all resources associated to the rolled back patches
        for (final String rollback : patches) {
            recursiveDelete(installedImage.getPatchHistoryDir(rollback));
            // Leave the patch dir to for the GC operation
            // IoUtils.recursiveDelete(structure.getPatchDirectory(rollback));
        }
        recursiveDelete(installedImage.getPatchHistoryDir(patchId));
    }

    @Override
    public void rollback() {
        // nothing to do here
    }
}
