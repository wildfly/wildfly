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


import java.util.Arrays;
import java.util.List;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;


/**
 * Handles metrics for a queueless thread pool.
 * @author Alexey Loubyansky
 */
public class QueuelessThreadPoolMetricsHandler extends ThreadPoolMetricsHandler {

    public static final List<AttributeDefinition> METRICS = Arrays.asList(PoolAttributeDefinitions.CURRENT_THREAD_COUNT, PoolAttributeDefinitions.LARGEST_THREAD_COUNT,
            PoolAttributeDefinitions.REJECTED_COUNT);

    public QueuelessThreadPoolMetricsHandler(final ServiceName serviceNameBase) {
        super(METRICS, serviceNameBase);
    }

    @Override
    protected void setResult(OperationContext context, final String attributeName, final Service<?> service)
            throws OperationFailedException {
        final QueuelessThreadPoolService pool = (QueuelessThreadPoolService) service;
        if(attributeName.equals(CommonAttributes.CURRENT_THREAD_COUNT)) {
            context.getResult().set(pool.getCurrentThreadCount());
        } else if (attributeName.equals(CommonAttributes.LARGEST_THREAD_COUNT)) {
            context.getResult().set(pool.getLargestThreadCount());
        } else if (attributeName.equals(CommonAttributes.REJECTED_COUNT)) {
            context.getResult().set(pool.getRejectedCount());
        } else {
            // Programming bug. Throw a RuntimeException, not OFE, as this is not a client error
            throw ThreadsMessages.MESSAGES.unsupportedQueuelessThreadPoolMetric(attributeName);
        }
    }
}
