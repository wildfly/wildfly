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

package org.jboss.as.messaging.jms;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.messaging.CommonAttributes.DURABLE;
import static org.jboss.as.messaging.CommonAttributes.SELECTOR;

import javax.jms.Queue;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.messaging.BinderServiceUtil;
import org.jboss.as.messaging.CommonAttributes;
import org.jboss.as.messaging.MessagingServices;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;

/**
 * Update handler adding a queue to the JMS subsystem. The
 * runtime action will create the {@link JMSQueueService}.
 *
 * @author Emanuel Muckenhuber
 * @author <a href="mailto:andy.taylor@jboss.com">Andy Taylor</a>
 */
public class JMSQueueAdd extends AbstractAddStepHandler {

    public static final JMSQueueAdd INSTANCE = new JMSQueueAdd(JMSQueueDefinition.ATTRIBUTES);

    private JMSQueueAdd(AttributeDefinition... attributes) {
        super(attributes);
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        final String name = address.getLastElement().getValue();
        final ServiceTarget serviceTarget = context.getServiceTarget();
        final ServiceName hqServiceName = MessagingServices.getHornetQServiceName(PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR)));

        final ModelNode selectorNode = SELECTOR.resolveModelAttribute(context, model);
        final boolean durable = DURABLE.resolveModelAttribute(context, model).asBoolean();

        final String selector = selectorNode.isDefined() ? selectorNode.asString() : null;

        // Do not pass the JNDI bindings to HornetQ but install them directly instead so that the
        // dependencies from the BinderServices to the JMSQueueService are not broken
        Service<Queue> queueService = JMSQueueService.installService(name, serviceTarget, hqServiceName, selector, durable, new String[0]);

        final ModelNode entries = CommonAttributes.DESTINATION_ENTRIES.resolveModelAttribute(context, model);
        final String[] jndiBindings = JMSServices.getJndiBindings(entries);
        for (String jndiBinding : jndiBindings) {
            BinderServiceUtil.installBinderService(serviceTarget, jndiBinding, queueService);
        }
    }

    /**
     * @deprecated use {@link JMSQueueService#installService(String, org.jboss.msc.service.ServiceTarget, org.jboss.msc.service.ServiceName, String, boolean, String[])} instead
     */
    @Deprecated
    public void installServices(final String name, final ServiceTarget serviceTarget, final ServiceName hqServiceName, final String selector, final boolean durable, final String[] jndiBindings) {
        JMSQueueService.installService(name, serviceTarget, hqServiceName, selector, durable, jndiBindings);
    }
}
