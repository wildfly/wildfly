/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.health;

import static org.jboss.as.controller.PersistentResourceXMLDescription.builder;

import org.jboss.as.controller.PersistentResourceXMLDescription;
import org.jboss.as.controller.PersistentSubsystemSchema;
import org.jboss.as.controller.SubsystemSchema;
import org.jboss.as.controller.xml.VersionedNamespace;
import org.jboss.staxmapper.IntVersion;

/**
 * Enumerates the supported namespaces for the health subsystem.
 * @author Paul Ferraro
 */
public enum HealthSubsystemSchema implements PersistentSubsystemSchema<HealthSubsystemSchema> {

    VERSION_1_0(1),
    ;
    static final HealthSubsystemSchema CURRENT = VERSION_1_0;

    private final VersionedNamespace<IntVersion, HealthSubsystemSchema> namespace;

    HealthSubsystemSchema(int major) {
        this.namespace = SubsystemSchema.createSubsystemURN(HealthExtension.SUBSYSTEM_NAME, new IntVersion(major));
    }

    @Override
    public VersionedNamespace<IntVersion, HealthSubsystemSchema> getNamespace() {
        return this.namespace;
    }

    @Override
    public PersistentResourceXMLDescription getXMLDescription() {
        return builder(HealthExtension.SUBSYSTEM_PATH, this.namespace)
                .addAttributes(HealthSubsystemDefinition.ATTRIBUTES)
                .build();
    }
}
