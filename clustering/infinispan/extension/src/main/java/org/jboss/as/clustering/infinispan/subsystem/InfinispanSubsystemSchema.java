/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.SubsystemSchema;
import org.jboss.as.controller.xml.VersionedNamespace;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.IntVersion;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * Enumeration of the supported subsystem xml schemas.
 * @author Paul Ferraro
 */
public enum InfinispanSubsystemSchema implements SubsystemSchema<InfinispanSubsystemSchema> {
/*  Unsupported, for documentation purposes only.
    VERSION_1_0(1, 0), // AS 7.0
    VERSION_1_1(1, 1), // AS 7.1.0
    VERSION_1_2(1, 2), // AS 7.1.1
    VERSION_1_3(1, 3), // AS 7.1.2
    VERSION_1_4(1, 4), // AS 7.2.0
*/
    VERSION_1_5(1, 5), // EAP 6.3
    VERSION_2_0(2, 0), // WildFly 8
    VERSION_3_0(3, 0), // WildFly 9
    VERSION_4_0(4, 0), // WildFly 10/11
    VERSION_5_0(5, 0), // WildFly 12
    VERSION_6_0(6, 0), // WildFly 13
    VERSION_7_0(7, 0), // WildFly 14-15
    VERSION_8_0(8, 0), // WildFly 16
    VERSION_9_0(9, 0), // WildFly 17-19
    VERSION_9_1(9, 1), // EAP 7.3.4
    VERSION_10_0(10, 0), // WildFly 20
    VERSION_11_0(11, 0), // WildFly 21
    VERSION_12_0(12, 0), // WildFly 23, EAP 7.4
    VERSION_13_0(13, 0), // WildFly 24-26
    VERSION_14_0(14, 0), // WildFly 27-35
    VERSION_15_0(15, 0), // WildFly 36-present
    ;
    static final InfinispanSubsystemSchema CURRENT = VERSION_15_0;

    private final VersionedNamespace<IntVersion, InfinispanSubsystemSchema> namespace;

    InfinispanSubsystemSchema(int major, int minor) {
        this.namespace = SubsystemSchema.createLegacySubsystemURN(InfinispanExtension.SUBSYSTEM_NAME, new IntVersion(major, minor));
    }

    @Override
    public VersionedNamespace<IntVersion, InfinispanSubsystemSchema> getNamespace() {
        return this.namespace;
    }

    @Override
    public void readElement(XMLExtendedStreamReader reader, List<ModelNode> operations) throws XMLStreamException {
        new InfinispanSubsystemXMLReader(this).readElement(reader, operations);
    }
}
