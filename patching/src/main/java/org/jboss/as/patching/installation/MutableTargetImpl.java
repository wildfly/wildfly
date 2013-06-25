package org.jboss.as.patching.installation;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.jboss.as.patching.Constants;
import org.jboss.as.patching.DirectoryStructure;
import org.jboss.as.patching.IoUtils;
import org.jboss.as.patching.metadata.Patch;
import org.jboss.as.patching.runner.PatchUtils;

/**
 * @author Emanuel Muckenhuber
 */
class MutableTargetImpl implements InstallationManager.MutablePatchingTarget {

    private final DirectoryStructure structure;
    private final PatchableTarget.TargetInfo current;
    // The mutable state
    private final List<String> patchIds;
    private final Properties properties;
    private String releaseID;
    private String cumulativeID;
    private boolean modified = false;

    MutableTargetImpl(PatchableTarget.TargetInfo current) {
        this.current = current;
        this.structure = current.getDirectoryStructure();
        this.releaseID = current.getReleasePatchID();
        this.cumulativeID = current.getCumulativeID();
        this.patchIds = new ArrayList<String>(current.getPatchIDs());
        this.properties = new Properties(current.getProperties());
    }

    @Override
    public boolean isApplied(String patchId) {
        if (releaseID.equals(patchId)) {
            return true;
        } else if (cumulativeID.equals(patchId)) {
            return true;
        }
        return patchIds.contains(patchId);
    }

    @Override
    public void rollback(final String patchId) {
        if (!patchIds.remove(patchId)) {
            if (patchId.equals(cumulativeID)) {
                cumulativeID = Constants.NOT_PATCHED;
            } else if (patchId.equals(releaseID)) {
                releaseID = Constants.NOT_PATCHED;
            } else {
                throw new IllegalStateException("cannot rollback not-applied patch " + patchId);
            }
        }
        modified = true;
    }

    @Override
    public void apply(String patchId, Patch.PatchType patchType) {
        if (patchType == Patch.PatchType.UPGRADE) {
            if (!patchIds.isEmpty()) {
                throw new IllegalStateException("cannot apply release patch if there are other patches applied " +patchIds);
            }
            releaseID = patchId;
        } else if (patchType == Patch.PatchType.CUMULATIVE) {
            if (! patchIds.isEmpty()) {
                throw new IllegalStateException("cannot apply cumulative patch if there are other patches applied" +patchIds);
            }
            cumulativeID = patchId;
        } else {
            patchIds.add(0, patchId);
        }
        modified = true;
    }

    @Override
    public String getReleasePatchID() {
        return current.getReleasePatchID();
    }

    @Override
    public List<String> getPatchIDs() {
        return current.getPatchIDs();
    }

    @Override
    public String getCumulativeID() {
        return cumulativeID;
    }

    @Override
    public Properties getProperties() {
        return current.getProperties();
    }

    @Override
    public DirectoryStructure getDirectoryStructure() {
        return structure;
    }

    protected void persist() throws IOException {
        if (modified) {
            // persist the state for bundles and modules directory
            persist(releaseID, cumulativeID, patchIds);
        }
    }

    protected void restore() throws IOException {
        if (modified) {
            // persist the state for bundles and modules directory
            persist(current.getReleasePatchID(), current.getCumulativeID(), current.getPatchIDs());
        }
    }

    private void persist(final String releaseID, final String cumulativeID, final List<String> patches) throws IOException {
        if (releaseID == null || cumulativeID == null) {
            throw new IllegalStateException();
        }
        // Create the parent
        IoUtils.mkdir(structure.getInstallationInfo().getParentFile());

        final List<String> consolidate = new ArrayList<String>();
        consolidate.addAll(patches);
        if (! Constants.BASE.equals(cumulativeID)) {
            consolidate.add(cumulativeID);
        }
        if (!Constants.BASE.equals(releaseID)) {
            consolidate.add(releaseID);
        }

        if (structure.getModuleRoot() != null) {
            final File overlays = new File(structure.getModuleRoot(), Constants.PATCHES);
            final File refs = new File(overlays, Constants.PATCHES);
            PatchUtils.writeRefs(refs, consolidate);
        }

        // Update the properties
        properties.put(Constants.RELEASE_PATCH_ID, releaseID);
        properties.put(Constants.CUMULATIVE, cumulativeID);
        properties.put(Constants.PATCHES, PatchUtils.asString(patches));

        // Write layer.conf
        PatchUtils.writeProperties(structure.getInstallationInfo(), properties);
    }

    @Override
    public PatchableTarget.TargetInfo getModifiedState() {
        if (modified) {
            return new LayerInfo.TargetInfoImpl(properties, releaseID, cumulativeID, patchIds, structure);
        } else {
            return current;
        }
    }

}
