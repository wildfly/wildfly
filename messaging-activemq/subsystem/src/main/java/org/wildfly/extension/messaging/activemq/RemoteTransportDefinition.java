/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.messaging.activemq;

import static org.jboss.as.controller.SimpleAttributeDefinitionBuilder.create;

import javax.xml.stream.XMLStreamWriter;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.AttributeMarshaller;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.dmr.ModelNode;

/**
 * remote transports resource definition
 *
 * @author <a href="http://jmesnil.net">Jeff Mesnil</a> (c) 2012 Red Hat Inc.
 */
public class RemoteTransportDefinition extends AbstractTransportDefinition {

    // for remote acceptor, the socket-binding is required
    public static final SimpleAttributeDefinition SOCKET_BINDING = create(GenericTransportDefinition.SOCKET_BINDING)
            .setRequired(true)
            .setAttributeMarshaller(new AttributeMarshaller() {
                public void marshallAsAttribute(AttributeDefinition attribute, ModelNode resourceModel, boolean marshallDefault, XMLStreamWriter writer) throws javax.xml.stream.XMLStreamException {
                    if (isMarshallable(attribute, resourceModel)) {
                        writer.writeAttribute(attribute.getXmlName(), resourceModel.get(attribute.getName()).asString());
                    }
                }
            })
            .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.SOCKET_BINDING_REF)
            .build();

    static AttributeDefinition[] ATTRIBUTES = {SOCKET_BINDING, CommonAttributes.PARAMS, CommonAttributes.SSL_CONTEXT};

    static RemoteTransportDefinition createAcceptorDefinition(boolean registerRuntimeOnlyValid) {
        return new RemoteTransportDefinition(true, CommonAttributes.REMOTE_ACCEPTOR, registerRuntimeOnlyValid);
    }

    /**
     * @param registerRuntimeOnlyValid: no effect
     */
    static RemoteTransportDefinition createConnectorDefinition(boolean registerRuntimeOnlyValid) {
        return new RemoteTransportDefinition(false, CommonAttributes.REMOTE_CONNECTOR, registerRuntimeOnlyValid);
    }

    private RemoteTransportDefinition(boolean isAcceptor, String specificType, boolean registerRuntimeOnlyValid) {
        super(isAcceptor, specificType, registerRuntimeOnlyValid, ATTRIBUTES);
    }
}
