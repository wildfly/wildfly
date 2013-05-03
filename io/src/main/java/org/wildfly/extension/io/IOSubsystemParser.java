/*
 *
 *  JBoss, Home of Professional Open Source.
 *  Copyright 2013, Red Hat, Inc., and individual contributors
 *  as indicated by the @author tags. See the copyright.txt file in the
 *  distribution for a full listing of individual contributors.
 *
 *  This is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as
 *  published by the Free Software Foundation; either version 2.1 of
 *  the License, or (at your option) any later version.
 *
 *  This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this software; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  02110-1301 USA, or see the FSF site: http://www.fsf.org.
 * /
 */

package org.wildfly.extension.io;

import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;

import java.util.List;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2013 Red Hat Inc.
 */
class IOSubsystemParser implements XMLStreamConstants, XMLElementReader<List<ModelNode>>, XMLElementWriter<SubsystemMarshallingContext> {
    static final IOSubsystemParser INSTANCE = new IOSubsystemParser();

    private IOSubsystemParser() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeContent(XMLExtendedStreamWriter writer, SubsystemMarshallingContext context) throws XMLStreamException {
        context.startSubsystemElement(Namespace.CURRENT.getUriString(), false);
        ModelNode model = context.getModelNode();
        WorkerResourceDefinition.INSTANCE.persist(writer, model);
        BufferPoolResourceDefinition.INSTANCE.persist(writer, model);
        writer.writeEndElement();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readElement(XMLExtendedStreamReader reader, List<ModelNode> list) throws XMLStreamException {
        PathAddress address = PathAddress.pathAddress(IOExtension.SUBSYSTEM_PATH);
        final ModelNode subsystem = Util.createAddOperation(address);
        list.add(subsystem);

        final Namespace namespace = Namespace.forUri(reader.getNamespaceURI());
        // elements
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (namespace) {
                case IO_1_0: {
                    switch (reader.getLocalName()) {
                        case Constants.WORKER: {
                            WorkerResourceDefinition.INSTANCE.parse(reader, address, list);
                            break;
                        }
                        case Constants.BUFFER_POOL: {
                            BufferPoolResourceDefinition.INSTANCE.parse(reader, address, list);
                            break;
                        }
                        default: {
                            throw unexpectedElement(reader);
                        }
                    }
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        ParseUtils.requireNoContent(reader);
    }
}

