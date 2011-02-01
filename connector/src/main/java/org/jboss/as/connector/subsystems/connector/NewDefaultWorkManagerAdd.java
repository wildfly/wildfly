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

import static org.jboss.as.connector.subsystems.connector.Constants.LONG_RUNNING_THREAD_POOL;
import static org.jboss.as.connector.subsystems.connector.Constants.SHORT_RUNNING_THREAD_POOL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADDRESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;

import java.util.concurrent.Executor;

import org.jboss.as.connector.ConnectorServices;
import org.jboss.as.connector.bootstrap.DefaultBootStrapContextService;
import org.jboss.as.connector.workmanager.WorkManagerService;
import org.jboss.as.controller.Cancellable;
import org.jboss.as.controller.ModelAddOperationHandler;
import org.jboss.as.controller.NewOperationContext;
import org.jboss.as.controller.OperationHandler;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.server.NewRuntimeOperationContext;
import org.jboss.as.server.RuntimeOperationHandler;
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
public class NewDefaultWorkManagerAdd implements RuntimeOperationHandler, ModelAddOperationHandler {

    static final OperationHandler INSTANCE = new NewDefaultWorkManagerAdd();

    @Override
    public Cancellable execute(final NewOperationContext context, final ModelNode operation, final ResultHandler resultHandler) {

        String shortRunningThreadPool = NewParamsUtils.parseStringParameter(operation, SHORT_RUNNING_THREAD_POOL);
        String longRunningThreadPool = NewParamsUtils.parseStringParameter(operation, LONG_RUNNING_THREAD_POOL);

        if (context instanceof NewRuntimeOperationContext) {
            ServiceTarget serviceTarget = ((NewRuntimeOperationContext) context).getServiceTarget();
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

        }

        // Apply to the model
        final ModelNode model = context.getSubModel();

        if (shortRunningThreadPool != null) {
            model.get(SHORT_RUNNING_THREAD_POOL).set(shortRunningThreadPool);

        }
        if (longRunningThreadPool != null) {
            model.get(LONG_RUNNING_THREAD_POOL).set(longRunningThreadPool);

        }
        // Compensating is remove
        final ModelNode compensating = new ModelNode();
        compensating.get(OP_ADDR).set(operation.require(ADDRESS));
        compensating.get(OP).set(REMOVE);
        resultHandler.handleResultComplete(compensating);

        return Cancellable.NULL;
    }
}
