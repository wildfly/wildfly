/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq.jms;

import static org.wildfly.extension.messaging.activemq.jms.JMSTopicService.JMS_TOPIC_PREFIX;

import org.apache.activemq.artemis.jms.server.JMSServerManager;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 * Handler for "add-jndi" and "remove-jndi" operations on a Jakarta Messaging topic resource.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class JMSTopicUpdateJndiHandler extends AbstractUpdateJndiHandler {

    private JMSTopicUpdateJndiHandler(boolean addOperation) {
        super(addOperation);
    }

    @Override
    protected void addJndiName(JMSServerManager jmsServerManager, String resourceName, String jndiName) throws Exception {
        jmsServerManager.addTopicToBindingRegistry(JMS_TOPIC_PREFIX + resourceName, jndiName);
    }

    @Override
    protected void removeJndiName(JMSServerManager jmsServerManager, String resourceName, String jndiName) throws Exception {
        jmsServerManager.removeTopicFromBindingRegistry(JMS_TOPIC_PREFIX + resourceName, jndiName);
    }

    static void registerOperations(ManagementResourceRegistration registry, ResourceDescriptionResolver resolver) {
        JMSTopicUpdateJndiHandler add = new JMSTopicUpdateJndiHandler(true);
        add.registerOperation(registry, resolver);

        JMSTopicUpdateJndiHandler remove = new JMSTopicUpdateJndiHandler(false);
        remove.registerOperation(registry, resolver);
    }
}
