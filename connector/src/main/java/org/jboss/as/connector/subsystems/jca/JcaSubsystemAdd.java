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

import static org.jboss.as.connector.subsystems.jca.Constants.ARCHIVE_VALIDATION_ENABLED;
import static org.jboss.as.connector.subsystems.jca.Constants.ARCHIVE_VALIDATION_FAIL_ON_ERROR;
import static org.jboss.as.connector.subsystems.jca.Constants.ARCHIVE_VALIDATION_FAIL_ON_WARN;
import static org.jboss.as.connector.subsystems.jca.Constants.BEAN_VALIDATION_ENABLED;
import static org.jboss.as.connector.subsystems.jca.Constants.CACHED_CONNECTION_MANAGER_DEBUG;
import static org.jboss.as.connector.subsystems.jca.Constants.CACHED_CONNECTION_MANAGER_ERROR;
import static org.jboss.as.connector.subsystems.jca.Constants.LONG_RUNNING_THREADS;
import static org.jboss.as.connector.subsystems.jca.Constants.SHORT_RUNNING_THREADS;
import static org.jboss.as.connector.subsystems.jca.Constants.THREAD_POOL;

import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;
import java.util.List;
import java.util.concurrent.Executor;

import org.jboss.as.connector.ConnectorServices;
import org.jboss.as.connector.bootstrap.DefaultBootStrapContextService;
import org.jboss.as.connector.deployers.RaDeploymentActivator;
import org.jboss.as.connector.registry.DriverRegistryService;
import org.jboss.as.connector.services.CcmService;
import org.jboss.as.connector.transactionintegration.TransactionIntegrationService;
import org.jboss.as.connector.workmanager.WorkManagerService;
import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.threads.ThreadsServices;
import org.jboss.as.txn.TxnServices;
import org.jboss.dmr.ModelNode;
import org.jboss.jca.core.api.bootstrap.CloneableBootstrapContext;
import org.jboss.jca.core.api.workmanager.WorkManager;
import org.jboss.jca.core.bootstrapcontext.BaseCloneableBootstrapContext;
import org.jboss.jca.core.spi.transaction.TransactionIntegration;
import org.jboss.jca.core.workmanager.WorkManagerImpl;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.tm.JBossXATerminator;
import org.jboss.tm.XAResourceRecoveryRegistry;


/**
 * @author @author <a href="mailto:stefano.maestri@redhat.com">Stefano
 *         Maestri</a>
 */
class JcaSubsystemAdd extends AbstractBoottimeAddStepHandler {

    static final JcaSubsystemAdd INSTANCE = new JcaSubsystemAdd();


    protected void populateModel(ModelNode operation, ModelNode model) {

        model.get(THREAD_POOL).setEmptyObject();

        final boolean beanValidationEnabled = ParamsUtils.parseBooleanParameter(operation, BEAN_VALIDATION_ENABLED, false);
        final boolean archiveValidationEnabled = ParamsUtils
                .parseBooleanParameter(operation, ARCHIVE_VALIDATION_ENABLED, false);
        final boolean failOnError = ParamsUtils.parseBooleanParameter(operation, ARCHIVE_VALIDATION_FAIL_ON_ERROR, true);
        final boolean failOnWarn = ParamsUtils.parseBooleanParameter(operation, ARCHIVE_VALIDATION_FAIL_ON_WARN, false);
        final boolean ccmDebug = ParamsUtils.parseBooleanParameter(operation, CACHED_CONNECTION_MANAGER_DEBUG, false);
        final boolean ccmError = ParamsUtils.parseBooleanParameter(operation, CACHED_CONNECTION_MANAGER_ERROR, false);

        // Apply to the model

        if (ParamsUtils.has(operation, BEAN_VALIDATION_ENABLED)) {
            model.get(BEAN_VALIDATION_ENABLED).set(beanValidationEnabled);
        }
        if (ParamsUtils.has(operation, ARCHIVE_VALIDATION_ENABLED)) {
            model.get(ARCHIVE_VALIDATION_ENABLED).set(archiveValidationEnabled);
        }
        if (ParamsUtils.has(operation, ARCHIVE_VALIDATION_FAIL_ON_ERROR)) {
            model.get(ARCHIVE_VALIDATION_FAIL_ON_ERROR).set(failOnError);
        }
        if (ParamsUtils.has(operation, ARCHIVE_VALIDATION_FAIL_ON_WARN)) {
            model.get(ARCHIVE_VALIDATION_FAIL_ON_WARN).set(failOnWarn);
        }
        if (ParamsUtils.has(operation, CACHED_CONNECTION_MANAGER_DEBUG)) {
            model.get(CACHED_CONNECTION_MANAGER_DEBUG).set(ccmDebug);
        }
        if (ParamsUtils.has(operation, CACHED_CONNECTION_MANAGER_ERROR)) {
            model.get(CACHED_CONNECTION_MANAGER_ERROR).set(ccmError);
        }
    }

    protected void performBoottime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) {
        final RaDeploymentActivator deploymentActivator = new RaDeploymentActivator();

        context.addStep(new AbstractDeploymentChainStep() {
            protected void execute(DeploymentProcessorTarget processorTarget) {
                deploymentActivator.activateProcessors(processorTarget);
            }
        }, OperationContext.Stage.RUNTIME);

        final boolean archiveValidationEnabled = ParamsUtils
                .parseBooleanParameter(operation, ARCHIVE_VALIDATION_ENABLED, false);
        final boolean failOnError = ParamsUtils.parseBooleanParameter(operation, ARCHIVE_VALIDATION_FAIL_ON_ERROR, true);
        final boolean failOnWarn = ParamsUtils.parseBooleanParameter(operation, ARCHIVE_VALIDATION_FAIL_ON_WARN, false);
        final boolean ccmDebug = ParamsUtils.parseBooleanParameter(operation, CACHED_CONNECTION_MANAGER_DEBUG, false);
        final boolean ccmError = ParamsUtils.parseBooleanParameter(operation, CACHED_CONNECTION_MANAGER_ERROR, false);
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
                .setInitialMode(Mode.ACTIVE)
                .install());

        CcmService ccmService = new CcmService(ccmDebug, ccmError);
        newControllers.add(serviceTarget
                .addService(ConnectorServices.CCM_SERVICE, ccmService)
                .addDependency(ConnectorServices.TRANSACTION_INTEGRATION_SERVICE, TransactionIntegration.class,
                        ccmService.getTransactionIntegrationInjector())
                .addListener(verificationHandler)
                .install());

        WorkManager wm = new WorkManagerImpl();

        final WorkManagerService wmService = new WorkManagerService(wm);
        newControllers.add(serviceTarget
                .addService(ConnectorServices.WORKMANAGER_SERVICE, wmService)
                .addDependency(ThreadsServices.EXECUTOR.append(SHORT_RUNNING_THREADS), Executor.class, wmService.getExecutorShortInjector())
                .addDependency(ThreadsServices.EXECUTOR.append(LONG_RUNNING_THREADS), Executor.class, wmService.getExecutorLongInjector())
                .addDependency(TxnServices.JBOSS_TXN_XA_TERMINATOR, JBossXATerminator.class, wmService.getXaTerminatorInjector())
                .addListener(verificationHandler)
                .setInitialMode(Mode.ACTIVE)
                .install());

        CloneableBootstrapContext ctx = new BaseCloneableBootstrapContext();
        final DefaultBootStrapContextService defaultBootCtxService = new DefaultBootStrapContextService(ctx);
        newControllers.add(serviceTarget
                .addService(ConnectorServices.DEFAULT_BOOTSTRAP_CONTEXT_SERVICE, defaultBootCtxService)
                .addDependency(ConnectorServices.WORKMANAGER_SERVICE, WorkManager.class, defaultBootCtxService.getWorkManagerValueInjector())
                .addDependency(TxnServices.JBOSS_TXN_XA_TERMINATOR, JBossXATerminator.class, defaultBootCtxService.getXaTerminatorInjector())
                .addDependency(TxnServices.JBOSS_TXN_ARJUNA_TRANSACTION_MANAGER, com.arjuna.ats.jbossatx.jta.TransactionManagerService.class, defaultBootCtxService.getTxManagerInjector())
                .addListener(verificationHandler)
                .setInitialMode(Mode.ACTIVE)
                .install());
        final JcaSubsystemConfiguration config = new JcaSubsystemConfiguration();

        config.setArchiveValidation(archiveValidationEnabled);
        config.setArchiveValidationFailOnError(failOnError);
        config.setArchiveValidationFailOnWarn(failOnWarn);

        // FIXME Bean validation currently not used
        config.setBeanValidation(false);

        final JcaConfigService connectorConfigService = new JcaConfigService(config);
        newControllers.add(serviceTarget
                .addService(ConnectorServices.CONNECTOR_CONFIG_SERVICE, connectorConfigService)
                .addDependency(ConnectorServices.DEFAULT_BOOTSTRAP_CONTEXT_SERVICE,
                        CloneableBootstrapContext.class,
                        connectorConfigService.getDefaultBootstrapContextInjector())
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
