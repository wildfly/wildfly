/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.lra.participant;

import org.jboss.as.controller.PersistentResourceXMLDescription;
import org.jboss.as.controller.PersistentSubsystemSchema;
import org.jboss.as.controller.SubsystemSchema;
import org.jboss.as.controller.xml.VersionedNamespace;
import org.jboss.staxmapper.IntVersion;

/**
 * Enumerates the supported schemas of the MicroProfile LRA participant subsystem.
 * @author Paul Ferraro
 */
enum MicroProfileLRAParticipantSubsystemSchema implements PersistentSubsystemSchema<MicroProfileLRAParticipantSubsystemSchema> {
    VERSION_1_0(1, 0),
    VERSION_1_1(1, 1),
    ;
    private final VersionedNamespace<IntVersion, MicroProfileLRAParticipantSubsystemSchema> namespace;

    MicroProfileLRAParticipantSubsystemSchema(int major, int minor) {
        this.namespace = SubsystemSchema.createSubsystemURN(MicroProfileLRAParticipantExtension.SUBSYSTEM_NAME, new IntVersion(major, minor));
    }

    @Override
    public VersionedNamespace<IntVersion, MicroProfileLRAParticipantSubsystemSchema> getNamespace() {
        return this.namespace;
    }

    @Override
    public PersistentResourceXMLDescription getXMLDescription() {
        PersistentResourceXMLDescription.Factory factory = PersistentResourceXMLDescription.factory(this);
        PersistentResourceXMLDescription.Builder builder = factory.builder(MicroProfileLRAParticipantSubsystemDefinition.PATH);
        builder.addAttribute(MicroProfileLRAParticipantSubsystemDefinition.LRA_COORDINATOR_URL);
        builder.addAttribute(MicroProfileLRAParticipantSubsystemDefinition.PROXY_SERVER);
        builder.addAttribute(MicroProfileLRAParticipantSubsystemDefinition.PROXY_HOST);
        return builder.build();
    }
}