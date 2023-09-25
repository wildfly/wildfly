/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jdr;

import static org.jboss.as.controller.PersistentResourceXMLDescription.builder;

import org.jboss.as.controller.PersistentResourceXMLDescription;
import org.jboss.as.controller.PersistentSubsystemSchema;
import org.jboss.as.controller.SubsystemSchema;
import org.jboss.as.controller.xml.VersionedNamespace;
import org.jboss.staxmapper.IntVersion;

/**
 * The namespaces supported by the JDR extension.
 *
 * @author Brian Stansberry
 * @author Mike M. Clark
 */
enum JdrReportSubsystemSchema implements PersistentSubsystemSchema<JdrReportSubsystemSchema> {

    VERSION_1_0(1),
    ;
    static final JdrReportSubsystemSchema CURRENT = VERSION_1_0;

    private final VersionedNamespace<IntVersion, JdrReportSubsystemSchema> namespace;

    JdrReportSubsystemSchema(int major) {
        this.namespace = SubsystemSchema.createLegacySubsystemURN(JdrReportExtension.SUBSYSTEM_NAME, new IntVersion(major));
    }

    @Override
    public VersionedNamespace<IntVersion, JdrReportSubsystemSchema> getNamespace() {
        return this.namespace;
    }

    @Override
    public PersistentResourceXMLDescription getXMLDescription() {
        return builder(JdrReportExtension.SUBSYSTEM_PATH, this.namespace).build();
    }
}
