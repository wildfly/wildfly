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

import org.glassfish.enterprise.concurrent.ContextServiceImpl;
import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.ee.concurrent.deployers.EEConcurrentDefaultManagedThreadFactoryProcessor;
import org.jboss.as.ee.concurrent.service.ConcurrentServiceNames;
import org.jboss.as.ee.concurrent.service.ManagedThreadFactoryService;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;

import java.util.List;

/**
 * @author Eduardo Martins
 */
public class DefaultManagedThreadFactoryAdd extends AbstractBoottimeAddStepHandler {

    static final DefaultManagedThreadFactoryAdd INSTANCE = new DefaultManagedThreadFactoryAdd();

    private DefaultManagedThreadFactoryAdd() {
        super(DefaultManagedThreadFactoryResourceDefinition.ATTRIBUTES);
    }

    @Override
    protected void performBoottime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {
        final int priority = ManagedThreadFactoryResourceDefinition.PRIORITY_AD.resolveModelAttribute(context, model).asInt();

        final ManagedThreadFactoryService service = new ManagedThreadFactoryService(ConcurrentServiceNames.DEFAULT_NAME, priority);
        final ServiceBuilder serviceBuilder = context.getServiceTarget().addService(ConcurrentServiceNames.DEFAULT_MANAGED_THREAD_FACTORY_SERVICE_NAME, service)
                .addDependency(ConcurrentServiceNames.DEFAULT_CONTEXT_SERVICE_SERVICE_NAME, ContextServiceImpl.class, service.getContextServiceInjector())
                .addListener(verificationHandler);
        newControllers.add(serviceBuilder.install());

        context.addStep(new AbstractDeploymentChainStep() {
            protected void execute(DeploymentProcessorTarget processorTarget) {
                processorTarget.addDeploymentProcessor(EeExtension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_EE_CONCURRENT_DEFAULT_MANAGED_THREAD_FACTORY, new EEConcurrentDefaultManagedThreadFactoryProcessor());
            }
        }, OperationContext.Stage.RUNTIME);
    }
}
