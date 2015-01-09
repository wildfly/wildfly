/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.as.ee.subsystem;

import org.glassfish.enterprise.concurrent.AbstractManagedExecutorService;
import org.glassfish.enterprise.concurrent.ContextServiceImpl;
import org.glassfish.enterprise.concurrent.ManagedExecutorServiceAdapter;
import org.glassfish.enterprise.concurrent.ManagedThreadFactoryImpl;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.ee.concurrent.service.ConcurrentServiceNames;
import org.jboss.as.ee.concurrent.service.ManagedExecutorServiceService;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.wildfly.extension.requestcontroller.RequestController;
import org.wildfly.extension.requestcontroller.RequestControllerExtension;

import java.util.concurrent.TimeUnit;

/**
 * @author Eduardo Martins
 */
public class ManagedExecutorServiceAdd extends AbstractAddStepHandler {

    static final ManagedExecutorServiceAdd INSTANCE = new ManagedExecutorServiceAdd();

    private ManagedExecutorServiceAdd() {
        super(ManagedExecutorServiceResourceDefinition.ATTRIBUTES);
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        final String name = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.ADDRESS)).getLastElement().getValue();

        final String jndiName = ManagedExecutorServiceResourceDefinition.JNDI_NAME_AD.resolveModelAttribute(context, model).asString();
        final long hungTaskThreshold = ManagedExecutorServiceResourceDefinition.HUNG_TASK_THRESHOLD_AD.resolveModelAttribute(context, model).asLong();
        final boolean longRunningTasks = ManagedExecutorServiceResourceDefinition.LONG_RUNNING_TASKS_AD.resolveModelAttribute(context, model).asBoolean();
        final int coreThreads = ManagedExecutorServiceResourceDefinition.CORE_THREADS_AD.resolveModelAttribute(context, model).asInt();
        final int maxThreads = ManagedExecutorServiceResourceDefinition.MAX_THREADS_AD.resolveModelAttribute(context, model).asInt();
        final long keepAliveTime = ManagedExecutorServiceResourceDefinition.KEEPALIVE_TIME_AD.resolveModelAttribute(context, model).asLong();
        final TimeUnit keepAliveTimeUnit = TimeUnit.MILLISECONDS;
        final long threadLifeTime = 0L;
        final int queueLength = ManagedExecutorServiceResourceDefinition.QUEUE_LENGTH_AD.resolveModelAttribute(context, model).asInt();
        final AbstractManagedExecutorService.RejectPolicy rejectPolicy = AbstractManagedExecutorService.RejectPolicy.valueOf(ManagedExecutorServiceResourceDefinition.REJECT_POLICY_AD.resolveModelAttribute(context, model).asString());

        final ManagedExecutorServiceService service = new ManagedExecutorServiceService(name, jndiName, hungTaskThreshold, longRunningTasks, coreThreads, maxThreads, keepAliveTime, keepAliveTimeUnit, threadLifeTime, queueLength, rejectPolicy);
        final ServiceBuilder<ManagedExecutorServiceAdapter> serviceBuilder = context.getServiceTarget().addService(ConcurrentServiceNames.getManagedExecutorServiceServiceName(name), service);

        boolean rcPresent = context.getOriginalRootResource().hasChild(PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, RequestControllerExtension.SUBSYSTEM_NAME));
        String contextService = null;
        if(model.hasDefined(ManagedExecutorServiceResourceDefinition.CONTEXT_SERVICE)) {
            contextService = ManagedExecutorServiceResourceDefinition.CONTEXT_SERVICE_AD.resolveModelAttribute(context, model).asString();
        }
        if (contextService != null) {
            serviceBuilder.addDependency(ConcurrentServiceNames.getContextServiceServiceName(contextService), ContextServiceImpl.class, service.getContextServiceInjector());
        }
        String threadFactory = null;
        if(model.hasDefined(ManagedExecutorServiceResourceDefinition.THREAD_FACTORY)) {
            threadFactory = ManagedExecutorServiceResourceDefinition.THREAD_FACTORY_AD.resolveModelAttribute(context, model).asString();
        }
        if (threadFactory != null) {
            serviceBuilder.addDependency(ConcurrentServiceNames.getManagedThreadFactoryServiceName(threadFactory), ManagedThreadFactoryImpl.class, service.getManagedThreadFactoryInjector());
        }
        if(rcPresent) {
            serviceBuilder.addDependency(RequestController.SERVICE_NAME, RequestController.class, service.getRequestController());
        }
        serviceBuilder.install();
    }
}
