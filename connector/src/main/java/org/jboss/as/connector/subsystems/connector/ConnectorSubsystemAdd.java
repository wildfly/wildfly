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

package org.jboss.as.connector.subsystems.connector;

import static org.jboss.as.connector.subsystems.connector.Constants.ARCHIVE_VALIDATION_ENABLED;
import static org.jboss.as.connector.subsystems.connector.Constants.ARCHIVE_VALIDATION_FAIL_ON_ERROR;
import static org.jboss.as.connector.subsystems.connector.Constants.ARCHIVE_VALIDATION_FAIL_ON_WARN;
import static org.jboss.as.connector.subsystems.connector.Constants.BEAN_VALIDATION_ENABLED;
import static org.jboss.as.connector.subsystems.connector.Constants.DEFAULT_WORKMANAGER_LONG_RUNNING_THREAD_POOL;
import static org.jboss.as.connector.subsystems.connector.Constants.DEFAULT_WORKMANAGER_SHORT_RUNNING_THREAD_POOL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADDRESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.util.concurrent.Executor;

import org.jboss.as.connector.ConnectorServices;
import org.jboss.as.connector.bootstrap.DefaultBootStrapContextService;
import org.jboss.as.connector.deployers.RaDeploymentActivator;
import org.jboss.as.connector.workmanager.WorkManagerService;
import org.jboss.as.controller.Cancellable;
import org.jboss.as.controller.ModelAddOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationHandler;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.server.BootOperationHandler;
import org.jboss.as.server.BootOperationContext;
import org.jboss.as.threads.ThreadsServices;
import org.jboss.as.txn.TxnServices;
import org.jboss.dmr.ModelNode;
import org.jboss.jca.core.api.bootstrap.CloneableBootstrapContext;
import org.jboss.jca.core.api.workmanager.WorkManager;
import org.jboss.jca.core.bootstrapcontext.BaseCloneableBootstrapContext;
import org.jboss.jca.core.workmanager.WorkManagerImpl;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.tm.JBossXATerminator;

/**
 * @author @author <a href="mailto:stefano.maestri@redhat.com">Stefano
 *         Maestri</a>
 */
class ConnectorSubsystemAdd implements ModelAddOperationHandler, BootOperationHandler {

    static final OperationHandler INSTANCE = new ConnectorSubsystemAdd();

    /** {@inheritDoc} */
    @Override
    public Cancellable execute(OperationContext context, ModelNode operation, ResultHandler resultHandler) {

        String shortRunningThreadPool = operation.get(DEFAULT_WORKMANAGER_SHORT_RUNNING_THREAD_POOL).asString();
        String longRunningThreadPool = operation.get(DEFAULT_WORKMANAGER_LONG_RUNNING_THREAD_POOL).asString();
        boolean beanValidationEnabled = ParamsUtils.parseBooleanParameter(operation, BEAN_VALIDATION_ENABLED, false);
        boolean archiveValidationEnabled = ParamsUtils.parseBooleanParameter(operation, ARCHIVE_VALIDATION_ENABLED, false);
        boolean failOnError = ParamsUtils.parseBooleanParameter(operation, ARCHIVE_VALIDATION_FAIL_ON_ERROR, true);
        boolean failOnWarn = ParamsUtils.parseBooleanParameter(operation, ARCHIVE_VALIDATION_FAIL_ON_WARN, false);

        if (context instanceof BootOperationContext) {
            BootOperationContext updateContext = (BootOperationContext) context;
            ServiceTarget serviceTarget = updateContext.getServiceTarget();
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
            final ConnectorSubsystemConfiguration config = new ConnectorSubsystemConfiguration();

            config.setArchiveValidation(archiveValidationEnabled);
            config.setArchiveValidationFailOnError(failOnError);
            config.setArchiveValidationFailOnWarn(failOnWarn);

            // FIXME Bean validation currently not used
            config.setBeanValidation(false);

            final ConnectorConfigService connectorConfigService = new ConnectorConfigService(config);

            serviceTarget
                    .addService(ConnectorServices.CONNECTOR_CONFIG_SERVICE, connectorConfigService)
                    .addDependency(ConnectorServices.DEFAULT_BOOTSTRAP_CONTEXT_SERVICE, CloneableBootstrapContext.class,
                            connectorConfigService.getDefaultBootstrapContextInjector()).setInitialMode(Mode.ACTIVE).install();

            new RaDeploymentActivator().activate(updateContext);
        }

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

        // Compensating is remove
        final ModelNode compensating = new ModelNode();
        compensating.get(OP_ADDR).set(operation.require(ADDRESS));
        compensating.get(OP).set("remove");

        resultHandler.handleResultComplete(compensating);

        return Cancellable.NULL;
    }

}
