/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.jboss.as.controller.persistence;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.parsing.Element;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * Context passed to {@link org.jboss.staxmapper.XMLElementWriter}s that marshal a subsystem.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class SubsystemMarshallingContext {

    private final ModelNode modelNode;
    private final XMLExtendedStreamWriter writer;

    public SubsystemMarshallingContext(final ModelNode modelNode, final XMLExtendedStreamWriter writer) {
        this.modelNode = modelNode;
        this.writer = writer;
    }

    public ModelNode getModelNode() {
        return modelNode;
    }

    public void startSubsystemElement(String namespaceURI, boolean empty) throws XMLStreamException {

        if (writer.getNamespaceContext().getPrefix(namespaceURI) == null) {
            // Unknown namespace; it becomes default
            writer.setDefaultNamespace(namespaceURI);
            if (empty) {
                writer.writeEmptyElement(Element.SUBSYSTEM.getLocalName());
            }
            else {
                writer.writeStartElement(Element.SUBSYSTEM.getLocalName());
            }
            writer.writeNamespace(null, namespaceURI);
        }
        else {
            if (empty) {
                writer.writeEmptyElement(namespaceURI, Element.SUBSYSTEM.getLocalName());
            }
            else {
                writer.writeStartElement(namespaceURI, Element.SUBSYSTEM.getLocalName());
            }
        }

    }
}
