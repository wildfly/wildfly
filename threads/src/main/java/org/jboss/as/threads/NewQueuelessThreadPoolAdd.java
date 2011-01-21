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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADDRESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.threads.Constants.BLOCKING;
import static org.jboss.as.threads.Constants.HANDOFF_EXECUTOR;
import static org.jboss.as.threads.Constants.KEEPALIVE_TIME_DURATION;
import static org.jboss.as.threads.Constants.KEEPALIVE_TIME_UNIT;
import static org.jboss.as.threads.Constants.MAX_THREADS_COUNT;
import static org.jboss.as.threads.Constants.MAX_THREADS_PER_CPU;
import static org.jboss.as.threads.Constants.PROPERTIES;
import static org.jboss.as.threads.Constants.THREAD_FACTORY;

import java.util.concurrent.ExecutorService;

import org.jboss.as.controller.Cancellable;
import org.jboss.as.controller.ModelAddOperationHandler;
import org.jboss.as.controller.NewOperationContext;
import org.jboss.as.controller.OperationHandler;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.server.NewRuntimeOperationContext;
import org.jboss.as.server.RuntimeOperationHandler;
import org.jboss.as.threads.NewThreadsSubsystemThreadPoolOperationUtils.QueuelessOperationParameters;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class NewQueuelessThreadPoolAdd implements RuntimeOperationHandler, ModelAddOperationHandler {

    static final OperationHandler INSTANCE = new NewQueuelessThreadPoolAdd();

    @Override
    public Cancellable execute(final NewOperationContext context, final ModelNode operation, final ResultHandler resultHandler) {
        QueuelessOperationParameters params = NewThreadsSubsystemThreadPoolOperationUtils.parseQueuelessThreadPoolOperationParameters(operation);

        if (context instanceof NewRuntimeOperationContext) {
            ServiceTarget target = ((NewRuntimeOperationContext)context).getServiceTarget();
            final ServiceName serviceName = ThreadsServices.executorName(params.getName());
            final QueuelessThreadPoolService service = new QueuelessThreadPoolService(params.getMaxThreads().getScaledCount(), params.isBlocking(), params.getKeepAliveTime());

            //TODO add the handoffExceutor injection

            final ServiceBuilder<ExecutorService> serviceBuilder = target.addService(serviceName, service);
            NewThreadsSubsystemThreadPoolOperationUtils.addThreadFactoryDependency(params.getThreadFactory(), serviceName, serviceBuilder, service.getThreadFactoryInjector(), target);
            serviceBuilder.install();

        }

        //Apply to the model
        final ModelNode model = context.getSubModel();
        model.get(NAME).set(params.getName());
        if (params.getThreadFactory() != null) {
            model.get(THREAD_FACTORY).set(params.getThreadFactory());
        }
        if (params.getProperties() != null && params.getProperties().asList().size() > 0) {
            model.get(PROPERTIES).set(params.getProperties());
        }
        if (params.getMaxThreads() != null) {
            model.get(MAX_THREADS_COUNT).set(params.getMaxThreads().getCount());
            model.get(MAX_THREADS_PER_CPU).set(params.getMaxThreads().getPerCpu());
        }
        if (params.getKeepAliveTime() != null) {
            model.get(KEEPALIVE_TIME_DURATION).set(params.getKeepAliveTime().getDuration());
            model.get(KEEPALIVE_TIME_UNIT).set(params.getKeepAliveTime().getUnit().toString());
        }
        model.get(BLOCKING).set(params.isBlocking());
        if (params.getHandoffExecutor() != null) {
            model.get(HANDOFF_EXECUTOR).set(params.getHandoffExecutor());
        }

        // Compensating is remove
        final ModelNode compensating = new ModelNode();
        compensating.get(OP_ADDR).set(operation.require(ADDRESS));
        compensating.get(OP).set(REMOVE);
        resultHandler.handleResultComplete(compensating);

        return Cancellable.NULL;
    }
}
