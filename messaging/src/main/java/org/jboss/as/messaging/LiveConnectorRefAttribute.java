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

import static org.jboss.as.messaging.MessagingMessages.MESSAGES;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Attribute definition for the live-connector-ref attribute.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class LiveConnectorRefAttribute extends SimpleAttributeDefinition {

    public static final LiveConnectorRefAttribute INSTANCE = new LiveConnectorRefAttribute();

    private LiveConnectorRefAttribute() {
        super(CommonAttributes.LIVE_CONNECTOR_REF_STRING, ModelType.STRING, true, AttributeAccess.Flag.RESTART_ALL_SERVICES);
    }

    @Override
    public void marshallAsAttribute(ModelNode resourceModel, boolean marshallDefault, XMLStreamWriter writer) throws XMLStreamException {
        throw MESSAGES.cannotMarshalAttribute(CommonAttributes.LIVE_CONNECTOR_REF_STRING);
    }

    @Override
    public void marshallAsElement(ModelNode resourceModel, boolean marshallDefault, XMLStreamWriter writer) throws XMLStreamException {
        if (isMarshallable(resourceModel, marshallDefault)) {
            writer.writeStartElement(getXmlName());
            writer.writeAttribute(Attribute.CONNECTOR_NAME.getLocalName(), resourceModel.get(getName()).asString());
            writer.writeEndElement();
        }
    }
}
