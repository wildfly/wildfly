package org.jboss.as.patching.installation;

import org.jboss.as.patching.metadata.IdentityPatch;
import org.jboss.as.patching.metadata.Patch;

/**
 * The installation manager.
 *
 * @author Emanuel Muckenhuber
 */
public interface InstallationManager {

    /**
     * Get the installed identity.
     *
     * @return the identity
     */
    Identity getIdentity();

    /**
     * Modify the installation.
     *
     * @param callback a completed callback
     * @return the modification
     */
    InstallationModification modifyInstallation(ModificationCompletion callback);

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
        MutablePatchingTarget resolve(String name, IdentityPatch.LayerType type);

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
