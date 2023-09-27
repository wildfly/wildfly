/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.telemetry;

import static org.jboss.as.controller.PersistentResourceXMLDescription.builder;

import org.jboss.as.controller.PersistentResourceXMLDescription;
import org.jboss.as.controller.PersistentSubsystemSchema;
import org.jboss.as.controller.SubsystemSchema;
import org.jboss.as.controller.xml.VersionedNamespace;
import org.jboss.staxmapper.IntVersion;

enum MicroProfileTelemetrySubsystemSchema implements PersistentSubsystemSchema<MicroProfileTelemetrySubsystemSchema> {
    VERSION_1_0(1, 0), // WildFly 28
    ;

    public static final MicroProfileTelemetrySubsystemSchema CURRENT = VERSION_1_0;

    private final VersionedNamespace<IntVersion, MicroProfileTelemetrySubsystemSchema> namespace;

    MicroProfileTelemetrySubsystemSchema(int major, int minor) {
        this.namespace = SubsystemSchema.createSubsystemURN(MicroProfileTelemetryExtension.SUBSYSTEM_NAME, new IntVersion(major, minor));
    }

    @Override
    public VersionedNamespace<IntVersion, MicroProfileTelemetrySubsystemSchema> getNamespace() {
        return this.namespace;
    }

    @Override
    public PersistentResourceXMLDescription getXMLDescription() {
        return builder(MicroProfileTelemetryExtension.SUBSYSTEM_PATH, this.namespace).build();
    }
}

