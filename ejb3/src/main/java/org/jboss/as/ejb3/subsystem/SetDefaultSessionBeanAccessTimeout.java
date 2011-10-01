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

package org.jboss.as.ejb3.subsystem;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.ejb3.component.DefaultAccessTimeoutService;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;

/**
 * @author Stuart Douglas
 */
public class SetDefaultSessionBeanAccessTimeout implements OperationStepHandler {


    private final String attribute;
    private final ServiceName serviceName;

    public SetDefaultSessionBeanAccessTimeout(final String attribute, final ServiceName serviceName) {
        this.attribute = attribute;
        this.serviceName = serviceName;
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

        final int timeout = operation.require(ModelDescriptionConstants.VALUE).asInt();
        // update the model
        // first get the ModelNode for the address on which this operation was executed. i.e. /subsystem=ejb3
        ModelNode model = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS).getModel();
        model.get(attribute).set(timeout);
        if (context.getType() == OperationContext.Type.SERVER) {
            context.addStep(new DefaultStatefulTimeoutUpdateHandler(timeout), OperationContext.Stage.RUNTIME);
        }
        // complete the step
        context.completeStep();
    }


    class DefaultStatefulTimeoutUpdateHandler implements OperationStepHandler {

        private final long timeout;

        public DefaultStatefulTimeoutUpdateHandler(final long timeout) {
            this.timeout = timeout;
        }

        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            final ServiceRegistry serviceRegistry = context.getServiceRegistry(true);
            ServiceController<DefaultAccessTimeoutService> defaultAccessTimeoutProviderServiceController = (ServiceController<DefaultAccessTimeoutService>) serviceRegistry.getService(serviceName);
            defaultAccessTimeoutProviderServiceController.getValue().setDefaultAccessTimeout(timeout);
            // complete the step
            context.completeStep();
        }
    }

}
