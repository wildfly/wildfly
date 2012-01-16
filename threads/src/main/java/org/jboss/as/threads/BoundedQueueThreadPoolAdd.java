/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.util.List;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.threads.ThreadsSubsystemThreadPoolOperationUtils.BoundedThreadPoolParameters;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

/**
 * Adds a bounded queue thread pool.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @author <a href="alex@jboss.org">Alexey Loubyansky</a>
 */
public class BoundedQueueThreadPoolAdd extends AbstractAddStepHandler {

    static final AttributeDefinition[] BLOCKING_ATTRIBUTES = new AttributeDefinition[] {PoolAttributeDefinitions.KEEPALIVE_TIME,
        PoolAttributeDefinitions.MAX_THREADS, PoolAttributeDefinitions.THREAD_FACTORY,
        PoolAttributeDefinitions.CORE_THREADS, PoolAttributeDefinitions.QUEUE_LENGTH,
        PoolAttributeDefinitions.ALLOW_CORE_TIMEOUT};

    static final AttributeDefinition[] NON_BLOCKING_ATTRIBUTES = new AttributeDefinition[BLOCKING_ATTRIBUTES.length + 1] ;

    static final AttributeDefinition[] RW_ATTRIBUTES = new AttributeDefinition[] {PoolAttributeDefinitions.KEEPALIVE_TIME,
        PoolAttributeDefinitions.MAX_THREADS, PoolAttributeDefinitions.CORE_THREADS, PoolAttributeDefinitions.QUEUE_LENGTH,
        PoolAttributeDefinitions.ALLOW_CORE_TIMEOUT};

    static {
        System.arraycopy(BLOCKING_ATTRIBUTES, 0, NON_BLOCKING_ATTRIBUTES, 0, BLOCKING_ATTRIBUTES.length);
        NON_BLOCKING_ATTRIBUTES[NON_BLOCKING_ATTRIBUTES.length - 1] = PoolAttributeDefinitions.HANDOFF_EXECUTOR;
    }

    private final boolean blocking;
    private final DefaultThreadFactoryProvider defaultThreadFactoryProvider;
    private final ServiceName serviceNameBase;

    public BoundedQueueThreadPoolAdd(boolean blocking, DefaultThreadFactoryProvider defaultThreadFactoryProvider, ServiceName serviceNameBase) {
        this.blocking = blocking;
        this.defaultThreadFactoryProvider = defaultThreadFactoryProvider;
        this.serviceNameBase = serviceNameBase;
    }

    @Override
    protected void populateModel(final ModelNode operation, final ModelNode model) throws OperationFailedException {
        final PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
        final String name = address.getLastElement().getValue();
        model.get(NAME).set(name);

        AttributeDefinition[] attributes = blocking ? BLOCKING_ATTRIBUTES : NON_BLOCKING_ATTRIBUTES;
        for(final AttributeDefinition attribute : attributes) {
            attribute.validateAndSet(operation, model);
        }
    }

    @Override
    protected void performRuntime(final OperationContext context, final ModelNode operation, final ModelNode model,
            final ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> newControllers) throws OperationFailedException {

        final BoundedThreadPoolParameters params = ThreadsSubsystemThreadPoolOperationUtils.parseBoundedThreadPoolParameters(context, operation, model, blocking);

        final BoundedQueueThreadPoolService service = new BoundedQueueThreadPoolService(
                params.getCoreThreads(),
                params.getMaxThreads(),
                params.getQueueLength(),
                blocking,
                params.getKeepAliveTime(),
                params.isAllowCoreTimeout());

        ThreadsSubsystemThreadPoolOperationUtils.installThreadPoolService(service, params.getName(), serviceNameBase, params.getThreadFactory(),
                defaultThreadFactoryProvider, service.getThreadFactoryInjector(), service.getHandoffExecutorInjector(),
                params.getHandoffExecutor(), context.getServiceTarget(), newControllers, verificationHandler);
    }

    ServiceName getServiceNameBase() {
        return serviceNameBase;
    }
}
