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
package org.jboss.as.threads;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.util.List;

import org.jboss.as.controller.AbstractRuntimeOnlyHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.operations.validation.ParametersValidator;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceController;

/**
*
* @author Alexey Loubyansky
*/
public abstract class ThreadPoolReadAttributeHandler extends AbstractRuntimeOnlyHandler {

    private ParametersValidator validator = new ParametersValidator();

    private final List<String> metrics;

    public ThreadPoolReadAttributeHandler(List<String> metrics) {
        super();
        this.metrics = metrics;
    }

    public void registerAttributes(final ManagementResourceRegistration registration) {
        for (String metric : metrics) {
            registration.registerMetric(metric, this);
        }
    }

    @Override
    protected void executeRuntimeStep(OperationContext context, ModelNode operation) throws OperationFailedException {

        validator.validate(operation);
        final String attributeName = operation.require(ModelDescriptionConstants.NAME).asString();

        ServiceController<?> serviceController = getService(context, operation);
        final Service<?> service = serviceController.getService();

        setResult(context, attributeName, service);

        context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
    }

    protected abstract void setResult(OperationContext context, String attributeName, Service<?> service) throws OperationFailedException;

    protected ServiceController<?> getService(final OperationContext context, final ModelNode operation)
            throws OperationFailedException {
                final String name = Util.getNameFromAddress(operation.require(OP_ADDR));
                ServiceController<?> controller = context.getServiceRegistry(true).getService(ThreadsServices.executorName(name));
                if(controller == null) {
                    throw new OperationFailedException(new ModelNode().set("Failed to locate executor service " + ThreadsServices.executorName(name)));
                }
                return controller;
            }

}
