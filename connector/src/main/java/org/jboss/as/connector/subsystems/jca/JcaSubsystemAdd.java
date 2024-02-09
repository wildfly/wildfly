/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */


package org.jboss.as.connector.subsystems.jca;

import static org.jboss.as.connector.subsystems.jca.JcaSubsystemRootDefinition.TRANSACTION_INTEGRATION_CAPABILITY;
import static org.jboss.as.connector.util.ConnectorServices.LOCAL_TRANSACTION_PROVIDER_CAPABILITY;
import static org.jboss.as.connector.util.ConnectorServices.TRANSACTION_INTEGRATION_CAPABILITY_NAME;
import static org.jboss.as.connector.util.ConnectorServices.TRANSACTION_SYNCHRONIZATION_REGISTRY_CAPABILITY;
import static org.jboss.as.connector.util.ConnectorServices.TRANSACTION_XA_RESOURCE_RECOVERY_REGISTRY_CAPABILITY;

import java.util.function.Consumer;
import java.util.function.Supplier;

import jakarta.transaction.TransactionSynchronizationRegistry;

import org.jboss.as.connector.deployers.ra.RaDeploymentActivator;
import org.jboss.as.connector.services.driver.registry.DriverRegistryService;
import org.jboss.as.connector.services.transactionintegration.TransactionIntegrationService;
import org.jboss.as.connector.util.ConnectorServices;
import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.CapabilityServiceBuilder;
import org.jboss.as.controller.CapabilityServiceTarget;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.naming.service.NamingService;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.txn.integration.JBossContextXATerminator;
import org.jboss.as.txn.service.TxnServices;
import org.jboss.dmr.ModelNode;
import org.jboss.jca.core.spi.transaction.TransactionIntegration;
import org.jboss.tm.XAResourceRecoveryRegistry;
import org.jboss.tm.usertx.UserTransactionRegistry;

/**
 * Jakarta Connectors subsystem
 *
 * @author @author <a href="mailto:stefano.maestri@redhat.com">Stefano Maestri</a>
 * @author @author <a href="mailto:jesper.pedersen@jboss.org">Jesper Pedersen</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
class JcaSubsystemAdd extends AbstractBoottimeAddStepHandler {

    static final JcaSubsystemAdd INSTANCE = new JcaSubsystemAdd();

    protected void performBoottime(OperationContext context, ModelNode operation, ModelNode model) {
        final boolean appclient = context.getProcessType() == ProcessType.APPLICATION_CLIENT;
        final RaDeploymentActivator raDeploymentActivator = new RaDeploymentActivator(appclient);
        context.addStep(new AbstractDeploymentChainStep() {
            protected void execute(DeploymentProcessorTarget processorTarget) {
                raDeploymentActivator.activateProcessors(processorTarget);
            }
        }, OperationContext.Stage.RUNTIME);


        final CapabilityServiceTarget serviceTarget = context.getCapabilityServiceTarget();

        final CapabilityServiceBuilder<?> sb = serviceTarget.addCapability(TRANSACTION_INTEGRATION_CAPABILITY);
        final Consumer<TransactionIntegration> tiConsumer = sb.provides(TRANSACTION_INTEGRATION_CAPABILITY, ConnectorServices.TRANSACTION_INTEGRATION_SERVICE);
        sb.requiresCapability(LOCAL_TRANSACTION_PROVIDER_CAPABILITY, Void.class);
        final Supplier<TransactionSynchronizationRegistry> tsrSupplier =  sb.requiresCapability(TRANSACTION_SYNCHRONIZATION_REGISTRY_CAPABILITY, TransactionSynchronizationRegistry.class);
        final Supplier<UserTransactionRegistry> utrSupplier = sb.requires(TxnServices.JBOSS_TXN_USER_TRANSACTION_REGISTRY);
        final Supplier<JBossContextXATerminator> terminatorSupplier = sb.requires(TxnServices.JBOSS_TXN_CONTEXT_XA_TERMINATOR);
        final Supplier<XAResourceRecoveryRegistry> rrSupplier = sb.requiresCapability(TRANSACTION_XA_RESOURCE_RECOVERY_REGISTRY_CAPABILITY, XAResourceRecoveryRegistry.class);
        final TransactionIntegrationService tiService = new TransactionIntegrationService(tiConsumer, tsrSupplier, utrSupplier, terminatorSupplier, rrSupplier);
        sb.setInstance(tiService);
        sb.install();

        // Cache the some capability service names for use by our runtime services
        final CapabilityServiceSupport support = context.getCapabilityServiceSupport();
        ConnectorServices.registerCapabilityServiceName(LOCAL_TRANSACTION_PROVIDER_CAPABILITY, support.getCapabilityServiceName(LOCAL_TRANSACTION_PROVIDER_CAPABILITY));
        ConnectorServices.registerCapabilityServiceName(NamingService.CAPABILITY_NAME, support.getCapabilityServiceName(NamingService.CAPABILITY_NAME));
        ConnectorServices.registerCapabilityServiceName(TRANSACTION_INTEGRATION_CAPABILITY_NAME, support.getCapabilityServiceName(TRANSACTION_INTEGRATION_CAPABILITY_NAME));

        final JcaSubsystemConfiguration config = new JcaSubsystemConfiguration();
        final JcaConfigService connectorConfigService = new JcaConfigService(config);
        serviceTarget.addService(ConnectorServices.CONNECTOR_CONFIG_SERVICE).setInstance(connectorConfigService).install();

        final IdleRemoverService idleRemoverService = new IdleRemoverService();
        serviceTarget.addService(ConnectorServices.IDLE_REMOVER_SERVICE).setInstance(idleRemoverService).install();

        final ConnectionValidatorService connectionValidatorService = new ConnectionValidatorService();
        serviceTarget.addService(ConnectorServices.CONNECTION_VALIDATOR_SERVICE).setInstance(connectionValidatorService).install();

        // TODO: Does the install of this and the DriverProcessor belong in DataSourcesSubsystemAdd?
        final DriverRegistryService driverRegistryService = new DriverRegistryService();
        serviceTarget.addService(ConnectorServices.JDBC_DRIVER_REGISTRY_SERVICE).setInstance(driverRegistryService).install();

        raDeploymentActivator.activateServices(serviceTarget);
    }
}
