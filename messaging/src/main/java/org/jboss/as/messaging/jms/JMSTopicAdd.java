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

import java.util.List;

import org.hornetq.jms.server.JMSServerManager;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.messaging.MessagingServices;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
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

    public static final JMSTopicAdd INSTANCE = new JMSTopicAdd();

    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        JndiEntriesAttribute.DESTINATION.validateAndSet(operation, model);
    }

    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {
        final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        final String name = address.getLastElement().getValue();
        final ServiceName hqServiceName = MessagingServices.getHornetQServiceName(PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR)));
        final ServiceTarget serviceTarget = context.getServiceTarget();

        final ModelNode entries = JndiEntriesAttribute.DESTINATION.resolveModelAttribute(context, model);
        final String[] jndiBindings = JndiEntriesAttribute.getJndiBindings(entries);
        installServices(verificationHandler, newControllers, name, hqServiceName, serviceTarget, jndiBindings);
    }

    public void installServices(final ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> newControllers, final String name, final ServiceName hqServiceName, final ServiceTarget serviceTarget, final String[] jndiBindings) {
        final JMSTopicService service = new JMSTopicService(name, jndiBindings);
        final ServiceName serviceName = JMSServices.getJmsTopicBaseServiceName(hqServiceName).append(name);

        final ServiceBuilder<Void> serviceBuilder = serviceTarget.addService(serviceName, service)
                .addDependency(JMSServices.getJmsManagerBaseServiceName(hqServiceName), JMSServerManager.class, service.getJmsServer())
                .setInitialMode(Mode.ACTIVE);
        org.jboss.as.server.Services.addServerExecutorDependency(serviceBuilder, service.getExecutorInjector(), false);
        if(verificationHandler != null) {
            serviceBuilder.addListener(verificationHandler);
        }

        final ServiceController<Void> controller = serviceBuilder.install();
        if(newControllers != null) {
            newControllers.add(controller);
        }
    }
}
