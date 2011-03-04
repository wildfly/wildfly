/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.ejb3;

import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

import javax.xml.stream.XMLStreamException;
import java.util.List;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

/**
 * Create a subsystem add directive from the given XML input.
 *
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
class EJB3SubsystemParser implements XMLElementReader<List<ModelNode>>, XMLElementWriter<SubsystemMarshallingContext> {
    //protected static final String NAMESPACE = Namespace.EJB3_1_0.getUriString();
    protected static final String NAMESPACE = "urn:jboss:domain:ejb3:1.0";

    private static final EJB3SubsystemParser instance = new EJB3SubsystemParser();

    public static EJB3SubsystemParser getInstance() {
        return instance;
    }

    @Override
    public void readElement(final XMLExtendedStreamReader reader, final List<ModelNode> list) throws XMLStreamException {
        // parse <jboss-ejb3> domain element

        final ModelNode subsystem = new ModelNode();
        subsystem.get(OP).set(ADD);
        subsystem.get(OP_ADDR).add(SUBSYSTEM, EJB3Extension.SUBSYSTEM_NAME);
        list.add(subsystem);

        /*
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case EJB3_1_0:
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case ASSEMBLY_DESCRIPTOR:
                            subsystem.get(ASSEMBLY_DESCRIPTOR).set(parseAssemblyDescriptor(reader));
                            break;
                        default:
                            throw ParseUtils.unexpectedElement(reader);
                    }
                    break;
                default:
                    throw ParseUtils.unexpectedElement(reader);
            }
        }
        */
    }

    /*
    private static ModelNode parseAssemblyDescriptor(XMLExtendedStreamReader reader) throws XMLStreamException {
        ModelNode assemblyDescriptor = new ModelNode();
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final String localName = reader.getLocalName();
            final Element element = Element.forName(localName);
            switch (element) {
                case UNKNOWN:
                    List<ModelNode> result = new ArrayList<ModelNode>();
                    reader.handleAny(result);
                    assemblyDescriptor.get(localName).set(result.get(0));
                    break;
            }
        }
        return assemblyDescriptor;
    }
    */

    @Override
    public void writeContent(final XMLExtendedStreamWriter writer, final SubsystemMarshallingContext context) throws XMLStreamException {
        // //TODO seems to be a problem with empty elements cleaning up the queue in FormattingXMLStreamWriter.runAttrQueue
        //context.startSubsystemElement(NewManagedBeansExtension.NAMESPACE, true);
        context.startSubsystemElement(NAMESPACE, false);
        writer.writeEndElement();
    }
}
