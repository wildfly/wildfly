package org.jboss.as.patching.runner;

import static org.jboss.as.patching.runner.PatchUtils.generateTimestamp;

import java.io.File;

import org.jboss.as.patching.Constants;
import org.jboss.as.patching.DirectoryStructure;
import org.jboss.as.patching.IoUtils;
import org.jboss.as.patching.installation.InstalledImage;
import org.jboss.as.patching.metadata.Patch;
import org.jboss.as.patching.metadata.PatchElement;
import org.jboss.as.patching.metadata.PatchXml;
import org.jboss.as.patching.metadata.RollbackPatch;

/**
 * Callback when applying a patch. This will write the patch history and in case of a failure trigger a cleanup
 * of the processed patch.
 *
 * @author Emanuel Muckenhuber
 */
class IdentityApplyCallback implements IdentityPatchContext.FinalizeCallback {

    private final String patchId;
    private final Patch original;
    private final DirectoryStructure structure;

    public IdentityApplyCallback(final Patch original, final DirectoryStructure structure) {
        this.patchId = original.getPatchId();
        this.original = original;
        this.structure = structure;
    }

    @Override
    public Patch getPatch() {
        return original;
    }

    @Override
    public void finishPatch(final Patch processedPatch, final RollbackPatch rollbackPatch, final IdentityPatchContext context) throws Exception {

        final File historyDir = structure.getInstalledImage().getPatchHistoryDir(patchId);
        if (!historyDir.exists()) {
            historyDir.mkdirs();
        }

        // Backup the current active patch Info
        final File timestamp = new File(historyDir, Constants.TIMESTAMP);
        PatchUtils.writeRef(timestamp, generateTimestamp());

        // Persist the processed patch, which contains the records of changes
        final File backupPatchXml = new File(historyDir, PatchXml.PATCH_XML);
        IdentityPatchContext.writePatch(processedPatch, backupPatchXml);

        // Persist the rollback.xml in the history directory
        final File rollbackPatchXml = new File(historyDir, Constants.ROLLBACK_XML);
        IdentityPatchContext.writePatch(rollbackPatch, rollbackPatchXml);

        // Backup the configuration
        context.backupConfiguration();
    }

    @Override
    public void completed(IdentityPatchContext context) {
        // nothing to do
    }

    @Override
    public void operationCancelled(IdentityPatchContext context) {
        // Cleanup history, bundles and module patch directories
        final InstalledImage image = structure.getInstalledImage();
        IoUtils.recursiveDelete(image.getPatchHistoryDir(patchId));
        IoUtils.recursiveDelete(structure.getBundlesPatchDirectory(patchId));
        IoUtils.recursiveDelete(structure.getModulePatchDirectory(patchId));
        for (final PatchElement element : original.getElements()) {
            boolean addOn = element.getProvider().isAddOn();
            final IdentityPatchContext.PatchEntry entry = context.getEntry(element.getProvider().getName(), addOn);
            final DirectoryStructure structure = entry.getDirectoryStructure();
            IoUtils.recursiveDelete(structure.getBundlesPatchDirectory(element.getId()));
            IoUtils.recursiveDelete(structure.getModulePatchDirectory(element.getId()));
        }

    }

}
