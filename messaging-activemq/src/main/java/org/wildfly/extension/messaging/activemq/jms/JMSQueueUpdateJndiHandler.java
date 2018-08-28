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

package org.wildfly.extension.messaging.activemq.jms;

import static org.wildfly.extension.messaging.activemq.jms.JMSQueueService.JMS_QUEUE_PREFIX;

import org.apache.activemq.artemis.jms.server.JMSServerManager;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 * Handler for "add-jndi" and "remove-jndi" operations on a JMS queue resource.
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
