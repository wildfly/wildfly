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
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;

/**
 * Update handler adding a topic to the JMS subsystem. The
 * runtime action, will create the {@link JMSTopicService}.
 *
 * @author Emanuel Muckenhuber
 * @author <a href="mailto:andy.taylor@jboss.com">Andy Taylor</a>
 */
public class JMSTopicAdd extends AbstractAddStepHandler {

    public static final JMSTopicAdd INSTANCE = new JMSTopicAdd(JMSTopicDefinition.ATTRIBUTES);

    private JMSTopicAdd(AttributeDefinition... attributes) {
        super(attributes);
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        final String name = address.getLastElement().getValue();
        final ServiceName hqServiceName = MessagingServices.getHornetQServiceName(PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR)));
        final ServiceTarget serviceTarget = context.getServiceTarget();

        // Do not pass the JNDI bindings to HornetQ but install them directly instead so that the
        // dependencies from the BinderServices to the JMSQueueService are not broken
        JMSTopicService jmsTopicService = JMSTopicService.installService(name, hqServiceName, serviceTarget, new String[0]);

        final ModelNode entries = CommonAttributes.DESTINATION_ENTRIES.resolveModelAttribute(context, model);
        final String[] jndiBindings = JMSServices.getJndiBindings(entries);
        for (String jndiBinding : jndiBindings) {
            BinderServiceUtil.installBinderService(serviceTarget, jndiBinding, jmsTopicService);
        }
    }

    /**
     * @deprecated use {@link JMSTopicService#installService(String, org.jboss.msc.service.ServiceName, org.jboss.msc.service.ServiceTarget, String[])} instead.
     */
    @Deprecated
    public void installServices(final String name, final ServiceName hqServiceName, final ServiceTarget serviceTarget, final String[] jndiBindings) {
        JMSTopicService.installService(name, hqServiceName, serviceTarget, jndiBindings);
    }
}
