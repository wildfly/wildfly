/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.reactive.messaging;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PersistentResourceXMLDescription;
import org.jboss.as.controller.PersistentSubsystemSchema;
import org.jboss.as.controller.SubsystemSchema;
import org.jboss.as.controller.xml.VersionedNamespace;
import org.jboss.staxmapper.IntVersion;

/**
 * Enumerates the supported namespaces of the MicroProfile reactive messaging subsystem.
 * @author Paul Ferraro
 */
public enum MicroProfileReactiveMessagingSubsystemSchema implements PersistentSubsystemSchema<MicroProfileReactiveMessagingSubsystemSchema> {

    VERSION_1_0(1),
    VERSION_2_0(2),
    ;
    static final MicroProfileReactiveMessagingSubsystemSchema CURRENT = VERSION_2_0;

    private final VersionedNamespace<IntVersion, MicroProfileReactiveMessagingSubsystemSchema> namespace;

    MicroProfileReactiveMessagingSubsystemSchema(int major) {
        this.namespace = SubsystemSchema.createSubsystemURN(MicroProfileReactiveMessagingExtension.SUBSYSTEM_NAME, new IntVersion(major));
    }

    @Override
    public VersionedNamespace<IntVersion, MicroProfileReactiveMessagingSubsystemSchema> getNamespace() {
        return this.namespace;
    }




    @Override
    public PersistentResourceXMLDescription getXMLDescription() {

        PersistentResourceXMLDescription.Factory factory = PersistentResourceXMLDescription.factory(this);
        PersistentResourceXMLDescription.Builder builder = factory.builder(MicroProfileReactiveMessagingExtension.SUBSYSTEM_PATH);
        if (this.since(VERSION_2_0)) {
            builder.addChild(
                factory.builder(ConnectorOpenTelemetryTracingResourceDefinition.PATH)
                        .setXmlElementName(ConnectorOpenTelemetryTracingResourceDefinition.PATH.getKey())
                        .addAttributes(ConnectorOpenTelemetryTracingResourceDefinition.ATTRIBUTES.toArray(new AttributeDefinition[0]))
                        .build());
        }

        return builder.build();
    }
}
