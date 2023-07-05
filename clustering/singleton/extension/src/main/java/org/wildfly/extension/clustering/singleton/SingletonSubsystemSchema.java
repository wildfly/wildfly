/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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
