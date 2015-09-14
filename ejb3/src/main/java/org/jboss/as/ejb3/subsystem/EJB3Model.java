package org.jboss.as.ejb3.subsystem;

import org.jboss.as.controller.ModelVersion;

/**
 * Enumerates the supported model versions.
 * @author Paul Ferraro
 * @author Richard Achmatowicz
 */
public enum EJB3Model {

    VERSION_1_2_0(1, 2, 0),
    VERSION_1_2_1(1, 2, 1),
    VERSION_1_3_0(1, 3, 0),
    VERSION_4_0_0(4, 0, 0)
    ;

    static final EJB3Model CURRENT = VERSION_4_0_0;

    private final ModelVersion version;

    EJB3Model(int major, int minor, int micro) {
        this.version = ModelVersion.create(major, minor, micro);
    }

    public ModelVersion getVersion() {
        return this.version;
    }

    /**
     * Indicates whether this model is more recent than the specified version and thus requires transformation
     * @param version a model version
     * @return true this this model is more recent than the specified version, false otherwise
     */
    public boolean requiresTransformation(ModelVersion version) {
        return ModelVersion.compare(this.version, version) < 0;
    }

    /**
     * Indicates whether this model is equal to the specified version
     * @param version a model version
     * @return true this model is equal to the specified version, false otherwise
     */
    public boolean matches(ModelVersion version) {
        return this.version.equals(version);
    }

}
