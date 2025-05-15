/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.opentelemetry;

import static org.jboss.as.controller.PersistentResourceXMLDescription.factory;

import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PersistentResourceXMLDescription;
import org.jboss.as.controller.PersistentSubsystemSchema;
import org.jboss.as.controller.SubsystemSchema;
import org.jboss.as.controller.xml.VersionedNamespace;
import org.jboss.as.version.Stability;
import org.jboss.staxmapper.IntVersion;

public enum OpenTelemetrySubsystemSchema implements PersistentSubsystemSchema<OpenTelemetrySubsystemSchema> {
    VERSION_1_0(1, 0, Stability.DEFAULT), // WildFly 25
    VERSION_1_1(1, 1, Stability.DEFAULT), // WildFly 31
    VERSION_2_0(2, 0, Stability.DEFAULT), // WildFly 36
    VERSION_2_0_PREVIEW(2, 0, Stability.PREVIEW), // WildFly 36
    ;
    public static final EnumSet<OpenTelemetrySubsystemSchema> CURRENT = EnumSet.of(VERSION_2_0, VERSION_2_0_PREVIEW);

    private final VersionedNamespace<IntVersion, OpenTelemetrySubsystemSchema> namespace;

    OpenTelemetrySubsystemSchema(int major, int minor, Stability stability) {
        this.namespace = SubsystemSchema.createSubsystemURN(OpenTelemetryConfigurationConstants.SUBSYSTEM_NAME, stability,
            new IntVersion(major, minor));
    }

    @Override
    public VersionedNamespace<IntVersion, OpenTelemetrySubsystemSchema> getNamespace() {
        return this.namespace;
    }

    @Override
    public PersistentResourceXMLDescription getXMLDescription() {
        PersistentResourceXMLDescription.Builder builder = factory(this)
            .builder(OpenTelemetryConfigurationConstants.SUBSYSTEM_PATH);

        List<AttributeDefinition> attributes = new LinkedList<>(OpenTelemetrySubsystemRegistrar.ATTRIBUTES);
        if (this.since(VERSION_2_0)) {
            attributes.add(OpenTelemetrySubsystemRegistrar.TRACES_EXPORT_INTERVAL);
        } else {
            attributes.add(OpenTelemetrySubsystemRegistrar.TRACE_BATCH_DELAY);
        }
        builder.addAttributes(attributes.stream());

        return builder.build();
    }
}
