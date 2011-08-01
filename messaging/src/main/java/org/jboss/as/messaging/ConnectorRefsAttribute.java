/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.messaging;

import static org.jboss.as.messaging.CommonAttributes.ENTRIES;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.as.controller.ListAttributeDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * {@link org.jboss.as.controller.AttributeDefinition} for a list of  {@code connector-ref}s.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class ConnectorRefsAttribute extends ListAttributeDefinition {

    public static final ConnectorRefsAttribute STATIC_CONNECTORS = new ConnectorRefsAttribute(Element.STATIC_CONNECTORS);

    public static final ConnectorRefsAttribute BROADCAST_GROUP = new ConnectorRefsAttribute(null);

    private final Element wrapper;

    private ConnectorRefsAttribute(final Element wrapper) {
        super(CommonAttributes.CONNECTORS, CommonAttributes.CONNECTOR_REF_STRING, wrapper == null, 1, Integer.MAX_VALUE, new StringLengthValidator(1));
        this.wrapper = wrapper;
    }

    @Override
    public void marshallAsElement(ModelNode resourceModel, XMLStreamWriter writer) throws XMLStreamException {
        if (resourceModel.hasDefined(getName())) {
            List<ModelNode> list = resourceModel.get(getName()).asList();
            if (list.size() > 0) {

                if (wrapper != null) {
                    writer.writeStartElement(wrapper.getLocalName());
                }

                for (ModelNode child : list) {
                    writer.writeStartElement(getXmlName());
                    writer.writeCharacters(child.asString());
                    writer.writeEndElement();
                }

                if (wrapper != null) {
                    writer.writeEndElement();
                }
            }
        }
    }

    @Override
    protected void addValueTypeDescription(ModelNode node) {
        node.get(ModelDescriptionConstants.VALUE_TYPE).set(ModelType.STRING);
    }
}
