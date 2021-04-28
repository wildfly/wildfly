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
import static org.jboss.dmr.ModelType.INT;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.PARAMS;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.AttributeMarshaller;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.dmr.ModelNode;

/**
 * remote acceptor resource definition
 *
 * @author <a href="http://jmesnil.net">Jeff Mesnil</a> (c) 2012 Red Hat Inc.
 */
public class InVMTransportDefinition extends AbstractTransportDefinition {

    public static final SimpleAttributeDefinition SERVER_ID = create("server-id", INT)
            .setAllowExpression(true)
            .setAttributeMarshaller(new AttributeMarshaller() {
                public void marshallAsAttribute(AttributeDefinition attribute, ModelNode resourceModel, boolean marshallDefault, XMLStreamWriter writer) throws XMLStreamException {
                    if(isMarshallable(attribute, resourceModel)) {
                        writer.writeAttribute(attribute.getXmlName(), resourceModel.get(attribute.getName()).asString());
                    }
                }
            })
            .setRestartAllServices()
            .build();

    static AttributeDefinition[] ATTRIBUTES = { SERVER_ID, PARAMS };

    public static InVMTransportDefinition createAcceptorDefinition(final boolean registerRuntimeOnly) {
        return new InVMTransportDefinition(registerRuntimeOnly, true, CommonAttributes.IN_VM_ACCEPTOR);
    }

    public static InVMTransportDefinition createConnectorDefinition(final boolean registerRuntimeOnly) {
        return new InVMTransportDefinition(registerRuntimeOnly, false, CommonAttributes.IN_VM_CONNECTOR);
    }

    private InVMTransportDefinition(final boolean registerRuntimeOnly, boolean isAcceptor, String specificType) {
        super(isAcceptor, specificType, registerRuntimeOnly, ATTRIBUTES);
    }
}
