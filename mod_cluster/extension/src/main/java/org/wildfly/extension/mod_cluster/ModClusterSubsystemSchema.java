/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.mod_cluster;

import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.SubsystemSchema;
import org.jboss.as.controller.xml.VersionedNamespace;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.IntVersion;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * @author Jean-Frederic Clere
 * @author Paul Ferraro
 * @author Radoslav Husar
 */
public enum ModClusterSubsystemSchema implements SubsystemSchema<ModClusterSubsystemSchema> {

    MODCLUSTER_1_0(1, 0), // AS 7.0
    MODCLUSTER_1_1(1, 1), // EAP 6.0-6.2
    MODCLUSTER_1_2(1, 2), // EAP 6.3-6.4
    MODCLUSTER_2_0(2, 0), // WildFly 10, EAP 7.0
    MODCLUSTER_3_0(3, 0), // WildFly 11-13, EAP 7.1
    MODCLUSTER_4_0(4, 0), // WildFly 14-15, EAP 7.2
    MODCLUSTER_5_0(5, 0), // WildFly 16-26, EAP 7.3-7.4
    MODCLUSTER_6_0(6, 0), // WildFly 27-present, EAP 8.0-present
    ;
    public static final ModClusterSubsystemSchema CURRENT = MODCLUSTER_6_0;

    private final VersionedNamespace<IntVersion, ModClusterSubsystemSchema> namespace;

    ModClusterSubsystemSchema(int major, int minor) {
        this.namespace = SubsystemSchema.createLegacySubsystemURN(ModClusterExtension.SUBSYSTEM_NAME, new IntVersion(major, minor));
    }

    @Override
    public VersionedNamespace<IntVersion, ModClusterSubsystemSchema> getNamespace() {
        return this.namespace;
    }

    @Override
    public void readElement(XMLExtendedStreamReader reader, List<ModelNode> operations) throws XMLStreamException {
        new ModClusterSubsystemXMLReader(this).readElement(reader, operations);
    }
}
