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

import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;

import org.jboss.as.connector.deployers.ra.RaDeploymentActivator;
import org.jboss.as.connector.services.driver.registry.DriverRegistryService;
import org.jboss.as.connector.services.transactionintegration.TransactionIntegrationService;
import org.jboss.as.connector.util.ConnectorServices;
import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.txn.integration.JBossContextXATerminator;
import org.jboss.as.txn.service.TxnServices;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceTarget;
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

    protected void performBoottime(OperationContext context, ModelNode operation, ModelNode model) {
        final boolean appclient = context.getProcessType() == ProcessType.APPLICATION_CLIENT;
        final RaDeploymentActivator raDeploymentActivator = new RaDeploymentActivator(appclient);
        context.addStep(new AbstractDeploymentChainStep() {
            protected void execute(DeploymentProcessorTarget processorTarget) {
                raDeploymentActivator.activateProcessors(processorTarget);
            }
        }, OperationContext.Stage.RUNTIME);


        ServiceTarget serviceTarget = context.getServiceTarget();

        TransactionIntegrationService tiService = new TransactionIntegrationService();

        serviceTarget
                .addService(ConnectorServices.TRANSACTION_INTEGRATION_SERVICE, tiService)
                .addDependency(TxnServices.JBOSS_TXN_TRANSACTION_MANAGER, TransactionManager.class, tiService.getTmInjector())
                .addDependency(TxnServices.JBOSS_TXN_SYNCHRONIZATION_REGISTRY, TransactionSynchronizationRegistry.class, tiService.getTsrInjector())
                .addDependency(TxnServices.JBOSS_TXN_USER_TRANSACTION_REGISTRY, org.jboss.tm.usertx.UserTransactionRegistry.class, tiService.getUtrInjector())
                .addDependency(TxnServices.JBOSS_TXN_CONTEXT_XA_TERMINATOR, JBossContextXATerminator.class, tiService.getTerminatorInjector())
                .addDependency(TxnServices.JBOSS_TXN_ARJUNA_RECOVERY_MANAGER, XAResourceRecoveryRegistry.class, tiService.getRrInjector())
                .setInitialMode(ServiceController.Mode.ACTIVE)
                .install();

        final JcaSubsystemConfiguration config = new JcaSubsystemConfiguration();

        final JcaConfigService connectorConfigService = new JcaConfigService(config);
        serviceTarget
                .addService(ConnectorServices.CONNECTOR_CONFIG_SERVICE, connectorConfigService)
                .setInitialMode(Mode.ACTIVE)
                .install();

        final IdleRemoverService idleRemoverService = new IdleRemoverService();
        serviceTarget
                .addService(ConnectorServices.IDLE_REMOVER_SERVICE, idleRemoverService)
                .setInitialMode(Mode.ACTIVE)
                .install();

        final ConnectionValidatorService connectionValidatorService = new ConnectionValidatorService();
        serviceTarget
                .addService(ConnectorServices.CONNECTION_VALIDATOR_SERVICE, connectionValidatorService)
                .setInitialMode(Mode.ACTIVE)
                .install();



        // TODO does the install of this and the DriverProcessor
        // belong in DataSourcesSubsystemAdd?
        final DriverRegistryService driverRegistryService = new DriverRegistryService();
        serviceTarget.addService(ConnectorServices.JDBC_DRIVER_REGISTRY_SERVICE, driverRegistryService)
                .install();

        raDeploymentActivator.activateServices(serviceTarget);
    }
}
