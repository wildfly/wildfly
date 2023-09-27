/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.lra.coordinator;

import org.jboss.as.controller.PersistentResourceXMLDescription;
import org.jboss.as.controller.PersistentSubsystemSchema;
import org.jboss.as.controller.SubsystemSchema;
import org.jboss.as.controller.xml.VersionedNamespace;
import org.jboss.staxmapper.IntVersion;

import static org.jboss.as.controller.PersistentResourceXMLDescription.builder;

/**
 * Enumerates the supported schemas of the MicroProfile LRA coordinator subsystem.
 *
 * @author Paul Ferraro
 */
public enum MicroProfileLRACoordinatorSubsystemSchema implements PersistentSubsystemSchema<MicroProfileLRACoordinatorSubsystemSchema> {
    VERSION_1_0(1),
    ;

    private final VersionedNamespace<IntVersion, MicroProfileLRACoordinatorSubsystemSchema> namespace;

    MicroProfileLRACoordinatorSubsystemSchema(int major) {
        this.namespace = SubsystemSchema.createSubsystemURN(MicroProfileLRACoordinatorExtension.SUBSYSTEM_NAME, new IntVersion(major));
    }

    @Override
    public VersionedNamespace<IntVersion, MicroProfileLRACoordinatorSubsystemSchema> getNamespace() {
        return this.namespace;
    }

    @Override
    public PersistentResourceXMLDescription getXMLDescription() {
        return builder(MicroProfileLRACoordinatorSubsystemDefinition.PATH, this.namespace)
            .addAttributes(MicroProfileLRACoordinatorSubsystemDefinition.ATTRIBUTES)
            .build();
    }
}