/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq;

import org.apache.activemq.artemis.core.config.DivertConfiguration;
import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;

/**
 * Removes a divert.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class DivertRemove extends AbstractRemoveStepHandler {

    public static final DivertRemove INSTANCE = new DivertRemove();

    private DivertRemove() {
        super();
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {

        final String name = PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR)).getLastElement().getValue();
        final ServiceRegistry registry = context.getServiceRegistry(true);
        final ServiceName serviceName = MessagingServices.getActiveMQServiceName(PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR)));
        final ServiceController<?> service = registry.getService(serviceName);
        if (service != null && service.getState() == ServiceController.State.UP) {

            ActiveMQBroker server = ActiveMQBroker.class.cast(service.getValue());
            try {
                server.getActiveMQServerControl().destroyDivert(name);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                // TODO should this be an OFE instead?
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    protected void recoverServices(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {

        final ServiceRegistry registry = context.getServiceRegistry(true);
        final ServiceName serviceName = MessagingServices.getActiveMQServiceName(PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR)));
        final ServiceController<?> service = registry.getService(serviceName);
        if (service != null && service.getState() == ServiceController.State.UP) {

            final String name = PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR)).getLastElement().getValue();
            final DivertConfiguration divertConfiguration = DivertAdd.createDivertConfiguration(context, name, model);

            ActiveMQBroker server = ActiveMQBroker.class.cast(service.getValue());
            DivertAdd.createDivert(name, divertConfiguration, server.getActiveMQServerControl());
        }
    }
}