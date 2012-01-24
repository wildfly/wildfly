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

package org.jboss.as.messaging.jms;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.messaging.CommonAttributes;
import org.jboss.dmr.ModelNode;

/**
 * Read handler for XML deployed JMS queues
 *
 * @author Stuart Douglas
 */
public class JMSQueueConfigurationRuntimeHandler extends AbstractJMSRuntimeHandler<ModelNode> {

    public static final JMSQueueConfigurationRuntimeHandler INSTANCE = new JMSQueueConfigurationRuntimeHandler();


    private JMSQueueConfigurationRuntimeHandler() {

    }

    public void registerAttributes(final ManagementResourceRegistration registry) {
        for (AttributeDefinition attr : CommonAttributes.JMS_QUEUE_ATTRIBUTES) {
            registry.registerReadOnlyAttribute(attr.getName(), this, AttributeAccess.Storage.RUNTIME);
        }
    }

    @Override
    protected void executeReadAttribute(final String attributeName, final OperationContext context, final ModelNode destination, final PathAddress address, final boolean includeDefault) {
        if (destination.hasDefined(attributeName)) {
            context.getResult().set(destination.get(attributeName));
        } else if(includeDefault) {
            for (AttributeDefinition attr : CommonAttributes.JMS_QUEUE_ATTRIBUTES) {
                if(attr.getName().equals(attributeName)) {
                    context.getResult().set(attr.getDefaultValue());
                }
            }
        }
    }
}
