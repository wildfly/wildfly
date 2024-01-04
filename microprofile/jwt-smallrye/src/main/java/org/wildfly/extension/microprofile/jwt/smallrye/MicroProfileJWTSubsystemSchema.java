/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.jwt.smallrye;

import static org.jboss.as.controller.PersistentResourceXMLDescription.builder;

import org.jboss.as.controller.PersistentResourceXMLDescription;
import org.jboss.as.controller.PersistentSubsystemSchema;
import org.jboss.as.controller.SubsystemSchema;
import org.jboss.as.controller.xml.VersionedNamespace;
import org.jboss.staxmapper.IntVersion;

/**
 * Enumerates the supported subsystem namespaces of the MicroProfile JWT subsystem.
 * @author Paul Ferraro
 */
public enum MicroProfileJWTSubsystemSchema implements PersistentSubsystemSchema<MicroProfileJWTSubsystemSchema> {

    VERSION_1_0(1),
    ;
    static final MicroProfileJWTSubsystemSchema CURRENT = VERSION_1_0;

    private final VersionedNamespace<IntVersion, MicroProfileJWTSubsystemSchema> namespace;

    MicroProfileJWTSubsystemSchema(int major) {
        this.namespace = SubsystemSchema.createSubsystemURN(MicroProfileJWTExtension.SUBSYSTEM_NAME, new IntVersion(major));
    }

    @Override
    public VersionedNamespace<IntVersion, MicroProfileJWTSubsystemSchema> getNamespace() {
        return this.namespace;
    }

    @Override
    public PersistentResourceXMLDescription getXMLDescription() {
        return builder(MicroProfileJWTExtension.SUBSYSTEM_PATH, this.namespace).build();
    }
}
