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

import java.util.Locale;
import java.util.concurrent.Executor;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.threads.CommonAttributes.ALLOW_CORE_TIMEOUT;
import static org.jboss.as.threads.CommonAttributes.BLOCKING;
import static org.jboss.as.threads.CommonAttributes.CORE_THREADS;
import static org.jboss.as.threads.CommonAttributes.HANDOFF_EXECUTOR;
import static org.jboss.as.threads.CommonAttributes.KEEPALIVE_TIME;
import static org.jboss.as.threads.CommonAttributes.MAX_THREADS;
import static org.jboss.as.threads.CommonAttributes.PROPERTIES;
import static org.jboss.as.threads.CommonAttributes.QUEUE_LENGTH;
import static org.jboss.as.threads.CommonAttributes.THREAD_FACTORY;
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
public class BoundedQueueThreadPoolAdd implements OperationStepHandler, DescriptionProvider {

    public static final BoundedQueueThreadPoolAdd INSTANCE = new BoundedQueueThreadPoolAdd();

    /**
     * {@inheritDoc}
     */
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        final BoundedOperationParameters params = ThreadsSubsystemThreadPoolOperationUtils.parseBoundedThreadPoolOperationParameters(operation);

        final PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
        final String name = address.getLastElement().getValue();

        //Apply to the model
        final ModelNode model = context.readModelForUpdate(PathAddress.EMPTY_ADDRESS);
        model.get(NAME).set(name);
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
        } else {
            throw new OperationFailedException(new ModelNode().set("Parameter " + QUEUE_LENGTH + " may not be null "));
        }

        if (params.getCoreThreads() != null) {
            model.get(CORE_THREADS).set(operation.get(CORE_THREADS));
        }

        if (context.getType() == OperationContext.Type.SERVER) {
            context.addStep(new OperationStepHandler() {
                public void execute(OperationContext context, ModelNode operation) {
                    final ServiceVerificationHandler verificationHandler = new ServiceVerificationHandler();
                    ServiceTarget target = context.getServiceTarget();
                    final int coreThreads =  params.getCoreThreads() == null ? params.getMaxThreads().getScaledCount() : params.getCoreThreads().getScaledCount();
                    final ServiceName serviceName = ThreadsServices.executorName(params.getName());
                    final BoundedQueueThreadPoolService service = new BoundedQueueThreadPoolService(
                            coreThreads,
                            params.getMaxThreads().getScaledCount(),
                            params.getQueueLength().getScaledCount(),
                            params.isBlocking(),
                            params.getKeepAliveTime(),
                            params.isAllowCoreTimeout());

                    //TODO add the handoffExceutor injection

                    final ServiceBuilder<Executor> serviceBuilder = target.addService(serviceName, service);
                    ThreadsSubsystemThreadPoolOperationUtils.addThreadFactoryDependency(params.getThreadFactory(), serviceName, serviceBuilder, service.getThreadFactoryInjector(), target, params.getName() + "-threads");
                    serviceBuilder.addListener(verificationHandler);
                    serviceBuilder.install();

                    context.addStep(verificationHandler, OperationContext.Stage.VERIFY);

                    if (context.completeStep() == OperationContext.ResultAction.ROLLBACK) {
                        context.removeService(serviceName);
                    }
                }
            }, OperationContext.Stage.RUNTIME);
        }

        context.completeStep();
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        return ThreadsSubsystemProviders.ADD_BOUNDED_QUEUE_THREAD_POOL_DESC.getModelDescription(locale);
    }


}
