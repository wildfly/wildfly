package org.jboss.as.patching.installation;

import java.io.IOException;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.as.patching.DirectoryStructure;

/**
 * The installation manager.
 *
 * @author Emanuel Muckenhuber
 */
public class InstallationManagerImpl extends InstallationManager {

    // The basic concurrency contract is copy on write
    private volatile InstalledIdentity installedIdentity;
    // TODO track this state a better way
    private final AtomicBoolean writable = new AtomicBoolean(true);
    private final InstalledImage installedImage;

    /**
     * This field is set to true when a patch is applied/rolled back at runtime.
     * It prevents another patch to be applied and overrides the modifications brought by the previous one
     * unless the process is restarted first
     *
     * This field has to be {@code static} in order to survive server reloads.
     */
    private static final AtomicBoolean restartRequired = new AtomicBoolean(false);

    public InstallationManagerImpl(InstalledIdentity installedIdentity) {
        this.installedIdentity = installedIdentity;
        this.installedImage = installedIdentity.getInstalledImage();
    }

    @Override
    public List<String> getAllInstalledPatches() {
        return installedIdentity.getAllInstalledPatches();
    }

    @Override
    public Identity getIdentity() {
        return installedIdentity.getIdentity();
    }

    @Override
    public List<String> getLayerNames() {
        return installedIdentity.getLayerNames();
    }

    @Override
    public Layer getLayer(String layerName) {
        return installedIdentity.getLayer(layerName);
    }

    @Override
    public List<Layer> getLayers() {
        return installedIdentity.getLayers();
    }

    @Override
    public Collection<String> getAddOnNames() {
        return installedIdentity.getAddOnNames();
    }

    @Override
    public AddOn getAddOn(String addOnName) {
        return installedIdentity.getAddOn(addOnName);
    }

    @Override
    public Collection<AddOn> getAddOns() {
        return installedIdentity.getAddOns();
    }

    @Override
    public InstalledImage getInstalledImage() {
        return installedImage;
    }

    @Override
    public InstallationModification modifyInstallation(final ModificationCompletionCallback callback) {
        if (! writable.compareAndSet(true, false)) {
            // This has to be guarded by the OperationContext.lock
            throw new ConcurrentModificationException();
        }
        try {
            // Load the state
            final InstalledIdentity installedIdentity = this.installedIdentity;
            final Identity identity = installedIdentity.getIdentity();
            final PatchableTarget.TargetInfo identityInfo = identity.loadTargetInfo();
            final InstallationModificationImpl.InstallationState state = load(installedIdentity);

            return new InstallationModificationImpl(identityInfo, identity.getName(), identity.getVersion(), installedIdentity.getAllInstalledPatches(), state) {

                @Override
                public InstalledIdentity getUnmodifiedInstallationState() {
                    return installedIdentity;
                }

                @Override
                public void complete() {
                    try {
                        // Update the state
                        InstallationManagerImpl.this.installedIdentity = updateState(identity.getName(), this, internalComplete());
                        writable.set(true);
                    } catch (Exception e) {
                        cancel();
                        throw new RuntimeException(e);
                    }
                    if (callback != null) {
                        callback.completed();
                    }
                }

                @Override
                public void cancel() {
                    try {
                        if (callback != null) {
                            callback.canceled();
                        }
                    } finally {
                        writable.set(true);
                    }
                }
            };
        } catch (Exception e) {
            writable.set(true);
            throw new RuntimeException(e);
        }
    }

    /**
     * Load the installation state based on the identity
     *
     * @param installedIdentity the installed identity
     * @return the installation state
     * @throws IOException
     */
    protected static InstallationModificationImpl.InstallationState load(final InstalledIdentity installedIdentity) throws IOException {
        final InstallationModificationImpl.InstallationState state = new InstallationModificationImpl.InstallationState();
        for (final Layer layer : installedIdentity.getLayers()) {
            state.putLayer(layer);
        }
        for (final AddOn addOn : installedIdentity.getAddOns()) {
            state.putAddOn(addOn);
        }
        return state;
    }

    /**
     * Update the installed identity using the modified state from the modification.
     *
     * @param name the identity name
     * @param modification the modification
     * @param state the installation state
     * @return the installed identity
     */
    protected InstalledIdentity updateState(final String name, final InstallationModificationImpl modification, final InstallationModificationImpl.InstallationState state) {
        final PatchableTarget.TargetInfo identityInfo = modification.getModifiedState();
        final Identity identity = new Identity() {
            @Override
            public String getVersion() {
                return modification.getVersion();
            }

            @Override
            public String getName() {
                return name;
            }

            @Override
            public TargetInfo loadTargetInfo() throws IOException {
                return identityInfo;
            }

            @Override
            public DirectoryStructure getDirectoryStructure() {
                return modification.getDirectoryStructure();
            }
        };

        final InstalledIdentityImpl installedIdentity = new InstalledIdentityImpl(identity, modification.getAllPatches(), installedImage);
        for (final Map.Entry<String, MutableTargetImpl> entry : state.getLayers().entrySet()) {
            final String layerName = entry.getKey();
            final MutableTargetImpl target = entry.getValue();
            installedIdentity.putLayer(layerName, new LayerInfo(layerName, target.getModifiedState(), target.getDirectoryStructure()));
        }
        for (final Map.Entry<String, MutableTargetImpl> entry : state.getAddOns().entrySet()) {
            final String addOnName = entry.getKey();
            final MutableTargetImpl target = entry.getValue();
            installedIdentity.putAddOn(addOnName, new LayerInfo(addOnName, target.getModifiedState(), target.getDirectoryStructure()));
        }
        return installedIdentity;
    }


    public boolean requiresRestart() {
        return restartRequired.get();
    }

    public boolean restartRequired() {
        return restartRequired.compareAndSet(false, true);
    }

    public void clearRestartRequired() {
        restartRequired.set(false);
    }
}
