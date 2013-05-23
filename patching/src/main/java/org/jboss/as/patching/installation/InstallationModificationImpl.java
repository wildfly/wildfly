package org.jboss.as.patching.installation;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.as.patching.PatchLogger;
import org.jboss.as.patching.metadata.IdentityPatch;

/**
 * @author Emanuel Muckenhuber
 */
abstract class InstallationModificationImpl extends MutableTargetImpl implements InstallationManager.InstallationModification {

    private final String version;
    private final InstallationState installationState;
    private final AtomicBoolean done = new AtomicBoolean();

    protected InstallationModificationImpl(final PatchableTarget.TargetInfo identity,
                                           final String version, final InstallationState installationState) {
        super(identity);
        this.version = version;
        this.installationState = installationState;
    }

    @Override
    public InstallationManager.MutablePatchingTarget resolve(String name, IdentityPatch.LayerType type) {
        if (type == IdentityPatch.LayerType.Layer) {
            return installationState.layers.get(name);
        } else {
            return installationState.addOns.get(name);
        }
    }

    @Override
    public String getVersion() {
        return version;
    }

    InstallationState internalCommit() throws Exception {
        try {
            installationState.persist();
        } catch (Exception e) {
            installationState.restore();
            throw e;
        }
        try {
            super.persist();
        } catch (Exception e) {
            installationState.restore();
        }
        return installationState;
    }

    static class InstallationState {

        private final Map<String, MutableTargetImpl> layers = new LinkedHashMap<String, MutableTargetImpl>();
        private final Map<String, MutableTargetImpl> addOns = new LinkedHashMap<String, MutableTargetImpl>();

        protected void putLayer(final Layer layer) throws IOException {
            putPatchableTarget(layer.getName(), layer, layers);
        }

        protected void putAddOn(final AddOn addOn) throws IOException {
            putPatchableTarget(addOn.getName(), addOn, addOns);
        }

        protected void putPatchableTarget(final String name, final PatchableTarget target, Map<String, MutableTargetImpl> map) throws IOException {
            final PatchableTarget.TargetInfo info = target.loadTargetInfo();
            map.put(name, new MutableTargetImpl(info));
        }

        Map<String, MutableTargetImpl> getLayers() {
            return layers;
        }

        Map<String, MutableTargetImpl> getAddOns() {
            return addOns;
        }

        protected void persist() throws IOException {
            for (final MutableTargetImpl target : layers.values()) {
                target.persist();
            }
            for (final MutableTargetImpl target : addOns.values()) {
                target.persist();
            }
        }

        private void restore() {
            for (final MutableTargetImpl target : layers.values()) {
                try {
                    target.restore();
                } catch (IOException e) {
                    PatchLogger.ROOT_LOGGER.debugf(e, "failed to restore original state for layer %s", target);
                }
            }
            for (final MutableTargetImpl target : addOns.values()) {
                try {
                    target.restore();
                } catch (IOException e) {
                    PatchLogger.ROOT_LOGGER.debugf(e, "failed to restore original state for add-on %s", target);
                }
            }
        }

    }


}
