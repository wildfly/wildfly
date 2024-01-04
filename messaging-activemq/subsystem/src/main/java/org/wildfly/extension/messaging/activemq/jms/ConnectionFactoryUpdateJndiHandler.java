/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq.jms;

import org.apache.activemq.artemis.jms.server.JMSServerManager;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 * Handler for "add-jndi" and "remove-jndi" operations on a connection factory resource.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class ConnectionFactoryUpdateJndiHandler extends AbstractUpdateJndiHandler {

    private ConnectionFactoryUpdateJndiHandler(boolean addOperation) {
        super(addOperation);
    }

    @Override
    protected void addJndiName(JMSServerManager jmsServerManager, String resourceName, String jndiName) throws Exception {
        jmsServerManager.addConnectionFactoryToBindingRegistry(resourceName, jndiName);
    }

    @Override
    protected void removeJndiName(JMSServerManager jmsServerManager, String resourceName, String jndiName) throws Exception {
        jmsServerManager.removeConnectionFactoryFromBindingRegistry(resourceName, jndiName);
    }

    static void registerOperations(ManagementResourceRegistration registry, ResourceDescriptionResolver resolver) {
        ConnectionFactoryUpdateJndiHandler add = new ConnectionFactoryUpdateJndiHandler(true);
        add.registerOperation(registry, resolver);

        ConnectionFactoryUpdateJndiHandler remove = new ConnectionFactoryUpdateJndiHandler(false);
        remove.registerOperation(registry, resolver);
    }
}
