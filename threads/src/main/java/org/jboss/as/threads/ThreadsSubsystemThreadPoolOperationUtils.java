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
import static org.jboss.as.threads.CommonAttributes.KEEPALIVE_TIME;
import static org.jboss.as.threads.CommonAttributes.TIME;
import static org.jboss.as.threads.CommonAttributes.UNIT;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceListener;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;

/**
 * Utilities related to converting detyped thread pool config ModelNodes to typed config objects.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
class ThreadsSubsystemThreadPoolOperationUtils {

    static <T> void installThreadPoolService(final Service<T> threadPoolService,
                                               final String threadPoolName, final String threadFactoryName,
                                               final DefaultThreadFactoryProvider defaultThreadFactoryProvider,
                                               final Injector<ThreadFactory> threadFactoryInjector,
                                               final Injector<Executor> handoffExecutorInjector,
                                               final String handoffExecutorName,
                                               final ServiceTarget target, final List<ServiceController<?>> newControllers,
                                               final ServiceListener<Object>... newServiceListeners) {

        final ServiceName serviceName = ThreadsServices.executorName(threadPoolName);

        final ServiceBuilder<?> serviceBuilder = target.addService(serviceName, threadPoolService);

        addThreadFactoryDependency(threadFactoryName, threadPoolName, serviceName,
                defaultThreadFactoryProvider, serviceBuilder, threadFactoryInjector, target, newControllers, newServiceListeners);

        if (handoffExecutorInjector != null && handoffExecutorName != null) {
            serviceBuilder.addDependency(ThreadsServices.executorName(handoffExecutorName), Executor.class, handoffExecutorInjector);
        }

        if (newServiceListeners != null  && newServiceListeners.length > 0) {
            serviceBuilder.addListener(newServiceListeners);
        }
        ServiceController<?> sc = serviceBuilder.install();
        if (newControllers != null) {
            newControllers.add(sc);
        }

    }

    private static void addThreadFactoryDependency(final String threadFactory, final String threadPoolName,
                                               final ServiceName threadPoolServiceName,
                                               final DefaultThreadFactoryProvider defaultThreadFactoryProvider,
                                               final ServiceBuilder<?> serviceBuilder, final Injector<ThreadFactory> injector,
                                               final ServiceTarget target, final List<ServiceController<?>> newControllers,
                                               final ServiceListener<Object>... newServiceListeners) {
        final ServiceName threadFactoryName;
        if (threadFactory == null) {
            threadFactoryName = defaultThreadFactoryProvider.getDefaultThreadFactory(threadPoolName, threadPoolServiceName, target, newControllers, newServiceListeners);
        } else {
            threadFactoryName = ThreadsServices.threadFactoryName(threadFactory);
        }
        serviceBuilder.addDependency(threadFactoryName, ThreadFactory.class, injector);
    }

    static BaseThreadPoolParameters parseUnboundedQueueThreadPoolParameters(final OperationContext context, final ModelNode operation, final ModelNode model) throws OperationFailedException {
        ThreadPoolParametersImpl params = new ThreadPoolParametersImpl();
        return parseBaseThreadPoolOperationParameters(context, operation, model, params);
    }

    static BaseThreadPoolParameters parseScheduledThreadPoolParameters(final OperationContext context, final ModelNode operation, final ModelNode model) throws OperationFailedException {
        ThreadPoolParametersImpl params = new ThreadPoolParametersImpl();
        return parseBaseThreadPoolOperationParameters(context, operation, model, params);
    }

    static QueuelessThreadPoolParameters parseQueuelessThreadPoolParameters(final OperationContext context, final ModelNode operation, final ModelNode model, boolean blocking) throws OperationFailedException {
        ThreadPoolParametersImpl params = new ThreadPoolParametersImpl();
        parseBaseThreadPoolOperationParameters(context, operation, model, params);

        if (!blocking) {
            ModelNode handoffEx = PoolAttributeDefinitions.HANDOFF_EXECUTOR.resolveModelAttribute(context, model);
            params.handoffExecutor = handoffEx.isDefined() ? handoffEx.asString() : null;
        }

        return params;
    }

    static BoundedThreadPoolParameters parseBoundedThreadPoolParameters(final OperationContext context, final ModelNode operation, final ModelNode model, boolean blocking) throws OperationFailedException {
        ThreadPoolParametersImpl params = new ThreadPoolParametersImpl();
        parseBaseThreadPoolOperationParameters(context, operation, model, params);

        params.allowCoreTimeout = PoolAttributeDefinitions.ALLOW_CORE_TIMEOUT.resolveModelAttribute(context, model).asBoolean();
        if (!blocking) {
            ModelNode handoffEx = PoolAttributeDefinitions.HANDOFF_EXECUTOR.resolveModelAttribute(context, model);
            params.handoffExecutor = handoffEx.isDefined() ? handoffEx.asString() : null;
        }
        ModelNode coreTh = PoolAttributeDefinitions.CORE_THREADS.resolveModelAttribute(context, model);
        params.coreThreads = coreTh.isDefined() ? coreTh.asInt() : params.maxThreads;
        params.queueLength = PoolAttributeDefinitions.QUEUE_LENGTH.resolveModelAttribute(context, model).asInt();
        return params;
    }


    private static ThreadPoolParametersImpl parseBaseThreadPoolOperationParameters(final OperationContext context, final ModelNode operation,
                                                                                   final ModelNode model, final ThreadPoolParametersImpl params) throws OperationFailedException {
        params.address = operation.require(OP_ADDR);
        PathAddress pathAddress = PathAddress.pathAddress(params.address);
        params.name = pathAddress.getLastElement().getValue();

        //Get/validate the properties
        ModelNode tfNode = PoolAttributeDefinitions.THREAD_FACTORY.resolveModelAttribute(context, model);
        params.threadFactory = tfNode.isDefined() ? tfNode.asString() : null;
        params.maxThreads = PoolAttributeDefinitions.MAX_THREADS.resolveModelAttribute(context, model).asInt();

        if (model.hasDefined(KEEPALIVE_TIME)) {
            ModelNode keepaliveTime = model.get(KEEPALIVE_TIME);
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

    interface BaseThreadPoolParameters {
        ModelNode getAddress();

        String getName();

        String getThreadFactory();

        int getMaxThreads();

        TimeSpec getKeepAliveTime();
    }

    interface QueuelessThreadPoolParameters extends BaseThreadPoolParameters {

        String getHandoffExecutor();
    }

    interface BoundedThreadPoolParameters extends QueuelessThreadPoolParameters {
        boolean isAllowCoreTimeout();
        int getCoreThreads();
        int getQueueLength();
    }

    private static class ThreadPoolParametersImpl implements QueuelessThreadPoolParameters, BoundedThreadPoolParameters {
        ModelNode address;
        String name;
        String threadFactory;
        int maxThreads;
        TimeSpec keepAliveTime;
        String handoffExecutor;
        boolean allowCoreTimeout;
        int coreThreads;
        int queueLength;

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
        public int getMaxThreads() {
            return maxThreads;
        }

        @Override
        public TimeSpec getKeepAliveTime() {
            return keepAliveTime;
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
        public int getCoreThreads() {
            return coreThreads;
        }

        @Override
        public int getQueueLength() {
            return queueLength;
        }
    }

}
