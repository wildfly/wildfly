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
import org.glassfish.enterprise.concurrent.spi.ContextSetupProvider;
import org.glassfish.enterprise.concurrent.spi.TransactionSetupProvider;
import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.ee.concurrent.DefaultContextSetupProviderImpl;
import org.jboss.as.ee.concurrent.deployers.EEConcurrentDefaultContextServiceProcessor;
import org.jboss.as.ee.concurrent.service.ConcurrentServiceNames;
import org.jboss.as.ee.concurrent.service.ContextServiceService;
import org.jboss.as.ee.concurrent.service.TransactionSetupProviderService;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.value.ImmediateValue;

import javax.transaction.TransactionManager;
import java.util.List;

/**
 * @author Eduardo Martins
 */
public class DefaultContextServiceAdd extends AbstractBoottimeAddStepHandler {

    static final DefaultContextServiceAdd INSTANCE = new DefaultContextServiceAdd();

    private DefaultContextServiceAdd() {
        super(DefaultContextServiceResourceDefinition.ATTRIBUTES);
    }

    @Override
    protected void performBoottime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {

        final boolean useTransactionSetupProvider = DefaultContextServiceResourceDefinition.USE_TRANSACTION_SETUP_PROVIDER_AD.resolveModelAttribute(context, model).asBoolean();

        // install the service which manages the default context service
        final ContextServiceService contextServiceService = new ContextServiceService(ConcurrentServiceNames.DEFAULT_NAME);
        final ServiceBuilder<ContextServiceImpl> contextServiceServiceBuilder = context.getServiceTarget().addService(ConcurrentServiceNames.DEFAULT_CONTEXT_SERVICE_SERVICE_NAME, contextServiceService)
                .addInjectionValue(contextServiceService.getContextSetupProvider(), new ImmediateValue<ContextSetupProvider>(new DefaultContextSetupProviderImpl()));
        if(useTransactionSetupProvider) {
            // install the transaction setup provider's service
            final TransactionSetupProviderService transactionSetupProviderService = new TransactionSetupProviderService();
            final ServiceBuilder<TransactionSetupProvider> transactionSetupServiceBuilder = context.getServiceTarget().addService(ConcurrentServiceNames.TRANSACTION_SETUP_PROVIDER_SERVICE_NAME, transactionSetupProviderService)
                        .addDependency(ServiceName.JBOSS.append("txn", "TransactionManager"), TransactionManager.class, transactionSetupProviderService.getTransactionManagerInjectedValue());
            newControllers.add(transactionSetupServiceBuilder.addListener(verificationHandler).install());
            // add it to deps of context service's service, for injection of its value
            contextServiceServiceBuilder.addDependency(ConcurrentServiceNames.TRANSACTION_SETUP_PROVIDER_SERVICE_NAME,TransactionSetupProvider.class,contextServiceService.getTransactionSetupProvider());
        }
        newControllers.add(contextServiceServiceBuilder.addListener(verificationHandler).install());

        // add related DUPs
        context.addStep(new AbstractDeploymentChainStep() {
            protected void execute(DeploymentProcessorTarget processorTarget) {
                processorTarget.addDeploymentProcessor(EeExtension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_EE_CONCURRENT_DEFAULT_CONTEXT_SERVICE, new EEConcurrentDefaultContextServiceProcessor());
            }
        }, OperationContext.Stage.RUNTIME);
    }
}
