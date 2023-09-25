/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.reactive.streams.operators;

import static org.jboss.as.controller.PersistentResourceXMLDescription.builder;

import org.jboss.as.controller.PersistentResourceXMLDescription;
import org.jboss.as.controller.PersistentSubsystemSchema;
import org.jboss.as.controller.SubsystemSchema;
import org.jboss.as.controller.xml.VersionedNamespace;
import org.jboss.staxmapper.IntVersion;

/**
 * Enumerates the supported namespaces of the MicroProfile reactive streams operators subsystem.
 * @author Paul Ferraro
 */
public enum MicroProfileReactiveStreamsOperatorsSubsystemSchema implements PersistentSubsystemSchema<MicroProfileReactiveStreamsOperatorsSubsystemSchema> {

    VERSION_1_0(1),
    ;
    static final MicroProfileReactiveStreamsOperatorsSubsystemSchema CURRENT = VERSION_1_0;

    private final VersionedNamespace<IntVersion, MicroProfileReactiveStreamsOperatorsSubsystemSchema> namespace;

    MicroProfileReactiveStreamsOperatorsSubsystemSchema(int major) {
        this.namespace = SubsystemSchema.createSubsystemURN(MicroProfileReactiveStreamsOperatorsExtension.SUBSYSTEM_NAME, new IntVersion(major));
    }

    @Override
    public VersionedNamespace<IntVersion, MicroProfileReactiveStreamsOperatorsSubsystemSchema> getNamespace() {
        return this.namespace;
    }

    @Override
    public PersistentResourceXMLDescription getXMLDescription() {
        return builder(MicroProfileReactiveStreamsOperatorsExtension.SUBSYSTEM_PATH, this.namespace).build();
    }
}
