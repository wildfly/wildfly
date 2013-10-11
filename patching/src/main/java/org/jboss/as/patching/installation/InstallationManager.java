package org.jboss.as.patching.installation;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.jboss.as.patching.PatchingException;
import org.jboss.as.patching.metadata.LayerType;
import org.jboss.as.patching.metadata.Patch;
import org.jboss.as.version.ProductConfig;

/**
 * The installation manager, basically represents a mutable {@code InstalledIdentity}.
 *
 * @author Emanuel Muckenhuber
 */
public abstract class InstallationManager extends InstalledIdentity {

    /**
     * Modify the installation.
     *
     * @param callback a completed callback
     * @return the modification
     */
    public abstract InstallationModification modifyInstallation(ModificationCompletionCallback callback);

    /**
     * Check whether the instance requires a restart.
     *
     * @return {@code true} if a restart is required, {@code false} otherwise
     */
    public abstract boolean requiresRestart();

    /**
     * Require a restart. This will set the patching service to read-only
     * and the server has to be restarted in order to execute the next
     * patch operation.
     * <p/>
     * In case the patch operation does not succeed it needs to clear the
     * reload required state using {@link #clearRestartRequired()}.
     *
     * @return this will return {@code true}
     */
    public abstract boolean restartRequired();

    /**
     * Clear the the restart required state.
     */
    public abstract void clearRestartRequired();

    public interface InstallationModification extends MutablePatchingTarget {

        /**
         * Get the identity name.
         *
         * @return the identity name
         */
        String getName();

        /**
         * Get the current version of the identity.
         *
         * @return the identity version
         */
        String getVersion();

        /**
         * Set the resulting version.
         *
         * @param version the resulting version
         */
        void setResultingVersion(String version);

        /**
         * Add a patch to the installed list.
         *
         * @param patchId the patch id
         */
        void addInstalledPatch(String patchId) throws PatchingException;

        /**
         * Remove a patch from the installed list.
         *
         * @param patchId the patch id
         * @throws PatchingException
         */
        void removeInstalledPatch(String patchId) throws PatchingException;

        /**
         * Get the unmodified state.
         *
         * @return the originals tate
         */
        InstalledIdentity getUnmodifiedInstallationState();

        /**
         * Resolve a target for patching.
         *
         * @param name the layer name
         * @param type the layer type
         * @return the patching target
         */
        MutablePatchingTarget resolve(String name, LayerType type);

        /**
         * Complete the modifications.
         */
        void complete();

        /**
         * Cancel the modifications. The installation is unchanged.
         */
        void cancel();

    }

    public interface MutablePatchingTarget extends PatchableTarget.TargetInfo {

        /**
         * Rollback an applied patch.
         *
         * @param patchId the patch id to rollback
         */
        void rollback(String patchId);

        /**
         * Apply a patch.
         *
         * @param patchId   the patch id
         * @param patchType the patch type
         */
        void apply(String patchId, Patch.PatchType patchType);

        /**
         * Check whether a patch is applied.
         *
         * @param patchId the patch id
         * @return {@code true} if the given patch is currently applied, {@code false} otherwise
         */
        boolean isApplied(String patchId);

        /**
         * Get the modified state.
         *
         * @return the modified state
         */
        PatchableTarget.TargetInfo getModifiedState();

    }

    public interface ModificationCompletionCallback {

        /**
         * The modification has been successfully completed.
         */
        void completed();

        /**
         * The modification has been canceled. The installation did not change.
         */
        void canceled();

    }

    /**
     * Load the default installation manager implementation.
     *
     * @param jbossHome     the jboss home directory
     * @param moduleRoots   the module roots
     * @param bundlesRoots  the bundle roots
     * @param productConfig the product config
     * @return the installation manager implementation
     * @throws IOException
     */
    public static InstallationManager load(final File jbossHome, final List<File> moduleRoots, final List<File> bundlesRoots, final ProductConfig productConfig) throws IOException {
        final InstalledImage installedImage = installedImage(jbossHome);
        final InstalledIdentity identity = LayersFactory.load(installedImage, productConfig, moduleRoots, bundlesRoots);
        return new InstallationManagerImpl(identity);
    }

}
