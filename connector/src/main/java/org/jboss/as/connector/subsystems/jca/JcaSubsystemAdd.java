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


package org.jboss.as.connector.subsystems.jca;

import java.util.List;

import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;

import org.jboss.as.connector.ConnectorServices;
import org.jboss.as.connector.deployers.RaDeploymentActivator;
import org.jboss.as.connector.registry.DriverRegistryService;
import org.jboss.as.connector.transactionintegration.TransactionIntegrationService;
import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.txn.service.TxnServices;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.tm.JBossXATerminator;
import org.jboss.tm.XAResourceRecoveryRegistry;

/**
 * JCA subsystem
 *
 * @author @author <a href="mailto:stefano.maestri@redhat.com">Stefano Maestri</a>
 * @author @author <a href="mailto:jesper.pedersen@jboss.org">Jesper Pedersen</a>
 */
class JcaSubsystemAdd extends AbstractBoottimeAddStepHandler {

    static final JcaSubsystemAdd INSTANCE = new JcaSubsystemAdd();


    protected void populateModel(ModelNode operation, ModelNode model) {

    }

    protected void performBoottime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) {
        final RaDeploymentActivator deploymentActivator = new RaDeploymentActivator();

        context.addStep(new AbstractDeploymentChainStep() {
            protected void execute(DeploymentProcessorTarget processorTarget) {
                deploymentActivator.activateProcessors(processorTarget);
            }
        }, OperationContext.Stage.RUNTIME);


        ServiceTarget serviceTarget = context.getServiceTarget();

        TransactionIntegrationService tiService = new TransactionIntegrationService();

        newControllers.add(serviceTarget
                .addService(ConnectorServices.TRANSACTION_INTEGRATION_SERVICE, tiService)
                .addDependency(TxnServices.JBOSS_TXN_TRANSACTION_MANAGER, TransactionManager.class, tiService.getTmInjector())
                .addDependency(TxnServices.JBOSS_TXN_SYNCHRONIZATION_REGISTRY, TransactionSynchronizationRegistry.class, tiService.getTsrInjector())
                .addDependency(TxnServices.JBOSS_TXN_USER_TRANSACTION_REGISTRY, org.jboss.tm.usertx.UserTransactionRegistry.class, tiService.getUtrInjector())
                .addDependency(TxnServices.JBOSS_TXN_XA_TERMINATOR, JBossXATerminator.class, tiService.getTerminatorInjector())
                .addDependency(TxnServices.JBOSS_TXN_ARJUNA_RECOVERY_MANAGER, XAResourceRecoveryRegistry.class, tiService.getRrInjector())
                .addListener(verificationHandler)
                .setInitialMode(ServiceController.Mode.ACTIVE)
                .install());

        final JcaSubsystemConfiguration config = new JcaSubsystemConfiguration();

        final JcaConfigService connectorConfigService = new JcaConfigService(config);
        newControllers.add(serviceTarget
                .addService(ConnectorServices.CONNECTOR_CONFIG_SERVICE, connectorConfigService)
                .addListener(verificationHandler)
                .setInitialMode(Mode.ACTIVE)
                .install());

        final IdleRemoverService idleRemoverService = new IdleRemoverService();
        newControllers.add(serviceTarget
                .addService(ConnectorServices.IDLE_REMOVER_SERVICE, idleRemoverService)
                .addListener(verificationHandler)
                .setInitialMode(Mode.ACTIVE)
                .install());

        final ConnectionValidatorService connectionValidatorService = new ConnectionValidatorService();
        newControllers.add(serviceTarget
                .addService(ConnectorServices.CONNECTION_VALIDATOR_SERVICE, connectionValidatorService)
                .addListener(verificationHandler)
                .setInitialMode(Mode.ACTIVE)
                .install());

        // TODO does the install of this and the DriverProcessor
        // belong in DataSourcesSubsystemAdd?
        final DriverRegistryService driverRegistryService = new DriverRegistryService();
        newControllers.add(serviceTarget.addService(ConnectorServices.JDBC_DRIVER_REGISTRY_SERVICE, driverRegistryService)
                .addListener(verificationHandler)
                .install());

        newControllers.addAll(deploymentActivator.activateServices(serviceTarget, verificationHandler));
    }
}
