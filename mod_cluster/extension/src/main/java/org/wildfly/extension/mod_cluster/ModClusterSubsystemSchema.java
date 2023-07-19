/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
    MODCLUSTER_6_0(6, 0), // WildFly 27-present
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
