/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.clustering.ejb;

import org.jboss.as.controller.PersistentResourceXMLDescription;
import org.jboss.as.controller.PersistentSubsystemSchema;
import org.jboss.as.controller.SubsystemSchema;
import org.jboss.as.controller.xml.VersionedNamespace;
import org.jboss.staxmapper.IntVersion;

/**
 * Enumerates the schema versions for the distributable-ejb subsystem.
 * @author Paul Ferraro
 * @author Richard Achmatowicz
 */
public enum DistributableEjbSubsystemSchema implements PersistentSubsystemSchema<DistributableEjbSubsystemSchema> {

    VERSION_1_0(1, 0), // WildFly 27
    VERSION_2_0(2, 0); // WildFly 36   ;
    static final DistributableEjbSubsystemSchema CURRENT = VERSION_2_0;

    private final VersionedNamespace<IntVersion, DistributableEjbSubsystemSchema> namespace;

    DistributableEjbSubsystemSchema(int major, int minor) {
        this.namespace = SubsystemSchema.createLegacySubsystemURN(DistributableEjbExtension.SUBSYSTEM_NAME, new IntVersion(major, minor));
    }

    @Override
    public VersionedNamespace<IntVersion, DistributableEjbSubsystemSchema> getNamespace() {
        return this.namespace;
    }

    @Override
    public PersistentResourceXMLDescription getXMLDescription() {
        return DistributableEjbXMLDescriptionFactory.INSTANCE.apply(this);
    }
}
