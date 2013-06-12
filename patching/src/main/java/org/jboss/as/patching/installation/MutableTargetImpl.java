package org.jboss.as.patching.installation;

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
    private String cumulative;


    MutableTargetImpl(PatchableTarget.TargetInfo current) {
        this.current = current;
        this.structure = current.getDirectoryStructure();
        this.cumulative = current.getCumulativeID();
        this.patchIds = new ArrayList<String>(current.getPatchIDs());
        this.properties = new Properties(current.getProperties());
    }

    @Override
    public void rollback(final String patchId) {
        if (!patchIds.remove(patchId)) {
            if (!patchId.equals(cumulative)) {
                throw new IllegalStateException("cannot rollback not-applied patch " + patchId);
            }
        }
    }

    @Override
    public void apply(String patchId, Patch.PatchType patchType) {
        if (patchType == Patch.PatchType.CUMULATIVE) {
            if (!patchIds.isEmpty()) {
                throw new IllegalStateException("cannot apply cumulative patch if there are one-off patches applied");
            }
            cumulative = patchId;
        } else {
            patchIds.add(0, patchId);
        }
    }

    @Override
    public String getCumulativeID() {
        return current.getCumulativeID();
    }

    @Override
    public List<String> getPatchIDs() {
        return current.getPatchIDs();
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
        // persist the state for bundles and modules directory
        persist(cumulative, patchIds);
    }

    protected void restore() throws IOException {
        // persist the state for bundles and modules directory
        persist(current.getCumulativeID(), current.getPatchIDs());
    }

    private void persist(final String cumulative, final List<String> patches) throws IOException {
        // Create the parent
        IoUtils.mkdir(structure.getInstallationInfo().getParentFile());

        // Update the properties
        properties.put(Constants.CUMULATIVE, cumulative);
        properties.put(Constants.PATCHES, PatchUtils.asString(patches));

        // Write layer.conf
        PatchUtils.writeProperties(structure.getInstallationInfo(), properties);
    }

    @Override
    public PatchableTarget.TargetInfo getModifiedState() {
        return new PatchableTarget.TargetInfo() {
            @Override
            public String getCumulativeID() {
                return cumulative;
            }

            @Override
            public List<String> getPatchIDs() {
                return patchIds;
            }

            @Override
            public DirectoryStructure getDirectoryStructure() {
                return structure;
            }

            @Override
            public Properties getProperties() {
                return properties;
            }
        };
    }

}
