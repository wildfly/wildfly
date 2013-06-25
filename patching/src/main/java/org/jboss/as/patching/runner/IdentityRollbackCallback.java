package org.jboss.as.patching.runner;

import static org.jboss.as.patching.IoUtils.recursiveDelete;

import java.util.Collection;
import java.util.Collections;

import org.jboss.as.patching.DirectoryStructure;
import org.jboss.as.patching.installation.InstalledImage;
import org.jboss.as.patching.metadata.ContentModification;
import org.jboss.as.patching.metadata.Patch;
import org.jboss.as.patching.metadata.PatchElement;
import org.jboss.as.patching.metadata.PatchImpl;
import org.jboss.as.patching.metadata.RollbackPatch;
import org.jboss.as.patching.metadata.impl.IdentityImpl;

/**
 * @author Emanuel Muckenhuber
 */
class IdentityRollbackCallback implements IdentityPatchContext.FinalizeCallback {

    private final Patch patch;
    private final Collection<String> patches;
    private final boolean resetConfiguration;
    private final DirectoryStructure directoryStructure;

    public IdentityRollbackCallback(final String patchId, final Collection<String> patches, boolean resetConfiguration, final DirectoryStructure directoryStructure) {
        this.patches = patches;
        this.directoryStructure = directoryStructure;
        this.resetConfiguration = resetConfiguration;
        // Create an empty patch, we don't do anything with the processedPatch
        final IdentityImpl identity = new IdentityImpl("", "");
        identity.setPatchType(Patch.PatchType.ONE_OFF); // Does not matter
        this.patch = new PatchImpl(patchId, "no description", identity, Collections.<PatchElement>emptyList(), Collections.<ContentModification>emptyList());
    }

    @Override
    public Patch getPatch() {
        return patch;
    }

    @Override
    public void finishPatch(final Patch processedPatch, final RollbackPatch patch, final IdentityPatchContext context) throws Exception {
        if (resetConfiguration) {
            context.restoreConfiguration(patch.getPatchId());
        }
    }

    @Override
    public void completed() {
        final InstalledImage installedImage = directoryStructure.getInstalledImage();
        // delete all resources associated to the rolled back patches
        for (final String rollback : patches) {
            recursiveDelete(installedImage.getPatchHistoryDir(rollback));
            // Leave the patch dir to for the GC operation
            // IoUtils.recursiveDelete(structure.getPatchDirectory(rollback));
        }
        recursiveDelete(installedImage.getPatchHistoryDir(patch.getPatchId()));
    }

    @Override
    public void operationCancelled() {
        // nothing to do here
    }
}
