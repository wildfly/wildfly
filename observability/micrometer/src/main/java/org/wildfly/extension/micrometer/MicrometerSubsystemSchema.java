/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.micrometer;

import static org.jboss.as.controller.PersistentResourceXMLDescription.factory;

import org.jboss.as.controller.PersistentResourceXMLDescription;
import org.jboss.as.controller.PersistentSubsystemSchema;
import org.jboss.as.controller.SubsystemSchema;
import org.jboss.as.controller.xml.VersionedNamespace;
import org.jboss.staxmapper.IntVersion;

public enum MicrometerSubsystemSchema implements PersistentSubsystemSchema<MicrometerSubsystemSchema> {
    VERSION_1_0(1, 0), // WildFly 28
    VERSION_1_1(1, 1), // WildFly 29.0.0.Alpha1
    ;

    public static final MicrometerSubsystemSchema CURRENT = VERSION_1_1;

    private final VersionedNamespace<IntVersion, MicrometerSubsystemSchema> namespace;

    MicrometerSubsystemSchema(int major, int minor) {
        this.namespace = SubsystemSchema.createSubsystemURN(MicrometerConfigurationConstants.NAME,
                new IntVersion(major, minor));
    }

    @Override
    public VersionedNamespace<IntVersion, MicrometerSubsystemSchema> getNamespace() {
        return this.namespace;
    }

    @Override
    public PersistentResourceXMLDescription getXMLDescription() {
        return factory(this)
                .builder(MicrometerSubsystemRegistrar.PATH)
                .addAttributes(MicrometerSubsystemRegistrar.ATTRIBUTES)
                .build();
    }
}
