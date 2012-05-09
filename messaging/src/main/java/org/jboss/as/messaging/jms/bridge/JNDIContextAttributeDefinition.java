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

package org.jboss.as.messaging.jms.bridge;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE_TYPE;
import static org.jboss.as.messaging.Element.PROPERTY;
import static org.jboss.dmr.ModelType.OBJECT;
import static org.jboss.dmr.ModelType.STRING;

import java.util.Locale;
import java.util.ResourceBundle;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.messaging.Attribute;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

/**
 * {@link org.jboss.as.controller.AttributeDefinition} for a JNDI context hashtable
 *
 * @author Jeff Mesnil (c) 2012 Red Hat Inc.
 */
public class JNDIContextAttributeDefinition extends SimpleAttributeDefinition {

    protected JNDIContextAttributeDefinition(String name, String xmlName) {
        super(name, xmlName, null, OBJECT, true, false, null);
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
        if (resourceModel.hasDefined(getName())) {
            ModelNode context = resourceModel.get(getName());

            writer.writeStartElement(getXmlName());
            for (Property property : context.asPropertyList()) {
                writer.writeStartElement(PROPERTY.getLocalName());
                writer.writeAttribute(Attribute.KEY.getLocalName(), property.getName());
                writer.writeAttribute(Attribute.VALUE.getLocalName(), property.getValue().asString());
                writer.writeEndElement();
            }
            writer.writeEndElement();
        }
    }
}
