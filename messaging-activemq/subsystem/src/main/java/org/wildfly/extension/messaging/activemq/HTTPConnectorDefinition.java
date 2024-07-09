/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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

    /**
     * @param registerRuntimeOnlyValid: no effect
     */
    public HTTPConnectorDefinition(boolean registerRuntimeOnlyValid) {
        super(false, CommonAttributes.HTTP_CONNECTOR, registerRuntimeOnlyValid, SOCKET_BINDING, ENDPOINT, SERVER_NAME, PARAMS, CommonAttributes.SSL_CONTEXT);
    }
}
