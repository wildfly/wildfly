/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.opentelemetry;

import static org.jboss.as.controller.PersistentResourceXMLDescription.factory;

import org.jboss.as.controller.PersistentResourceXMLDescription;
import org.jboss.as.controller.PersistentSubsystemSchema;
import org.jboss.as.controller.SubsystemSchema;
import org.jboss.as.controller.xml.VersionedNamespace;
import org.jboss.staxmapper.IntVersion;

public enum OpenTelemetrySubsystemSchema implements PersistentSubsystemSchema<OpenTelemetrySubsystemSchema> {
    VERSION_1_0(1, 0), // WildFly 25
    VERSION_1_1(1, 1), // WildFly 31
    ;
    public static final OpenTelemetrySubsystemSchema CURRENT = VERSION_1_1;

    private final VersionedNamespace<IntVersion, OpenTelemetrySubsystemSchema> namespace;

    OpenTelemetrySubsystemSchema(int major, int minor) {
        this.namespace = SubsystemSchema.createSubsystemURN(OpenTelemetryConfigurationConstants.SUBSYSTEM_NAME, new IntVersion(major, minor));
    }

    @Override
    public VersionedNamespace<IntVersion, OpenTelemetrySubsystemSchema> getNamespace() {
        return this.namespace;
    }

    @Override
    public PersistentResourceXMLDescription getXMLDescription() {
        return factory(this)
                .builder(OpenTelemetryConfigurationConstants.SUBSYSTEM_PATH)
                .addAttributes(OpenTelemetrySubsystemRegistrar.ATTRIBUTES.stream())
                .build();
    }
}
