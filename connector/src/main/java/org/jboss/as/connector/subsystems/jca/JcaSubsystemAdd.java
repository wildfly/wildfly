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
import static org.jboss.as.connector.subsystems.jca.Constants.DEFAULT_WORKMANAGER_LONG_RUNNING_THREAD_POOL;
import static org.jboss.as.connector.subsystems.jca.Constants.DEFAULT_WORKMANAGER_SHORT_RUNNING_THREAD_POOL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADDRESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.util.concurrent.Executor;

import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;

import org.jboss.as.connector.ConnectorServices;
import org.jboss.as.connector.bootstrap.DefaultBootStrapContextService;
import org.jboss.as.connector.deployers.RaDeploymentActivator;
import org.jboss.as.connector.deployers.processors.DataSourceDefinitionDeployer;
import org.jboss.as.connector.registry.DriverRegistryService;
import org.jboss.as.connector.services.CcmService;
import org.jboss.as.connector.transactionintegration.TransactionIntegrationService;
import org.jboss.as.connector.workmanager.WorkManagerService;
import org.jboss.as.controller.BasicOperationResult;
import org.jboss.as.controller.ModelAddOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationHandler;
import org.jboss.as.controller.OperationResult;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.RuntimeTask;
import org.jboss.as.controller.RuntimeTaskContext;
import org.jboss.as.server.BootOperationContext;
import org.jboss.as.server.BootOperationHandler;
import org.jboss.as.server.deployment.Phase;
import org.jboss.as.threads.ThreadsServices;
import org.jboss.as.txn.TxnServices;
import org.jboss.dmr.ModelNode;
import org.jboss.jca.core.api.bootstrap.CloneableBootstrapContext;
import org.jboss.jca.core.api.workmanager.WorkManager;
import org.jboss.jca.core.bootstrapcontext.BaseCloneableBootstrapContext;
import org.jboss.jca.core.spi.transaction.TransactionIntegration;
import org.jboss.jca.core.workmanager.WorkManagerImpl;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.tm.JBossXATerminator;
import org.jboss.tm.XAResourceRecoveryRegistry;

/**
 * @author @author <a href="mailto:stefano.maestri@redhat.com">Stefano
 *         Maestri</a>
 */
class JcaSubsystemAdd implements ModelAddOperationHandler, BootOperationHandler {

    static final OperationHandler INSTANCE = new JcaSubsystemAdd();

    /**
     * {@inheritDoc}
     */
    @Override
    public OperationResult execute(final OperationContext context, final ModelNode operation, final ResultHandler resultHandler) {
        final String shortRunningThreadPool = operation.get(DEFAULT_WORKMANAGER_SHORT_RUNNING_THREAD_POOL).asString();
        final String longRunningThreadPool = operation.get(DEFAULT_WORKMANAGER_LONG_RUNNING_THREAD_POOL).asString();
        final boolean beanValidationEnabled = ParamsUtils.parseBooleanParameter(operation, BEAN_VALIDATION_ENABLED, false);
        final boolean archiveValidationEnabled = ParamsUtils
                .parseBooleanParameter(operation, ARCHIVE_VALIDATION_ENABLED, false);
        final boolean failOnError = ParamsUtils.parseBooleanParameter(operation, ARCHIVE_VALIDATION_FAIL_ON_ERROR, true);
        final boolean failOnWarn = ParamsUtils.parseBooleanParameter(operation, ARCHIVE_VALIDATION_FAIL_ON_WARN, false);
        final boolean ccmDebug = ParamsUtils.parseBooleanParameter(operation, CACHED_CONNECTION_MANAGER_DEBUG, false);
        final boolean ccmError = ParamsUtils.parseBooleanParameter(operation, CACHED_CONNECTION_MANAGER_ERROR, false);

        // Apply to the model
        final ModelNode model = context.getSubModel();

        if (shortRunningThreadPool != null) {
            model.get(DEFAULT_WORKMANAGER_SHORT_RUNNING_THREAD_POOL).set(shortRunningThreadPool);

        }
        if (longRunningThreadPool != null) {
            model.get(DEFAULT_WORKMANAGER_LONG_RUNNING_THREAD_POOL).set(longRunningThreadPool);

        }
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

        if (context instanceof BootOperationContext) {
            final BootOperationContext bootContext = BootOperationContext.class.cast(context);
            // Add the deployer which processes EE @DataSourceDefinition and
            // @DataSourceDefinitions
            // TODO: The DataSourceDefinitionDeployer should perhaps belong to
            // EE subsystem
            bootContext.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_DATA_SOURCE_DEFINITION,
                    new DataSourceDefinitionDeployer());

            context.getRuntimeContext().setRuntimeTask(new RuntimeTask() {
                @Override
                public void execute(RuntimeTaskContext context) throws OperationFailedException {
                    ServiceTarget serviceTarget = context.getServiceTarget();

                    TransactionIntegrationService tiService = new TransactionIntegrationService();

                    serviceTarget
                            .addService(ConnectorServices.TRANSACTION_INTEGRATION_SERVICE, tiService)
                            .addDependency(TxnServices.JBOSS_TXN_TRANSACTION_MANAGER, TransactionManager.class,
                                    tiService.getTmInjector())
                            .addDependency(TxnServices.JBOSS_TXN_SYNCHRONIZATION_REGISTRY,
                                    TransactionSynchronizationRegistry.class, tiService.getTsrInjector())
                            .addDependency(TxnServices.JBOSS_TXN_USER_TRANSACTION_REGISTRY,
                                    org.jboss.tm.usertx.UserTransactionRegistry.class, tiService.getUtrInjector())
                            .addDependency(TxnServices.JBOSS_TXN_XA_TERMINATOR, JBossXATerminator.class,
                                    tiService.getTerminatorInjector())
                            .addDependency(TxnServices.JBOSS_TXN_ARJUNA_RECOVERY_MANAGER, XAResourceRecoveryRegistry.class,
                                    tiService.getRrInjector()).setInitialMode(Mode.ACTIVE).install();

                    CcmService ccmService = new CcmService(ccmDebug, ccmError);
                    serviceTarget
                            .addService(ConnectorServices.CCM_SERVICE, ccmService)
                            .addDependency(ConnectorServices.TRANSACTION_INTEGRATION_SERVICE, TransactionIntegration.class,
                                    ccmService.getTransactionIntegrationInjector()).install();

                    WorkManager wm = new WorkManagerImpl();

                    final WorkManagerService wmService = new WorkManagerService(wm);
                    serviceTarget
                            .addService(ConnectorServices.WORKMANAGER_SERVICE, wmService)
                            .addDependency(ThreadsServices.EXECUTOR.append(shortRunningThreadPool), Executor.class,
                                    wmService.getExecutorShortInjector())
                            .addDependency(ThreadsServices.EXECUTOR.append(longRunningThreadPool), Executor.class,
                                    wmService.getExecutorLongInjector())
                            .addDependency(TxnServices.JBOSS_TXN_XA_TERMINATOR, JBossXATerminator.class,
                                    wmService.getXaTerminatorInjector()).setInitialMode(Mode.ACTIVE).install();

                    CloneableBootstrapContext ctx = new BaseCloneableBootstrapContext();
                    final DefaultBootStrapContextService defaultBootCtxService = new DefaultBootStrapContextService(ctx);
                    serviceTarget
                            .addService(ConnectorServices.DEFAULT_BOOTSTRAP_CONTEXT_SERVICE, defaultBootCtxService)
                            .addDependency(ConnectorServices.WORKMANAGER_SERVICE, WorkManager.class,
                                    defaultBootCtxService.getWorkManagerValueInjector())
                            .addDependency(TxnServices.JBOSS_TXN_XA_TERMINATOR, JBossXATerminator.class,
                                    defaultBootCtxService.getXaTerminatorInjector())
                            .addDependency(TxnServices.JBOSS_TXN_ARJUNA_TRANSACTION_MANAGER,
                                    com.arjuna.ats.jbossatx.jta.TransactionManagerService.class,
                                    defaultBootCtxService.getTxManagerInjector()).setInitialMode(Mode.ACTIVE).install();
                    final JcaSubsystemConfiguration config = new JcaSubsystemConfiguration();

                    config.setArchiveValidation(archiveValidationEnabled);
                    config.setArchiveValidationFailOnError(failOnError);
                    config.setArchiveValidationFailOnWarn(failOnWarn);

                    // FIXME Bean validation currently not used
                    config.setBeanValidation(false);

                    final JcaConfigService connectorConfigService = new JcaConfigService(config);
                    serviceTarget
                            .addService(ConnectorServices.CONNECTOR_CONFIG_SERVICE, connectorConfigService)
                            .addDependency(ConnectorServices.DEFAULT_BOOTSTRAP_CONTEXT_SERVICE,
                                    CloneableBootstrapContext.class,
                                    connectorConfigService.getDefaultBootstrapContextInjector()).setInitialMode(Mode.ACTIVE)
                            .install();

                    // TODO does the install of this and the DriverProcessor
                    // belong in DataSourcesSubsystemAdd?
                    final DriverRegistryService driverRegistryService = new DriverRegistryService();
                    serviceTarget.addService(ConnectorServices.JDBC_DRIVER_REGISTRY_SERVICE, driverRegistryService).install();

                    new RaDeploymentActivator().activate(bootContext, serviceTarget);

                    resultHandler.handleResultComplete();
                }
            });

        } else {
            resultHandler.handleResultComplete();
        }

        // Compensating is remove
        final ModelNode compensating = new ModelNode();
        compensating.get(OP_ADDR).set(operation.require(ADDRESS));
        compensating.get(OP).set("remove");
        return new BasicOperationResult(compensating);
    }
}
