/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq.jms;

import static org.wildfly.extension.messaging.activemq.jms.JMSQueueService.JMS_QUEUE_PREFIX;

import org.apache.activemq.artemis.jms.server.JMSServerManager;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 * Handler for "add-jndi" and "remove-jndi" operations on a Jakarta Messaging queue resource.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class JMSQueueUpdateJndiHandler extends AbstractUpdateJndiHandler {

    private JMSQueueUpdateJndiHandler(boolean addOperation) {
        super(addOperation);
    }

    @Override
    protected void addJndiName(JMSServerManager jmsServerManager, String resourceName, String jndiName) throws Exception {
        jmsServerManager.addQueueToBindingRegistry(JMS_QUEUE_PREFIX + resourceName, jndiName);
    }

    @Override
    protected void removeJndiName(JMSServerManager jmsServerManager, String resourceName, String jndiName) throws Exception {
        jmsServerManager.removeQueueFromBindingRegistry(JMS_QUEUE_PREFIX + resourceName, jndiName);
    }

    static void registerOperations(ManagementResourceRegistration registry, ResourceDescriptionResolver resolver) {
        JMSQueueUpdateJndiHandler add = new JMSQueueUpdateJndiHandler(true);
        add.registerOperation(registry, resolver);

        JMSQueueUpdateJndiHandler remove = new JMSQueueUpdateJndiHandler(false);
        remove.registerOperation(registry, resolver);
    }
}
