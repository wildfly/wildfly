/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.eesecurity;

import static org.jboss.as.controller.PersistentResourceXMLDescription.builder;

import org.jboss.as.controller.PersistentResourceXMLDescription;
import org.jboss.as.controller.PersistentSubsystemSchema;
import org.jboss.as.controller.SubsystemSchema;
import org.jboss.as.controller.xml.VersionedNamespace;
import org.jboss.staxmapper.IntVersion;

/**
 * Enumerates the supported subsystem schema namespaces of the EE security subsystem.
 * @author Paul Ferraro
 */
public enum EESecuritySubsystemSchema implements PersistentSubsystemSchema<EESecuritySubsystemSchema> {

    VERSION_1_0(1),
    ;
    static final EESecuritySubsystemSchema CURRENT = VERSION_1_0;

    private final VersionedNamespace<IntVersion, EESecuritySubsystemSchema> namespace;

    EESecuritySubsystemSchema(int major) {
        this.namespace = SubsystemSchema.createLegacySubsystemURN(EESecurityExtension.SUBSYSTEM_NAME, new IntVersion(major));
    }

    @Override
    public VersionedNamespace<IntVersion, EESecuritySubsystemSchema> getNamespace() {
        return this.namespace;
    }

    @Override
    public PersistentResourceXMLDescription getXMLDescription() {
        return builder(EESecurityExtension.SUBSYSTEM_PATH, this.namespace).build();
    }
}
