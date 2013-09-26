package org.jboss.as.patching.installation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.as.patching.Constants;
import org.jboss.as.patching.PatchLogger;
import org.jboss.as.patching.PatchMessages;
import org.jboss.as.patching.PatchingException;
import org.jboss.as.patching.metadata.LayerType;
import org.jboss.as.patching.runner.PatchUtils;

/**
 * @author Emanuel Muckenhuber
 */
abstract class InstallationModificationImpl extends MutableTargetImpl implements InstallationManager.InstallationModification {

    private final String name;
    private final String version;
    private final InstallationState installationState;
    private final List<String> allPatches;
    private final AtomicBoolean done = new AtomicBoolean();

    protected InstallationModificationImpl(final PatchableTarget.TargetInfo identity, final String name,
                                           final String version, final List<String> allPatches,
                                           final InstallationState installationState) {
        super(identity);
        this.name = name;
        this.version = version;
        this.installationState = installationState;
        this.allPatches = new ArrayList<String>(allPatches);
    }

    @Override
    public InstallationManager.MutablePatchingTarget resolve(String name, LayerType type) {
        if (type == LayerType.Layer) {
            return installationState.layers.get(name);
        } else {
            return installationState.addOns.get(name);
        }
    }

    @Override
    public void addInstalledPatch(String patchId) throws PatchingException {
        if (allPatches.contains(patchId)) {
            throw PatchMessages.MESSAGES.alreadyApplied(patchId);
        }
        allPatches.add(patchId);
    }

    @Override
    public void removeInstalledPatch(String patchId) throws PatchingException {
        if (! allPatches.contains(patchId)) {
            throw PatchMessages.MESSAGES.cannotRollbackPatch(patchId);
        }
        allPatches.remove(patchId);
    }

    List<String> getAllPatches() {
        return allPatches;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    protected void persist() throws IOException {
        getMutableProperties().put(Constants.ALL_PATCHES, PatchUtils.asString(allPatches));
        super.persist();
    }

    boolean setDone() {
        return done.compareAndSet(false, true);
    }

    InstallationState internalComplete() throws Exception {
        if (!setDone()) {
            throw new IllegalStateException();
        }
        try {
            installationState.persist();
        } catch (Exception e) {
            installationState.restore();
            throw e;
        }
        try {
            persist();
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
