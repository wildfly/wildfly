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

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.as.controller.ListAttributeDefinition;
import org.jboss.as.controller.PrimitiveListAttributeDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * {@link org.jboss.as.controller.AttributeDefinition} for a list of  {@code connector-ref}s.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class ConnectorRefsAttribute extends PrimitiveListAttributeDefinition {

    public static final ConnectorRefsAttribute CLUSTER_CONNECTION_CONNECTORS = new ConnectorRefsAttribute(CommonAttributes.STATIC_CONNECTORS, false, true);

    public static final ConnectorRefsAttribute BRIDGE_CONNECTORS = new ConnectorRefsAttribute(CommonAttributes.STATIC_CONNECTORS, true, true);

    public static final ConnectorRefsAttribute BROADCAST_GROUP = new ConnectorRefsAttribute(CommonAttributes.CONNECTORS, false, true);

    private final boolean wrap;

    private ConnectorRefsAttribute(final String name, boolean wrap, boolean allowNull) {
        super(name, CommonAttributes.CONNECTOR_REF_STRING, allowNull, ModelType.STRING, 1, Integer.MAX_VALUE, new StringLengthValidator(1));
        this.wrap = wrap;
    }

    @Override
    public void marshallAsElement(ModelNode resourceModel, XMLStreamWriter writer) throws XMLStreamException {
        if (resourceModel.hasDefined(getName())) {
            List<ModelNode> list = resourceModel.get(getName()).asList();
            if (list.size() > 0) {

                if (wrap) {
                    writer.writeStartElement(Element.STATIC_CONNECTORS.getLocalName());
                }

                for (ModelNode child : list) {
                    writer.writeStartElement(getXmlName());
                    writer.writeCharacters(child.asString());
                    writer.writeEndElement();
                }

                if (wrap) {
                    writer.writeEndElement();
                }
            }
        }
    }
}
