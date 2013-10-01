package org.jboss.as.patching.runner;

import static org.jboss.as.patching.IoUtils.recursiveDelete;

import java.io.File;
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
 * Callback when rolling back a patch. This will cleanup the patch directories and history if completed successfully.
 *
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
        identity.setPatchType(Patch.PatchType.ONE_OFF); // Does not matter, we delete the temp directory anyway
        this.patch = new PatchImpl(patchId, "no description", identity, Collections.<PatchElement>emptyList(), Collections.<ContentModification>emptyList());
    }

    @Override
    public Patch getPatch() {
        return patch;
    }

    @Override
    public void finishPatch(final Patch processedPatch, final RollbackPatch patch, final IdentityPatchContext context) throws Exception {
        context.restoreConfiguration(patch.getPatchId(), resetConfiguration);
    }

    @Override
    public void completed(IdentityPatchContext context) {
        final InstalledImage installedImage = directoryStructure.getInstalledImage();
        final File history = installedImage.getPatchHistoryDir(patch.getPatchId());
        if (!recursiveDelete(history)) {
            context.failedToCleanupDir(history);
        }
        // Cleanup all the recorded rollbacks
        cleanupEntry(context.getLayers());
        cleanupEntry(context.getAddOns());
        cleanupEntry(Collections.singleton(context.getIdentityEntry()));
    }

    @Override
    public void operationCancelled(IdentityPatchContext context) {
        // nothing to do here
    }

    static void cleanupEntry(final Collection<IdentityPatchContext.PatchEntry> entries) {
        for (final IdentityPatchContext.PatchEntry entry : entries) {
            entry.cleanupRollbackPatchHistory();
        }
    }

}
