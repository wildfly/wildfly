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
import org.wildfly.extension.undertow.Constants;

/**
 * Enumerates the supported schemas of the MicroProfile LRA participant subsystem.
 * @author Paul Ferraro
 */
enum MicroProfileLRAParticipantSubsystemSchema implements PersistentSubsystemSchema<MicroProfileLRAParticipantSubsystemSchema> {
    VERSION_1_0(1),
    VERSION_2_0(2),
    ;
    private final VersionedNamespace<IntVersion, MicroProfileLRAParticipantSubsystemSchema> namespace;

    MicroProfileLRAParticipantSubsystemSchema(int major) {
        this.namespace = SubsystemSchema.createSubsystemURN(MicroProfileLRAParticipantExtension.SUBSYSTEM_NAME, new IntVersion(major));
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
        if (!this.since(VERSION_2_0)) {
            // Version 1.0 declared default values ("default-server"/"default-host") for the 'proxy-server'
            // and 'proxy-host' attributes. Version 2.0 turns them into capability references, which cannot
            // declare default values. Preserve the previous behavior for configurations migrated from 1.0
            // by populating these defaults when the attributes are omitted.
            builder.setAdditionalOperationsGenerator((address, addOperation, operations) -> {
                if (!addOperation.hasDefined(CommonAttributes.PROXY_SERVER)) {
                    addOperation.get(CommonAttributes.PROXY_SERVER).set(Constants.DEFAULT_SERVER);
                }
                if (!addOperation.hasDefined(CommonAttributes.PROXY_HOST)) {
                    addOperation.get(CommonAttributes.PROXY_HOST).set(Constants.DEFAULT_HOST);
                }
            });
        }
        return builder.build();
    }
}