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

package org.jboss.as.jpa.hibernate4.management;

import org.hibernate.stat.SecondLevelCacheStatistics;
import org.jboss.as.controller.AbstractRuntimeOnlyHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;

/**
 * Handles reads of second level cache metrics.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public abstract class SecondLevelCacheMetricsHandler extends AbstractRuntimeOnlyHandler {

    @Override
    protected void executeRuntimeStep(OperationContext context, ModelNode operation) throws OperationFailedException {
        SecondLevelCacheStatistics statistics = getSecondLevelCacheStatistics(context, operation);
        if (statistics != null) {
            handle(statistics, context, operation.require(ModelDescriptionConstants.NAME).asString());
        }
        context.completeStep();
    }

    protected abstract void handle(SecondLevelCacheStatistics statistics, OperationContext context, String attributeName);

    private SecondLevelCacheStatistics getSecondLevelCacheStatistics(OperationContext context, ModelNode operation) {
        //TODO implement getSecondLevelCacheStatistics
        throw new UnsupportedOperationException();
    }

    static final SecondLevelCacheMetricsHandler HIT_COUNT = new SecondLevelCacheMetricsHandler() {
        @Override
        protected void handle(SecondLevelCacheStatistics statistics, OperationContext context, String attributeName) {
            long count = statistics.getHitCount();
            context.getResult().set(count);
        }
    };

    static final SecondLevelCacheMetricsHandler MISS_COUNT = new SecondLevelCacheMetricsHandler() {
        @Override
        protected void handle(SecondLevelCacheStatistics statistics, OperationContext context, String attributeName) {
            long count = statistics.getMissCount();
            context.getResult().set(count);
        }
    };

    static final SecondLevelCacheMetricsHandler PUT_COUNT = new SecondLevelCacheMetricsHandler() {
        @Override
        protected void handle(SecondLevelCacheStatistics statistics, OperationContext context, String attributeName) {
            long count = statistics.getPutCount();
            context.getResult().set(count);
        }
    };

    static final SecondLevelCacheMetricsHandler ELEMENT_COUNT_IN_MEMORY = new SecondLevelCacheMetricsHandler() {
        @Override
        protected void handle(SecondLevelCacheStatistics statistics, OperationContext context, String attributeName) {
            long count = statistics.getElementCountInMemory();
            context.getResult().set(count);
        }
    };

    static final SecondLevelCacheMetricsHandler ELEMENT_COUNT_ON_DISK = new SecondLevelCacheMetricsHandler() {
        @Override
        protected void handle(SecondLevelCacheStatistics statistics, OperationContext context, String attributeName) {
            long count = statistics.getElementCountOnDisk();
            context.getResult().set(count);
        }
    };

    static final SecondLevelCacheMetricsHandler SIZE_IN_MEMORY = new SecondLevelCacheMetricsHandler() {
        @Override
        protected void handle(SecondLevelCacheStatistics statistics, OperationContext context, String attributeName) {
            long size = statistics.getSizeInMemory();
            context.getResult().set(size);
        }
    };


}
