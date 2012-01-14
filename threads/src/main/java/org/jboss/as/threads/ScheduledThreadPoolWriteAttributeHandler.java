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

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;


/**
 * Handles attribute writes for a scheduled thread pool.
 *
 * @author Alexey Loubyansky
 */
public class ScheduledThreadPoolWriteAttributeHandler extends ThreadsWriteAttributeOperationHandler {

    public static final ScheduledThreadPoolWriteAttributeHandler INSTANCE = new ScheduledThreadPoolWriteAttributeHandler();

    private ScheduledThreadPoolWriteAttributeHandler() {
        super(ScheduledThreadPoolAdd.ATTRIBUTES, ScheduledThreadPoolAdd.RW_ATTRIBUTES);
    }

    @Override
    protected void applyOperation(final OperationContext context, ModelNode operation, String attributeName, ServiceController<?> service) {

        throw new IllegalArgumentException("Unexpected attribute '" + attributeName + "'");
        //final UnboundedQueueThreadPoolService pool =  (UnboundedQueueThreadPoolService) service.getService();
    }

/*    protected int getScaledCount(String attributeName, final ModelNode value) {
        if (!value.hasDefined(COUNT)) {
            throw new IllegalArgumentException("Missing '" + COUNT + "' for '" + attributeName + "'");
        }
        if (!value.hasDefined(PER_CPU)) {
            throw new IllegalArgumentException("Missing '" + PER_CPU + "' for '" + attributeName + "'");
        }

        final BigDecimal count;
        try {
            count = value.get(COUNT).asBigDecimal();
        } catch(NumberFormatException e) {
            throw new IllegalArgumentException("Failed to parse '" + COUNT + "' as java.math.BigDecimal", e);
        }
        final BigDecimal perCpu;
        try {
            perCpu = value.get(PER_CPU).asBigDecimal();
        } catch(NumberFormatException e) {
            throw new IllegalArgumentException("Failed to parse '" + PER_CPU + "' as java.math.BigDecimal", e);
        }

        return new ScaledCount(count, perCpu).getScaledCount();
    }
*/
    @Override
    protected ServiceController<?> getService(final OperationContext context, final ModelNode model) throws OperationFailedException {
        final String name = Util.getNameFromAddress(model.require(OP_ADDR));
        final ServiceName serviceName = ThreadsServices.executorName(name);
        ServiceController<?> controller = context.getServiceRegistry(true).getService(serviceName);
        if(controller == null) {
            throw new OperationFailedException(new ModelNode().set("Service " + serviceName + " not found."));
        }
        return controller;
    }
}
