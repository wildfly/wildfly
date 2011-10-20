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

import java.util.List;
import java.util.Locale;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import org.jboss.as.threads.ThreadsSubsystemThreadPoolOperationUtils.BaseOperationParameters;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;

/**
 * Adds a scheduled thread pool.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class ScheduledThreadPoolAdd extends AbstractAddStepHandler implements DescriptionProvider {

    static final ScheduledThreadPoolAdd INSTANCE = new ScheduledThreadPoolAdd();

    static final AttributeDefinition[] ATTRIBUTES = new AttributeDefinition[] {PoolAttributeDefinitions.KEEPALIVE_TIME,
        PoolAttributeDefinitions.MAX_THREADS, PoolAttributeDefinitions.PROPERTIES, PoolAttributeDefinitions.THREAD_FACTORY};

    static final AttributeDefinition[] RW_ATTRIBUTES = new AttributeDefinition[]{};

    @Override
    public ModelNode getModelDescription(Locale locale) {
        return ThreadsSubsystemProviders.ADD_SCHEDULED_THREAD_POOL_DESC.getModelDescription(locale);
    }

    @Override
    protected void populateModel(final ModelNode operation, final ModelNode model) throws OperationFailedException {
        final PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
        final String name = address.getLastElement().getValue();
        model.get(NAME).set(name);

        for(final AttributeDefinition attribute : ATTRIBUTES) {
            attribute.validateAndSet(operation, model);
        }
    }

    @Override
    protected void performRuntime(final OperationContext context, final ModelNode operation, final ModelNode model,
            final ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> newControllers) throws OperationFailedException {

        for(final AttributeDefinition attribute : ATTRIBUTES) {
            attribute.validateResolvedOperation(model);
        }

        final BaseOperationParameters params = ThreadsSubsystemThreadPoolOperationUtils.parseScheduledThreadPoolOperationParameters(operation);

        ServiceTarget target = context.getServiceTarget();
        final ServiceName serviceName = ThreadsServices.executorName(params.getName());
        final ScheduledThreadPoolService service = new ScheduledThreadPoolService(params.getMaxThreads().getScaledCount(), params.getKeepAliveTime());
        final ServiceBuilder<ManagedScheduledExecutorService> serviceBuilder = target.addService(serviceName, service);
        ThreadsSubsystemThreadPoolOperationUtils.addThreadFactoryDependency(params.getThreadFactory(), serviceName, serviceBuilder, service.getThreadFactoryInjector(), target, params.getName() + "-threads");
        serviceBuilder.addListener(verificationHandler);
        serviceBuilder.install();

    }
}
