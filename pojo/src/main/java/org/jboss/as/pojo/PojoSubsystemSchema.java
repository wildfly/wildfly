/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.pojo;

import static org.jboss.as.controller.PersistentResourceXMLDescription.builder;

import org.jboss.as.controller.PersistentResourceXMLDescription;
import org.jboss.as.controller.PersistentSubsystemSchema;
import org.jboss.as.controller.SubsystemSchema;
import org.jboss.as.controller.xml.VersionedNamespace;
import org.jboss.staxmapper.IntVersion;

/**
 * Enumerates the supported namespaces of the POJO subsystem.
 * @author Paul Ferraro
 */
public enum PojoSubsystemSchema implements PersistentSubsystemSchema<PojoSubsystemSchema> {

    VERSION_1_0(1),
    ;
    static final PojoSubsystemSchema CURRENT = VERSION_1_0;

    private final VersionedNamespace<IntVersion, PojoSubsystemSchema> namespace;

    PojoSubsystemSchema(int major) {
        this.namespace = SubsystemSchema.createLegacySubsystemURN(PojoExtension.SUBSYSTEM_NAME, new IntVersion(major));
    }

    @Override
    public VersionedNamespace<IntVersion, PojoSubsystemSchema> getNamespace() {
        return this.namespace;
    }

    @Override
    public PersistentResourceXMLDescription getXMLDescription() {
        return builder(PojoResource.PATH, this.namespace).build();
    }
}
