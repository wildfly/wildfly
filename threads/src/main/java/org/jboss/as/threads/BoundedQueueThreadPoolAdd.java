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

import org.jboss.as.controller.BasicOperationResult;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationResult;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import org.jboss.as.server.RuntimeTask;
import org.jboss.as.server.RuntimeTaskContext;
import static org.jboss.as.threads.CommonAttributes.ALLOW_CORE_TIMEOUT;
import static org.jboss.as.threads.CommonAttributes.BLOCKING;
import static org.jboss.as.threads.CommonAttributes.CORE_THREADS;
import static org.jboss.as.threads.CommonAttributes.HANDOFF_EXECUTOR;
import static org.jboss.as.threads.CommonAttributes.KEEPALIVE_TIME;
import static org.jboss.as.threads.CommonAttributes.MAX_THREADS;
import static org.jboss.as.threads.CommonAttributes.PROPERTIES;
import static org.jboss.as.threads.CommonAttributes.QUEUE_LENGTH;
import static org.jboss.as.threads.CommonAttributes.THREAD_FACTORY;

import java.util.concurrent.Executor;

import org.jboss.as.controller.ModelAddOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationHandler;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.server.RuntimeOperationContext;
import org.jboss.as.server.RuntimeOperationHandler;
import org.jboss.as.threads.ThreadsSubsystemThreadPoolOperationUtils.BoundedOperationParameters;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;

/**
 * Adds a bounded queue thread pool.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class BoundedQueueThreadPoolAdd implements RuntimeOperationHandler, ModelAddOperationHandler {

    static final OperationHandler INSTANCE = new BoundedQueueThreadPoolAdd();

    @Override
    public OperationResult execute(final OperationContext context, final ModelNode operation, final ResultHandler resultHandler) {
        final BoundedOperationParameters params = ThreadsSubsystemThreadPoolOperationUtils.parseBoundedThreadPoolOperationParameters(operation);

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
            model.get(MAX_THREADS).set(operation.get(MAX_THREADS));
        }
        if (params.getKeepAliveTime() != null) {
            model.get(KEEPALIVE_TIME).set(operation.get(KEEPALIVE_TIME));
        }

        model.get(BLOCKING).set(params.isBlocking());
        if (params.getHandoffExecutor() != null) {
            model.get(HANDOFF_EXECUTOR).set(params.getHandoffExecutor());
        }
        model.get(ALLOW_CORE_TIMEOUT).set(params.isAllowCoreTimeout());
        if (params.getQueueLength() != null) {
            model.get(QUEUE_LENGTH).set(operation.get(QUEUE_LENGTH));
        }

        if (params.getCoreThreads() != null) {
            model.get(CORE_THREADS).set(operation.get(CORE_THREADS));
        }

        // Compensating is remove
        final ModelNode compensating = Util.getResourceRemoveOperation(params.getAddress());

        if (context instanceof RuntimeOperationContext) {
            RuntimeOperationContext.class.cast(context).executeRuntimeTask(new RuntimeTask() {
                public void execute(RuntimeTaskContext context, final ResultHandler resultHandler) throws OperationFailedException {

                    ServiceTarget target = context.getServiceTarget();
                    final ServiceName serviceName = ThreadsServices.executorName(params.getName());
                    final BoundedQueueThreadPoolService service = new BoundedQueueThreadPoolService(
                            params.getCoreThreads().getScaledCount(),
                            params.getMaxThreads().getScaledCount(),
                            params.getQueueLength().getScaledCount(),
                            params.isBlocking(),
                            params.getKeepAliveTime(),
                            params.isAllowCoreTimeout());

                    //TODO add the handoffExceutor injection

                    final ServiceBuilder<Executor> serviceBuilder = target.addService(serviceName, service);
                    ThreadsSubsystemThreadPoolOperationUtils.addThreadFactoryDependency(params.getThreadFactory(), serviceName, serviceBuilder, service.getThreadFactoryInjector(), target);
                    serviceBuilder.addListener(new ResultHandler.ServiceStartListener(resultHandler));
                    serviceBuilder.install();
                }
            }, resultHandler);
        } else {
            resultHandler.handleResultComplete();
        }
        return new BasicOperationResult(compensating);
    }
}
