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
import static org.jboss.as.threads.CommonAttributes.KEEPALIVE_TIME;
import static org.jboss.as.threads.CommonAttributes.TIME;
import static org.jboss.as.threads.CommonAttributes.UNIT;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;


/**
 * Handles attribute writes for a queueless thread pool.
 *
 * @author Alexey Loubyansky
 */
public class QueuelessThreadPoolWriteAttributeHandler extends ThreadsWriteAttributeOperationHandler {

    public static final QueuelessThreadPoolWriteAttributeHandler BLOCKING = new QueuelessThreadPoolWriteAttributeHandler(true);
    public static final QueuelessThreadPoolWriteAttributeHandler NON_BLOCKING = new QueuelessThreadPoolWriteAttributeHandler(false);

    private QueuelessThreadPoolWriteAttributeHandler(boolean blocking) {
        super(blocking ? QueuelessThreadPoolAdd.BLOCKING_ATTRIBUTES : QueuelessThreadPoolAdd.NON_BLOCKING_ATTRIBUTES, QueuelessThreadPoolAdd.RW_ATTRIBUTES);
    }

    @Override
    protected void applyOperation(final OperationContext context, ModelNode model, String attributeName, ServiceController<?> service) throws OperationFailedException {

        final QueuelessThreadPoolService pool =  (QueuelessThreadPoolService) service.getService();

        if (PoolAttributeDefinitions.KEEPALIVE_TIME.getName().equals(attributeName)) {
            ModelNode value = PoolAttributeDefinitions.KEEPALIVE_TIME.resolveModelAttribute(context, model);
            if (!value.hasDefined(TIME)) {
                throw new IllegalArgumentException("Missing '" + TIME + "' for '" + KEEPALIVE_TIME + "'");
            }
            final TimeUnit unit;
            if (!value.hasDefined(UNIT)) {
                unit = pool.getKeepAliveUnit();
            } else {
                try {
                unit = Enum.valueOf(TimeUnit.class, value.get(UNIT).asString());
                } catch(IllegalArgumentException e) {
                    throw new OperationFailedException(new ModelNode().set("Failed to parse '" + UNIT + "', allowed values are: " + Arrays.asList(TimeUnit.values())));
                }
            }
            final TimeSpec spec = new TimeSpec(unit, value.get(TIME).asLong());
            pool.setKeepAlive(spec);
        } else if(PoolAttributeDefinitions.MAX_THREADS.getName().equals(attributeName)) {
            pool.setMaxThreads(PoolAttributeDefinitions.MAX_THREADS.resolveModelAttribute(context, model).asInt());
        } else {
            throw new IllegalStateException("Unexpected attribute '" + attributeName + "'");
        }
    }

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
