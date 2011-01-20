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

package org.jboss.as.txn;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.txn.CommonAttributes.*;

import org.jboss.as.controller.Cancellable;
import org.jboss.as.controller.ModelAddOperationHandler;
import org.jboss.as.controller.NewOperationContext;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.server.NewRuntimeOperationContext;
import org.jboss.as.server.RuntimeOperationHandler;
import org.jboss.as.server.services.net.SocketBinding;
import org.jboss.as.server.services.path.AbstractPathService;
import org.jboss.as.server.services.path.RelativePathService;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder.DependencyType;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.tm.JBossXATerminator;
import org.omg.CORBA.ORB;

/**
 * @author Emanuel Muckenhuber
 */
class NewTransactionSubsystemAdd implements ModelAddOperationHandler, RuntimeOperationHandler {

    static final NewTransactionSubsystemAdd INSTANCE = new NewTransactionSubsystemAdd();
    private static final String INTERNAL_OBJECTSTORE_PATH = "jboss.transactions.object.store.path";

    private NewTransactionSubsystemAdd() {
        //
    }

    /** {@inheritDoc} */
    public Cancellable execute(NewOperationContext context, ModelNode operation, ResultHandler resultHandler) {

        final ModelNode compensatingOperation = new ModelNode();
        compensatingOperation.get(OP).set(REMOVE);
        compensatingOperation.get(OP_ADDR).set(operation.require(OP_ADDR));

        String bindingName = operation.get(CORE_ENVIRONMENT).require(BINDING).asString();
        String recoveryBindingName = operation.get(RECOVERY_ENVIRONMENT).require(BINDING).asString();
        String recoveryStatusBindingName = operation.get(RECOVERY_ENVIRONMENT).require(STATUS_BINDING).asString();
        String nodeIdentifier = operation.get(CORE_ENVIRONMENT).has(NODE_IDENTIFIER) ? operation.get(CORE_ENVIRONMENT, NODE_IDENTIFIER).asString() : "1";
        boolean coordinatorEnableStatistics = operation.get(COORDINATOR_ENVIRONMENT, ENABLE_STATISTICS).asBoolean();
        String objectStorePathRef = "jboss.server.data.dir";
        String objectStorePath = "tx-object-store";
        int maxPorts = 10;
        int coordinatorDefaultTimeout = 300;

        if(context instanceof NewRuntimeOperationContext) {
            final NewRuntimeOperationContext updateContext = (NewRuntimeOperationContext) context;
            final ServiceTarget target = updateContext.getServiceTarget();

            // XATerminator has no deps, so just add it in there
            final XATerminatorService xaTerminatorService = new XATerminatorService();
            target.addService(TxnServices.JBOSS_TXN_XA_TERMINATOR, xaTerminatorService).setInitialMode(Mode.ACTIVE).install();

            final ArjunaTransactionManagerService transactionManagerService = new ArjunaTransactionManagerService(nodeIdentifier, maxPorts, coordinatorEnableStatistics, coordinatorDefaultTimeout);
            target.addService(TxnServices.JBOSS_TXN_ARJUNA_TRANSACTION_MANAGER, transactionManagerService)
                .addDependency(DependencyType.OPTIONAL, ServiceName.JBOSS.append("iiop", "orb"), ORB.class, transactionManagerService.getOrbInjector())
                .addDependency(TxnServices.JBOSS_TXN_XA_TERMINATOR, JBossXATerminator.class, transactionManagerService.getXaTerminatorInjector())
                .addDependency(SocketBinding.JBOSS_BINDING_NAME.append(recoveryBindingName), SocketBinding.class, transactionManagerService.getRecoveryBindingInjector())
                .addDependency(SocketBinding.JBOSS_BINDING_NAME.append(recoveryStatusBindingName), SocketBinding.class, transactionManagerService.getStatusBindingInjector())
                .addDependency(SocketBinding.JBOSS_BINDING_NAME.append(bindingName), SocketBinding.class, transactionManagerService.getSocketProcessBindingInjector())
                .addDependency(AbstractPathService.pathNameOf(INTERNAL_OBJECTSTORE_PATH), String.class, transactionManagerService.getPathInjector())
                .setInitialMode(Mode.ACTIVE)
                .install();

            TransactionManagerService.addService(target);
            UserTransactionService.addService(target);

            RelativePathService.addService(INTERNAL_OBJECTSTORE_PATH, objectStorePath, objectStorePathRef, target);
        }

        final ModelNode subModel = context.getSubModel();
        subModel.get(CORE_ENVIRONMENT, BINDING).set(operation.get(CORE_ENVIRONMENT).require(BINDING));
        subModel.get(CORE_ENVIRONMENT, NODE_IDENTIFIER).set(operation.get(CORE_ENVIRONMENT, NODE_IDENTIFIER));
        subModel.get(RECOVERY_ENVIRONMENT, BINDING).set(operation.get(RECOVERY_ENVIRONMENT).require(BINDING));
        subModel.get(RECOVERY_ENVIRONMENT, STATUS_BINDING).set(operation.get(RECOVERY_ENVIRONMENT, STATUS_BINDING));
        subModel.get(COORDINATOR_ENVIRONMENT, ENABLE_STATISTICS).set(operation.get(COORDINATOR_ENVIRONMENT, ENABLE_STATISTICS));

        resultHandler.handleResultComplete(compensatingOperation);

        return Cancellable.NULL;
    }

}
