/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.ejb3.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.KEEPALIVE_TIME;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.MAX_THREADS;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.NAME;

import java.util.List;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.threads.ManagedJBossThreadPoolExecutorService;
import org.jboss.as.threads.ThreadFactoryService;
import org.jboss.as.threads.TimeSpec;
import org.jboss.as.threads.UnboundedQueueThreadPoolService;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;

/**
 * A {@link org.jboss.as.controller.AbstractBoottimeAddStepHandler} to handle the add operation for the EJB
 * thread pool service, in the EJB subsystem
 *
 * @author Stuart Douglas
 */
public class EJB3ThreadPoolAdd extends AbstractBoottimeAddStepHandler {

    public static final ServiceName BASE_SERVICE_NAME = ServiceName.JBOSS.append("as", "ejb3", "threadPool");
    private static final ServiceName THREAD_FACTORY_BASE_SERVICE_NAME = BASE_SERVICE_NAME.append("threadFactory");

    static final EJB3ThreadPoolAdd INSTANCE = new EJB3ThreadPoolAdd();

    private EJB3ThreadPoolAdd() {
    }

    // TODO why is this a boottime handler?
    @Override
    protected void performBoottime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {
        final String name = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.ADDRESS)).getLastElement().getValue();
        installRuntimeServices(context, name, model, verificationHandler, newControllers);
    }

    void installRuntimeServices(final OperationContext context, final String name, final ModelNode model, ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> newControllers) throws OperationFailedException {

        final Integer maxThreads = EJB3ThreadPoolResourceDefinition.MAX_THREADS.resolveModelAttribute(context, model).asInt(Runtime.getRuntime().availableProcessors());
        final Integer keepAlive = EJB3ThreadPoolResourceDefinition.KEEPALIVE_TIME.resolveModelAttribute(context, model).asInt(0);

        final ServiceTarget serviceTarget = context.getServiceTarget();
        final ThreadFactoryService threadFactory = new ThreadFactoryService();
        threadFactory.setThreadGroupName("EJB " + name);
        final ServiceName threadFactoryServiceName = THREAD_FACTORY_BASE_SERVICE_NAME.append(name);

        ServiceBuilder<ThreadFactory> factoryBuilder = serviceTarget.addService(threadFactoryServiceName, threadFactory);
        if (verificationHandler != null) {
            factoryBuilder.addListener(verificationHandler);
        }
        if (newControllers != null) {
            newControllers.add(factoryBuilder.install());
        } else {
            factoryBuilder.install();
        }

        final UnboundedQueueThreadPoolService service = new UnboundedQueueThreadPoolService(maxThreads,  new TimeSpec(TimeUnit.MILLISECONDS, keepAlive));
        ServiceBuilder<ManagedJBossThreadPoolExecutorService> builder = serviceTarget.addService(BASE_SERVICE_NAME.append(name), service)
                .addDependency(threadFactoryServiceName, ThreadFactory.class, service.getThreadFactoryInjector())
                .setInitialMode(ServiceController.Mode.ACTIVE);
        if (verificationHandler != null) {
            builder.addListener(verificationHandler);
        }
        if (newControllers != null) {
            newControllers.add(
                    builder.install());
        } else {
            builder.install();
        }
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {

        PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        String name = address.getLastElement().getValue();
        model.get(NAME).set(name);
        if (operation.hasDefined(KEEPALIVE_TIME)) {
            model.get(KEEPALIVE_TIME).set(operation.require(KEEPALIVE_TIME).asString());
        }
        if (operation.hasDefined(MAX_THREADS)) {
            model.get(MAX_THREADS).set(operation.require(MAX_THREADS).asString());
        }
    }
}
