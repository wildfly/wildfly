/*
* JBoss, Home of Professional Open Source.
* Copyright 2006, Red Hat Middleware LLC, and individual contributors
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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.threads.Constants.ALLOW_CORE_TIMEOUT;
import static org.jboss.as.threads.Constants.BLOCKING;
import static org.jboss.as.threads.Constants.CORE_THREADS_COUNT;
import static org.jboss.as.threads.Constants.CORE_THREADS_PER_CPU;
import static org.jboss.as.threads.Constants.HANDOFF_EXECUTOR;
import static org.jboss.as.threads.Constants.KEEPALIVE_TIME_DURATION;
import static org.jboss.as.threads.Constants.KEEPALIVE_TIME_UNIT;
import static org.jboss.as.threads.Constants.MAX_THREADS_COUNT;
import static org.jboss.as.threads.Constants.MAX_THREADS_PER_CPU;
import static org.jboss.as.threads.Constants.PROPERTIES;
import static org.jboss.as.threads.Constants.QUEUE_LENGTH_COUNT;
import static org.jboss.as.threads.Constants.QUEUE_LENGTH_PER_CPU;
import static org.jboss.as.threads.Constants.THREAD_FACTORY;

import java.math.BigDecimal;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
class NewThreadsSubsystemThreadPoolOperationUtils {

    static <T> void addThreadFactoryDependency(final String threadFactory, final ServiceName serviceName, ServiceBuilder<T> serviceBuilder, Injector<ThreadFactory> injector, ServiceTarget target) {
        final ServiceName threadFactoryName;
        if (threadFactory == null) {
            threadFactoryName = serviceName.append("thread-factory");
            target.addService(threadFactoryName, new ThreadFactoryService())
                .install();
        } else {
            threadFactoryName = ThreadsServices.threadFactoryName(threadFactory);
        }
        serviceBuilder.addDependency(threadFactoryName, ThreadFactory.class, injector);
    }

    static BaseOperationParameters parseUnboundedQueueThreadPoolOperationParameters(ModelNode operation) {
        OperationParametersImpl params = new OperationParametersImpl();
        return parseBaseThreadPoolOperationParameters(operation, params);
    }

    static BaseOperationParameters parseScheduledThreadPoolOperationParameters(ModelNode operation) {
        OperationParametersImpl params = new OperationParametersImpl();
        return parseBaseThreadPoolOperationParameters(operation, params);
    }

    static QueuelessOperationParameters parseQueuelessThreadPoolOperationParameters(ModelNode operation) {
        OperationParametersImpl params = new OperationParametersImpl();
        parseBaseThreadPoolOperationParameters(operation, params);

        params.blocking = has(operation, BLOCKING) ? operation.get(BLOCKING).asBoolean() : false;
        params.handoffExecutor = has(operation, HANDOFF_EXECUTOR) ? operation.get(HANDOFF_EXECUTOR).asString() : null;

        return params;
    }

    static BoundedOperationParameters parseBoundedThreadPoolOperationParameters(ModelNode operation) {
        OperationParametersImpl params = new OperationParametersImpl();
        parseBaseThreadPoolOperationParameters(operation, params);

        params.blocking = has(operation, BLOCKING) ? operation.get(BLOCKING).asBoolean() : false;
        params.allowCoreTimeout = has(operation, ALLOW_CORE_TIMEOUT) ? operation.get(ALLOW_CORE_TIMEOUT).asBoolean() : false;
        params.handoffExecutor = has(operation, HANDOFF_EXECUTOR) ? operation.get(HANDOFF_EXECUTOR).asString() : null;
        params.coreThreads = getScaledCount(operation, CORE_THREADS_COUNT, CORE_THREADS_PER_CPU);
        params.queueLength = getScaledCount(operation, QUEUE_LENGTH_COUNT, QUEUE_LENGTH_PER_CPU);

        return params;
    }


    private static OperationParametersImpl parseBaseThreadPoolOperationParameters(ModelNode operation, OperationParametersImpl params) {
        //Get/validate the properties
        params.name = operation.require(NAME).asString();
        params.threadFactory = has(operation, THREAD_FACTORY) ? operation.get(THREAD_FACTORY).asString() : null;
        params.properties = has(operation, PROPERTIES) ? operation.get(PROPERTIES) : null;
        if (params.properties != null) {
            if (params.properties.getType() != ModelType.LIST) {
                throw new IllegalArgumentException(PROPERTIES + " must be a list of properties"); //TODO i18n
            }
            for (ModelNode property : params.properties.asList()) {
                if (property.getType() != ModelType.PROPERTY) {
                    throw new IllegalArgumentException(PROPERTIES + " must be a list of properties"); //TODO i18n
                }
            }
        }
        params.maxThreads = getScaledCount(operation, MAX_THREADS_COUNT, MAX_THREADS_PER_CPU);

        final long duration = has(operation, KEEPALIVE_TIME_DURATION) ? operation.get(KEEPALIVE_TIME_DURATION).asLong() : -1;
        final TimeUnit unit = has(operation, KEEPALIVE_TIME_UNIT) ? Enum.valueOf(TimeUnit.class, operation.get(KEEPALIVE_TIME_UNIT).asString()) : null;
        if (duration == - 1 && unit != null) {
            throw new IllegalArgumentException("Need " + KEEPALIVE_TIME_DURATION + " when " + KEEPALIVE_TIME_UNIT + " is set"); //TODO i18n
        }
        if (duration != - 1 && unit == null) {
            throw new IllegalArgumentException("Need " + KEEPALIVE_TIME_UNIT + " when " + KEEPALIVE_TIME_DURATION + " is set"); //TODO i18n
        }
        params.keepAliveTime = unit != null ? new TimeSpec(unit, duration) : null;

        return params;
    }

    private static ScaledCount getScaledCount(ModelNode operation, String count, String perCpu) {
        final BigDecimal maxThreadsCount = has(operation, count) ? operation.get(count).asBigDecimal() : null;
        final BigDecimal maxThreadsPerCpu = has(operation, perCpu) ? operation.get(perCpu).asBigDecimal() : null;
        if (maxThreadsCount != null && maxThreadsPerCpu == null) {
            throw new IllegalArgumentException("Need " + perCpu + " when " + count + " is set"); //TODO i18n
        }
        if (maxThreadsCount == null && maxThreadsPerCpu != null) {
            throw new IllegalArgumentException("Need " + count + " when " + perCpu + " is set"); //TODO i18n
        }
        return maxThreadsCount != null ? new ScaledCount(maxThreadsCount, maxThreadsPerCpu) : null;
    }

    private static boolean has(ModelNode operation, String name) {
        return operation.has(name) && operation.get(name).isDefined();
    }

    interface BaseOperationParameters {
        String getName();

        String getThreadFactory();

        ModelNode getProperties();

        ScaledCount getMaxThreads();

        TimeSpec getKeepAliveTime();
    }

    interface QueuelessOperationParameters extends BaseOperationParameters {
        boolean isBlocking();

        String getHandoffExecutor();
    }

    interface BoundedOperationParameters extends QueuelessOperationParameters {
        boolean isAllowCoreTimeout();
        ScaledCount getCoreThreads();
        ScaledCount getQueueLength();
    }

    private static class OperationParametersImpl implements QueuelessOperationParameters, BoundedOperationParameters {
        String name;
        String threadFactory;
        ModelNode properties;
        ScaledCount maxThreads;
        TimeSpec keepAliveTime;
        boolean blocking;
        String handoffExecutor;
        boolean allowCoreTimeout;
        ScaledCount coreThreads;
        ScaledCount queueLength;

        public String getName() {
            return name;
        }

        public String getThreadFactory() {
            return threadFactory;
        }

        public ModelNode getProperties() {
            return properties;
        }

        public ScaledCount getMaxThreads() {
            return maxThreads;
        }

        public TimeSpec getKeepAliveTime() {
            return keepAliveTime;
        }

        public boolean isBlocking() {
            return blocking;
        }

        @Override
        public String getHandoffExecutor() {
            return handoffExecutor;
        }

        @Override
        public boolean isAllowCoreTimeout() {
            return allowCoreTimeout;
        }

        @Override
        public ScaledCount getCoreThreads() {
            return coreThreads;
        }

        @Override
        public ScaledCount getQueueLength() {
            return queueLength;
        }
    }

}
