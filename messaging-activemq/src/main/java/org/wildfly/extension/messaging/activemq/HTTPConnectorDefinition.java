/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.messaging.activemq;

import static org.jboss.as.controller.SimpleAttributeDefinitionBuilder.create;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.PARAMS;

import javax.xml.stream.XMLStreamWriter;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.AttributeMarshaller;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * HTTP connector resource definition
 *
 * @author <a href="http://jmesnil.net">Jeff Mesnil</a> (c) 2012 Red Hat Inc.
 */
public class HTTPConnectorDefinition extends AbstractTransportDefinition {

    // for remote acceptor, the socket-binding is required
    public static final SimpleAttributeDefinition SOCKET_BINDING = create(GenericTransportDefinition.SOCKET_BINDING)
            .setRequired(true)
            .setAllowExpression(false) // references another resource
            .setAttributeMarshaller(new AttributeMarshaller() {
                public void marshallAsAttribute(AttributeDefinition attribute, ModelNode resourceModel, boolean marshallDefault, XMLStreamWriter writer) throws javax.xml.stream.XMLStreamException {
                    if (isMarshallable(attribute, resourceModel)) {
                        writer.writeAttribute(attribute.getXmlName(), resourceModel.get(attribute.getName()).asString());
                    }
                }
            })
            .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.SOCKET_BINDING_REF)
            .build();

    // for remote acceptor, the socket-binding is required
    public static final SimpleAttributeDefinition ENDPOINT = create("endpoint", ModelType.STRING)
            .setRequired(true)
            .setAllowExpression(false) // references another resource
            .build();

    public static final SimpleAttributeDefinition SERVER_NAME = create("server-name", ModelType.STRING)
            .setRequired(false)
            .setAllowExpression(false)
            .build();

    public HTTPConnectorDefinition(boolean registerRuntimeOnly) {
        super(false, CommonAttributes.HTTP_CONNECTOR, registerRuntimeOnly, SOCKET_BINDING, ENDPOINT, SERVER_NAME, PARAMS);
    }
}
