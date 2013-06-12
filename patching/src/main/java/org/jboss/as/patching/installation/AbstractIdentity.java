package org.jboss.as.patching.installation;

import java.io.File;

import org.jboss.as.patching.Constants;

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
    public File getInstallationInfo() {
        return new File(getPatchesMetadata(), Constants.IDENTITY_METADATA);
    }

    @Override
    protected File getPatchesMetadata() {
        return getInstalledImage().getInstallationMetadata();
    }
}
