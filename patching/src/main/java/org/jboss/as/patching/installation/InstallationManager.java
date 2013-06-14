package org.jboss.as.patching.installation;

import org.jboss.as.patching.metadata.LayerType;
import org.jboss.as.patching.metadata.Patch;

/**
 * The installation manager. It basically represents a mutable {@code InstalledIdentity}.
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
    public abstract InstallationModification modifyInstallation(ModificationCompletion callback);

    public abstract boolean requiresRestart();

    /**
     * Require a restart. This will set the patching service to read-only
     * and the server has to be restarted in order to execute the next
     * patch operation.
     *
     * In case the patch operation does not succeed it needs to clear the
     * reload required state using {@link #clearRestartRequired()}.
     *
     * @return this will return {@code true}
     */
    public abstract boolean restartRequired();

    public abstract void clearRestartRequired();

    public interface InstallationModification extends MutablePatchingTarget {

        /**
         * Get the current version of the identity.
         *
         * @return the identity version
         */
        String getVersion();

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
         * @param patchId the patch id
         * @param patchType the patch type
         */
        void apply(String patchId, Patch.PatchType patchType);

        /**
         * Get the modified state.
         *
         * @return the modified state
         */
        PatchableTarget.TargetInfo getModifiedState();

    }

    public interface ModificationCompletion {

        /**
         * The modification has been successfully completed.
         */
        void completed();

        /**
         * The modification has been canceled. The installation did not change.
         */
        void canceled();

    }

}
