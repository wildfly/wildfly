/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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

    public static InVMTransportDefinition createAcceptorDefinition(final boolean registerRuntimeOnlyValid) {
        return new InVMTransportDefinition(registerRuntimeOnlyValid, true, CommonAttributes.IN_VM_ACCEPTOR);
    }

    /**
     * @param registerRuntimeOnlyValid: no effect
     */
    public static InVMTransportDefinition createConnectorDefinition(final boolean registerRuntimeOnlyValid) {
        return new InVMTransportDefinition(registerRuntimeOnlyValid, false, CommonAttributes.IN_VM_CONNECTOR);
    }

    private InVMTransportDefinition(final boolean registerRuntimeOnlyValid, boolean isAcceptor, String specificType) {
        super(isAcceptor, specificType, registerRuntimeOnlyValid, ATTRIBUTES);
    }
}
