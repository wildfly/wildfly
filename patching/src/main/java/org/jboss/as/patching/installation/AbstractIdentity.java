package org.jboss.as.patching.installation;

import java.io.File;

/**
 * @author Emanuel Muckenhuber
 */
abstract class AbstractIdentity extends AbstractPatchableTarget implements Identity {

    @Override
    public final File getBundleRepositoryRoot() {
        return null; // no bundle root associated with the identity
    }

    @Override
    public final File getModuleRoot() {
        return null; // no module root associated with the identity
    }

    @Override
    protected File getPatchesMetadata() {
        return getInstalledImage().getInstallationMetadata();
    }
}
