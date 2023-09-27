/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.openapi;

import static org.jboss.as.controller.PersistentResourceXMLDescription.builder;

import org.jboss.as.controller.PersistentResourceXMLDescription;
import org.jboss.as.controller.PersistentSubsystemSchema;
import org.jboss.as.controller.SubsystemSchema;
import org.jboss.as.controller.xml.VersionedNamespace;
import org.jboss.staxmapper.IntVersion;

/**
 * Enumeration of MicroProfile OpenAPI subsystem schema versions.
 * @author Paul Ferraro
 */
public enum MicroProfileOpenAPISubsystemSchema implements PersistentSubsystemSchema<MicroProfileOpenAPISubsystemSchema> {

    VERSION_1_0(1, 0), // WildFly 19
    ;
    static final MicroProfileOpenAPISubsystemSchema CURRENT = VERSION_1_0;

    private final VersionedNamespace<IntVersion, MicroProfileOpenAPISubsystemSchema> namespace;

    MicroProfileOpenAPISubsystemSchema(int major, int minor) {
        this.namespace = SubsystemSchema.createSubsystemURN(MicroProfileOpenAPIExtension.SUBSYSTEM_NAME, new IntVersion(major, minor));
    }

    @Override
    public VersionedNamespace<IntVersion, MicroProfileOpenAPISubsystemSchema> getNamespace() {
        return this.namespace;
    }

    @Override
    public PersistentResourceXMLDescription getXMLDescription() {
        return builder(MicroProfileOpenAPISubsystemDefinition.PATH, this.namespace).build();
    }
}
