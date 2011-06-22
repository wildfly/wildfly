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
import java.util.concurrent.ScheduledExecutorService;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.threads.CommonAttributes.KEEPALIVE_TIME;
import static org.jboss.as.threads.CommonAttributes.MAX_THREADS;
import static org.jboss.as.threads.CommonAttributes.PROPERTIES;
import static org.jboss.as.threads.CommonAttributes.THREAD_FACTORY;
import org.jboss.as.threads.ThreadsSubsystemThreadPoolOperationUtils.BaseOperationParameters;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;

/**
 * Adds a scheduled thread pool.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class ScheduledThreadPoolAdd implements OperationStepHandler, DescriptionProvider {

    static final ScheduledThreadPoolAdd INSTANCE = new ScheduledThreadPoolAdd();

    /**
     * {@inheritDoc}
     */
    public void execute(OperationContext context, ModelNode operation) {
        final BaseOperationParameters params = ThreadsSubsystemThreadPoolOperationUtils.parseScheduledThreadPoolOperationParameters(operation);
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

        if (context.getType() == OperationContext.Type.SERVER) {
            context.addStep(new OperationStepHandler() {
                public void execute(OperationContext context, ModelNode operation) {
                    final ServiceVerificationHandler verificationHandler = new ServiceVerificationHandler();

                    ServiceTarget target = context.getServiceTarget();
                    final ServiceName serviceName = ThreadsServices.executorName(params.getName());
                    final ScheduledThreadPoolService service = new ScheduledThreadPoolService(params.getMaxThreads().getScaledCount(), params.getKeepAliveTime());
                    final ServiceBuilder<ScheduledExecutorService> serviceBuilder = target.addService(serviceName, service);
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
        return ThreadsSubsystemProviders.ADD_SCHEDULED_THREAD_POOL_DESC.getModelDescription(locale);
    }
}
