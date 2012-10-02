/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2012, Red Hat, Inc., and individual contributors
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
package org.jboss.as.ejb3.subsystem.deployment;

import org.jboss.as.controller.AbstractRuntimeOnlyHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.ee.component.BasicComponent;
import org.jboss.as.ejb3.component.EJBComponent;
import org.jboss.as.server.deployment.Services;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public abstract class AbstractRuntimeMetricsHandler extends AbstractRuntimeOnlyHandler {
    private static ServiceName componentServiceName(final ModelNode operation) {
        final PathAddress address = PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR));
        final String parent;
        final String module;
        int i = 2;
        if (address.getElement(1).getKey().equals(ModelDescriptionConstants.SUBDEPLOYMENT)) {
            parent = address.getElement(0).getValue();
            module = address.getElement(1).getValue();
            i++;
        } else {
            parent = null;
            module = address.getElement(0).getValue();
        }
        final String component = address.getElement(i).getValue();
        final ServiceName deploymentUnitServiceName;
        if (parent == null) {
            deploymentUnitServiceName = Services.deploymentUnitName(module);
        }
        else {
            deploymentUnitServiceName = Services.deploymentUnitName(parent, module);
        }
        // Hmm, don't like the START bit
        return BasicComponent.serviceNameOf(deploymentUnitServiceName, component).append("START");
    }

    protected abstract void executeReadMetricStep(final OperationContext context, final ModelNode operation, final EJBComponent component) throws OperationFailedException;

    @Override
    protected void executeRuntimeStep(final OperationContext context, final ModelNode operation) throws OperationFailedException {
        final ServiceName componentServiceName = componentServiceName(operation);
        final EJBComponent component = (EJBComponent) context.getServiceRegistry(false).getRequiredService(componentServiceName).getValue();
        executeReadMetricStep(context, operation, component);
        context.stepCompleted();
    }
}
