package org.jboss.as.logging;

import org.jboss.as.controller.ModelVersion;

/**
 * Known model versions for the logging extension.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
enum KnownModelVersion {
    VERSION_1_1_0(ModelVersion.create(1, 1, 0), true),
    VERSION_1_2_0(ModelVersion.create(1, 2, 0), true),
    VERSION_1_3_0(ModelVersion.create(1, 3, 0), true),
    VERSION_2_0_0(ModelVersion.create(2, 0, 0), false),
    ;
    private final ModelVersion modelVersion;
    private final boolean hasTransformers;

    private KnownModelVersion(final ModelVersion modelVersion, final boolean hasTransformers) {
        this.modelVersion = modelVersion;
        this.hasTransformers = hasTransformers;
    }

    /**
     * Returns {@code true} if transformers should be registered against the model version.
     *
     * @return {@code true} if transformers should be registered, otherwise {@code false}
     */
    public boolean hasTransformers() {
        return hasTransformers;
    }

    /**
     * The model version.
     *
     * @return the model version
     */
    public ModelVersion getModelVersion() {
        return modelVersion;
    }
}
