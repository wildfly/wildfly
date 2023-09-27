/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.service;

import static org.jboss.as.controller.PersistentResourceXMLDescription.builder;

import org.jboss.as.controller.PersistentResourceXMLDescription;
import org.jboss.as.controller.PersistentSubsystemSchema;
import org.jboss.as.controller.SubsystemSchema;
import org.jboss.as.controller.xml.VersionedNamespace;
import org.jboss.staxmapper.IntVersion;

/**
 * Enumerates the supported namespaces of the SAR subsystem.
 * @author Paul Ferraro
 */
public enum SarSubsystemSchema implements PersistentSubsystemSchema<SarSubsystemSchema> {

    VERSION_1_0(1),
    ;
    static final SarSubsystemSchema CURRENT = VERSION_1_0;

    private final VersionedNamespace<IntVersion, SarSubsystemSchema> namespace;

    SarSubsystemSchema(int major) {
        this.namespace = SubsystemSchema.createLegacySubsystemURN(SarExtension.SUBSYSTEM_NAME, new IntVersion(major));
    }

    @Override
    public VersionedNamespace<IntVersion, SarSubsystemSchema> getNamespace() {
        return this.namespace;
    }

    @Override
    public PersistentResourceXMLDescription getXMLDescription() {
        return builder(SarExtension.PATH, this.namespace).build();
    }
}
