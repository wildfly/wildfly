package org.jboss.as.patching.installation;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Emanuel Muckenhuber
 */
public class InstallationManagerImpl implements InstallationManager {

    // private volatile InstalledIdentity installedIdentity;
    private final Identity identity;
    private volatile InstallationModificationImpl.InstallationState state;
    private final AtomicBoolean writable = new AtomicBoolean(true);

    public InstallationManagerImpl(InstalledIdentity installedIdentity) throws IOException {
        this.identity = installedIdentity.getIdentity();
        this.state = load(installedIdentity);
    }

    protected InstallationModificationImpl.InstallationState load(final InstalledIdentity installedIdentity) throws IOException {
        final InstallationModificationImpl.InstallationState state = new InstallationModificationImpl.InstallationState();
        for (final Layer layer : installedIdentity.getLayers()) {
            state.putLayer(layer);
        }
        for (final AddOn addOn : installedIdentity.getAddOns()) {
            state.putAddOn(addOn);
        }
        return state;
    }

    @Override
    public Identity getIdentity() {
        return identity;
    }

    @Override
    public InstallationModification modifyInstallation(final ModificationCompletion callback) {
        if (! writable.compareAndSet(true, false)) {
            // This should be guarded by the OperationContext.lock
            throw new IllegalStateException();
        }
        try {
            // Load the state from the disk
            final PatchableTarget.TargetInfo identityInfo = identity.loadTargetInfo();
            final InstallationModificationImpl.InstallationState state = this.state;

            return new InstallationModificationImpl(identityInfo, identity.getVersion(), state) {
                @Override
                public void complete() {
                    try {

                        final InstallationState newState = internalComplete();
                        // TODO update state
                        callback.completed();
                        InstallationManagerImpl.this.state = newState;
                        writable.set(true);
                    } catch (Exception e) {
                        cancel();
                        throw new RuntimeException(e);
                    }

                }

                @Override
                public void cancel() {
                    try {
                        callback.canceled();
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


}
