/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.opentelemetry;

import static org.jboss.as.controller.PersistentResourceXMLDescription.factory;

import java.util.EnumSet;
import java.util.Set;

import org.jboss.as.controller.PersistentResourceXMLDescription;
import org.jboss.as.controller.PersistentSubsystemSchema;
import org.jboss.as.controller.SubsystemSchema;
import org.jboss.as.controller.xml.VersionedNamespace;
import org.jboss.as.version.Stability;
import org.jboss.staxmapper.IntVersion;
import org.wildfly.extension.observability.shared.FilterDefinitionRegistrar;

public enum OpenTelemetrySubsystemSchema implements PersistentSubsystemSchema<OpenTelemetrySubsystemSchema> {
    VERSION_1_0(1, 0, Stability.DEFAULT), // WildFly 25
    VERSION_1_1(1, 1, Stability.DEFAULT), // WildFly 31
    VERSION_1_2_COMMUNITY(1, 2, Stability.COMMUNITY), // WildFly 42
    ;

    public static final Set<OpenTelemetrySubsystemSchema> CURRENT = EnumSet.of(VERSION_1_1, VERSION_1_2_COMMUNITY);

    private final VersionedNamespace<IntVersion, OpenTelemetrySubsystemSchema> namespace;

    OpenTelemetrySubsystemSchema(int major, int minor, Stability stability) {
        this.namespace = SubsystemSchema.createSubsystemURN(OpenTelemetryConfigurationConstants.SUBSYSTEM_NAME,
                stability, new IntVersion(major, minor));
    }

    @Override
    public VersionedNamespace<IntVersion, OpenTelemetrySubsystemSchema> getNamespace() {
        return this.namespace;
    }

    @Override
    public PersistentResourceXMLDescription getXMLDescription() {
        PersistentResourceXMLDescription.Factory factory = factory(this);
        return factory.builder(OpenTelemetryConfigurationConstants.SUBSYSTEM_PATH)
                .addAttributes(OpenTelemetrySubsystemRegistrar.ATTRIBUTES.stream())
                .addChild(factory.builder(FilterDefinitionRegistrar.RESOURCE_REGISTRATION)
                        .addAttributes(FilterDefinitionRegistrar.ATTRIBUTES.stream())
                        .setXmlWrapperElement("filters")
                        .build())
                .build();
    }
}
