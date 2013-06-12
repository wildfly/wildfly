package org.jboss.as.patching.runner;

import static org.jboss.as.patching.runner.PatchUtils.generateTimestamp;

import java.io.File;

import org.jboss.as.patching.Constants;
import org.jboss.as.patching.DirectoryStructure;
import org.jboss.as.patching.IoUtils;
import org.jboss.as.patching.installation.InstalledImage;
import org.jboss.as.patching.metadata.Patch;
import org.jboss.as.patching.metadata.PatchXml;

/**
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
    public String getPatchId() {
        return patchId;
    }

    @Override
    public Patch.PatchType getPatchType() {
        return original.getPatchType();
    }

    @Override
    public void finishPatch(final Patch rollbackPatch, final IdentityPatchContext context) throws Exception {
        final File historyDir = structure.getInstalledImage().getPatchHistoryDir(patchId);

        if (!historyDir.exists()) {
            historyDir.mkdirs();
        }

        // Backup the current active patch Info
        final File installationInfo = new File(historyDir, Constants.INSTALLATION_METADATA);
        final File timestamp = new File(historyDir, Constants.TIMESTAMP);

        // Backup identity information
        final IdentityPatchContext.PatchEntry identity = context.getIdentityEntry();
        PatchUtils.writeProperties(installationInfo, identity.getProperties());
        PatchUtils.writeRef(timestamp, generateTimestamp());

        // Persist the patch.xml in the patch directory
        final File backupPatchXml = new File(historyDir, PatchXml.PATCH_XML);
        IdentityPatchContext.writePatch(original, backupPatchXml);
        // Persist the rollback.xml in the history directory
        final File rollbackPatchXml = new File(historyDir, Constants.ROLLBACK_XML);
        IdentityPatchContext.writePatch(rollbackPatch, rollbackPatchXml);

        // Backup the configuration
        context.backupConfiguration();
    }

    @Override
    public void commit() {
        // nothing to do
    }

    @Override
    public void rollback() {
        // Cleanup history, bundles and module patch directories
        final InstalledImage image = structure.getInstalledImage();
        IoUtils.recursiveDelete(image.getPatchHistoryDir(patchId));
        IoUtils.recursiveDelete(structure.getBundlesPatchDirectory(patchId));
        IoUtils.recursiveDelete(structure.getModulePatchDirectory(patchId));
    }

}
