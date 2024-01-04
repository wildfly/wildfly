/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.microprofile.faulttolerance;

import static org.jboss.as.controller.PersistentResourceXMLDescription.builder;

import org.jboss.as.controller.PersistentResourceXMLDescription;
import org.jboss.as.controller.PersistentSubsystemSchema;
import org.jboss.as.controller.SubsystemSchema;
import org.jboss.as.controller.xml.VersionedNamespace;
import org.jboss.staxmapper.IntVersion;

/**
 * Enumeration of supported subsystem schemas.
 *
 * @author Radoslav Husar
 */
public enum MicroProfileFaultToleranceSchema implements PersistentSubsystemSchema<MicroProfileFaultToleranceSchema> {

    VERSION_1_0(1, 0), // WildFly 19-present
    ;
    public static final MicroProfileFaultToleranceSchema CURRENT = VERSION_1_0;

    private final VersionedNamespace<IntVersion, MicroProfileFaultToleranceSchema> namespace;

    MicroProfileFaultToleranceSchema(int major, int minor) {
        this.namespace = SubsystemSchema.createSubsystemURN(MicroProfileFaultToleranceExtension.SUBSYSTEM_NAME, new IntVersion(major, minor));
    }

    @Override
    public VersionedNamespace<IntVersion, MicroProfileFaultToleranceSchema> getNamespace() {
        return this.namespace;
    }

    @Override
    public PersistentResourceXMLDescription getXMLDescription() {
        return builder(MicroProfileFaultToleranceResourceDefinition.PATH, this.namespace).build();
    }
}
