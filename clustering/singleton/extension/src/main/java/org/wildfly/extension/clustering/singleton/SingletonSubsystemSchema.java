/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.clustering.singleton;

import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.SubsystemSchema;
import org.jboss.as.controller.xml.VersionedNamespace;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.IntVersion;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * Enumeration of supported subsystem schemas.
 * @author Paul Ferraro
 */
public enum SingletonSubsystemSchema implements SubsystemSchema<SingletonSubsystemSchema> {

    VERSION_1_0(1, 0),
    ;
    static final SingletonSubsystemSchema CURRENT = VERSION_1_0;

    private final VersionedNamespace<IntVersion, SingletonSubsystemSchema> namespace;

    SingletonSubsystemSchema(int major, int minor) {
        this.namespace = SubsystemSchema.createLegacySubsystemURN(SingletonExtension.SUBSYSTEM_NAME, new IntVersion(major, minor));
    }

    @Override
    public VersionedNamespace<IntVersion, SingletonSubsystemSchema> getNamespace() {
        return this.namespace;
    }

    @Override
    public void readElement(XMLExtendedStreamReader reader, List<ModelNode> operations) throws XMLStreamException {
        new SingletonXMLReader(this).readElement(reader, operations);
    }
}
