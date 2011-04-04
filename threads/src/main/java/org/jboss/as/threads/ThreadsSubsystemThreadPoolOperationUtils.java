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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.threads.CommonAttributes.ALLOW_CORE_TIMEOUT;
import static org.jboss.as.threads.CommonAttributes.BLOCKING;
import static org.jboss.as.threads.CommonAttributes.CORE_THREADS;
import static org.jboss.as.threads.CommonAttributes.COUNT;
import static org.jboss.as.threads.CommonAttributes.HANDOFF_EXECUTOR;
import static org.jboss.as.threads.CommonAttributes.KEEPALIVE_TIME;
import static org.jboss.as.threads.CommonAttributes.MAX_THREADS;
import static org.jboss.as.threads.CommonAttributes.PER_CPU;
import static org.jboss.as.threads.CommonAttributes.PROPERTIES;
import static org.jboss.as.threads.CommonAttributes.QUEUE_LENGTH;
import static org.jboss.as.threads.CommonAttributes.THREAD_FACTORY;
import static org.jboss.as.threads.CommonAttributes.TIME;
import static org.jboss.as.threads.CommonAttributes.UNIT;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.jboss.as.controller.PathAddress;
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
class ThreadsSubsystemThreadPoolOperationUtils {

    static <T> void addThreadFactoryDependency(final String threadFactory, final ServiceName serviceName, ServiceBuilder<T> serviceBuilder, Injector<ThreadFactory> injector, ServiceTarget target, String defaultThreadGroupName) {
        final ServiceName threadFactoryName;
        if (threadFactory == null) {
            threadFactoryName = serviceName.append("thread-factory");
            final ThreadFactoryService service = new ThreadFactoryService();
            service.setThreadGroupName(defaultThreadGroupName);
            service.setNamePattern("%G - %t");
            target.addService(threadFactoryName, service)
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

        params.blocking = operation.hasDefined(BLOCKING) ? operation.get(BLOCKING).asBoolean() : false;
        params.handoffExecutor = operation.hasDefined(HANDOFF_EXECUTOR) ? operation.get(HANDOFF_EXECUTOR).asString() : null;

        return params;
    }

    static BoundedOperationParameters parseBoundedThreadPoolOperationParameters(ModelNode operation) {
        OperationParametersImpl params = new OperationParametersImpl();
        parseBaseThreadPoolOperationParameters(operation, params);

        params.blocking = operation.hasDefined(BLOCKING) ? operation.get(BLOCKING).asBoolean() : false;
        params.allowCoreTimeout = operation.hasDefined(ALLOW_CORE_TIMEOUT) ? operation.get(ALLOW_CORE_TIMEOUT).asBoolean() : false;
        params.handoffExecutor = operation.hasDefined(HANDOFF_EXECUTOR) ? operation.get(HANDOFF_EXECUTOR).asString() : null;
        params.coreThreads = getScaledCount(operation, CORE_THREADS);
        params.queueLength = getScaledCount(operation, QUEUE_LENGTH);

        return params;
    }


    private static OperationParametersImpl parseBaseThreadPoolOperationParameters(ModelNode operation, OperationParametersImpl params) {
        params.address = operation.require(OP_ADDR);
        PathAddress pathAddress = PathAddress.pathAddress(params.address);
        params.name = pathAddress.getLastElement().getValue();

        //Get/validate the properties
        params.threadFactory = operation.hasDefined(THREAD_FACTORY) ? operation.get(THREAD_FACTORY).asString() : null;
        params.properties = operation.hasDefined(PROPERTIES) ? operation.get(PROPERTIES) : null;
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
        params.maxThreads = getScaledCount(operation, MAX_THREADS);
        if (params.maxThreads == null) {
            throw new IllegalArgumentException(MAX_THREADS + " was not defined");
        }

        if (operation.hasDefined(KEEPALIVE_TIME)) {
            ModelNode keepaliveTime = operation.get(KEEPALIVE_TIME);
            if (!keepaliveTime.hasDefined(TIME)) {
                throw new IllegalArgumentException("Missing '" + TIME + "' for '" + KEEPALIVE_TIME + "'");
            }
            if (!keepaliveTime.hasDefined(UNIT)) {
                throw new IllegalArgumentException("Missing '" + UNIT + "' for '" + KEEPALIVE_TIME + "'");
            }
            params.keepAliveTime = new TimeSpec(Enum.valueOf(TimeUnit.class, keepaliveTime.get(UNIT).asString()), keepaliveTime.get(TIME).asLong());
        }

        return params;
    }

    private static ScaledCount getScaledCount(ModelNode operation, String paramName) {
        if (operation.hasDefined(paramName)) {
            ModelNode scaledCount = operation.get(paramName);
            if (!scaledCount.hasDefined(COUNT)) {
                throw new IllegalArgumentException("Missing '" + COUNT + "' for '" + paramName + "'");
            }
            if (!scaledCount.hasDefined(PER_CPU)) {
                throw new IllegalArgumentException("Missing '" + PER_CPU + "' for '" + paramName + "'");
            }
            return new ScaledCount(scaledCount.get(COUNT).asBigDecimal(), scaledCount.get(PER_CPU).asBigDecimal());
        }
        return null;
    }

    interface BaseOperationParameters {
        ModelNode getAddress();

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
        ModelNode address;
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

        @Override
        public ModelNode getAddress() {
            return address;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getThreadFactory() {
            return threadFactory;
        }

        @Override
        public ModelNode getProperties() {
            return properties;
        }

        @Override
        public ScaledCount getMaxThreads() {
            return maxThreads;
        }

        @Override
        public TimeSpec getKeepAliveTime() {
            return keepAliveTime;
        }

        @Override
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
