/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq;

import static org.jboss.as.controller.SimpleAttributeDefinitionBuilder.create;
import static org.jboss.dmr.ModelType.STRING;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;

/**
 * Generic transport resource definition
 *
 * @author <a href="http://jmesnil.net">Jeff Mesnil</a> (c) 2012 Red Hat Inc.
 */
public class GenericTransportDefinition extends AbstractTransportDefinition {

    public static final SimpleAttributeDefinition SOCKET_BINDING = create("socket-binding", STRING)
            .setRequired(false)
            .setRestartAllServices()
            .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.SOCKET_BINDING_REF)
            .build();

    static AttributeDefinition[] ATTRIBUTES = { CommonAttributes.FACTORY_CLASS, SOCKET_BINDING, CommonAttributes.PARAMS };

    static GenericTransportDefinition createAcceptorDefinition(boolean registerRuntimeOnly) {
        return new GenericTransportDefinition(true, registerRuntimeOnly, CommonAttributes.ACCEPTOR);
    }

    static GenericTransportDefinition createConnectorDefinition(boolean registerRuntimeOnly) {
        return new GenericTransportDefinition(false, registerRuntimeOnly, CommonAttributes.CONNECTOR);
    }

    private GenericTransportDefinition(boolean isAcceptor, boolean registerRuntimeOnly, String specificType) {
        super(isAcceptor, specificType, registerRuntimeOnly, ATTRIBUTES);
    }
}
