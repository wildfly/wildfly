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

import java.util.concurrent.TimeUnit;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;


/**
 * Handles attribute writes for a bounded queue thread pool.
 * @author Alexey Loubyansky
 */
public class BoundedQueueThreadPoolWriteAttributeHandler extends ThreadsWriteAttributeOperationHandler {

    private final ServiceName serviceNameBase;

    public  BoundedQueueThreadPoolWriteAttributeHandler(boolean blocking, ServiceName serviceNameBase) {
        super(blocking ? BoundedQueueThreadPoolAdd.BLOCKING_ATTRIBUTES : BoundedQueueThreadPoolAdd.NON_BLOCKING_ATTRIBUTES,
                BoundedQueueThreadPoolAdd.RW_ATTRIBUTES);
        this.serviceNameBase = serviceNameBase;
    }

    @Override
    protected void applyOperation(final OperationContext context, ModelNode model, String attributeName,
                                  ServiceController<?> service, boolean forRollback) throws OperationFailedException {

        final BoundedQueueThreadPoolService pool =  (BoundedQueueThreadPoolService) service.getService();

        if (PoolAttributeDefinitions.KEEPALIVE_TIME.getName().equals(attributeName)) {
            TimeUnit defaultUnit = pool.getKeepAliveUnit();
            final TimeSpec spec = getTimeSpec(context, model, defaultUnit);
            pool.setKeepAlive(spec);
        } else if(PoolAttributeDefinitions.MAX_THREADS.getName().equals(attributeName)) {
            pool.setMaxThreads(PoolAttributeDefinitions.MAX_THREADS.resolveModelAttribute(context, model).asInt());
        } else if(PoolAttributeDefinitions.CORE_THREADS.getName().equals(attributeName)) {
            pool.setCoreThreads(PoolAttributeDefinitions.CORE_THREADS.resolveModelAttribute(context, model).asInt());
        } else if(PoolAttributeDefinitions.QUEUE_LENGTH.getName().equals(attributeName)) {
            if (forRollback) {
                context.revertReloadRequired();
            } else {
                context.reloadRequired();
            }
        } else if (PoolAttributeDefinitions.ALLOW_CORE_TIMEOUT.getName().equals(attributeName)) {
            pool.setAllowCoreTimeout(PoolAttributeDefinitions.ALLOW_CORE_TIMEOUT.resolveModelAttribute(context, model).asBoolean());
        } else if (!forRollback) {
            // Programming bug. Throw a RuntimeException, not OFE, as this is not a client error
            throw ThreadsMessages.MESSAGES.unsupportedBoundedQueueThreadPoolAttribute(attributeName);
        }
    }

    @Override
    protected ServiceController<?> getService(final OperationContext context, final ModelNode model) throws OperationFailedException {
        final String name = Util.getNameFromAddress(model.require(OP_ADDR));
        final ServiceName serviceName = serviceNameBase.append(name);
        ServiceController<?> controller = context.getServiceRegistry(true).getService(serviceName);
        if(controller == null) {
            throw ThreadsMessages.MESSAGES.boundedQueueThreadPoolServiceNotFound(serviceName);
        }
        return controller;
    }
}
