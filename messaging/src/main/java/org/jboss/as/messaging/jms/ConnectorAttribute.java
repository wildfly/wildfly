/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.messaging.jms;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE_TYPE;
import static org.jboss.as.controller.registry.AttributeAccess.Flag.RESTART_ALL_SERVICES;
import static org.jboss.dmr.ModelType.OBJECT;
import static org.jboss.dmr.ModelType.STRING;

import java.util.Locale;
import java.util.ResourceBundle;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.messaging.Attribute;
import org.jboss.as.messaging.CommonAttributes;
import org.jboss.as.messaging.Element;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

/**
 * {@link org.jboss.as.controller.AttributeDefinition} for a connection factory connector
 *
 *  a <connector> used to defined (pooled/non-pooled) JMS CF.
 *  It is a map defined by connector name and undefined value.
 * (legacy code, previously the value could be a backup-connector name but HornetQ no longer supports it)
 *
 * @author Jeff Mesnil (c) 2012 Red Hat Inc.
 */
public class ConnectorAttribute extends SimpleAttributeDefinition {

    public static AttributeDefinition CONNECTOR = new ConnectorAttribute();

    protected ConnectorAttribute() {
        super(CommonAttributes.CONNECTOR, CommonAttributes.CONNECTOR, null, OBJECT, true, false, null, RESTART_ALL_SERVICES);
    }

    @Override
    public ModelNode addResourceAttributeDescription(ModelNode resourceDescription, ResourceDescriptionResolver resolver,
            Locale locale, ResourceBundle bundle) {
        final ModelNode result = super.addResourceAttributeDescription(resourceDescription, resolver, locale, bundle);
        result.get(VALUE_TYPE).set(STRING);
        return result;
    }

    @Override
    public ModelNode addOperationParameterDescription(ModelNode resourceDescription, String operationName,
            ResourceDescriptionResolver resolver, Locale locale, ResourceBundle bundle) {
        final ModelNode result = super.addOperationParameterDescription(resourceDescription, operationName, resolver, locale,
                bundle);
        result.get(VALUE_TYPE).set(STRING);
        return result;
    }

    @Override
    public void marshallAsElement(ModelNode resourceModel, boolean marshallDefault, XMLStreamWriter writer)
            throws XMLStreamException {
        if (resourceModel.hasDefined(Element.CONNECTOR.getLocalName())) {
            writer.writeStartElement(Element.CONNECTORS.getLocalName());
            for (Property connProp : resourceModel.get(Element.CONNECTOR.getLocalName()).asPropertyList()) {
                writer.writeStartElement(Element.CONNECTOR_REF.getLocalName());
                writer.writeAttribute(Attribute.CONNECTOR_NAME.getLocalName(), connProp.getName());
                writer.writeEndElement();
            }
            writer.writeEndElement();
        }
    }
}
