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

import org.jboss.as.controller.BasicOperationResult;
import org.jboss.as.controller.ModelAddOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationResult;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.RuntimeTask;
import org.jboss.as.controller.RuntimeTaskContext;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.server.BootOperationContext;
import org.jboss.as.server.BootOperationHandler;
import org.jboss.as.server.deployment.Phase;
import org.jboss.as.server.services.net.SocketBinding;
import org.jboss.as.server.services.path.AbstractPathService;
import org.jboss.as.server.services.path.RelativePathService;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceBuilder.DependencyType;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.tm.JBossXATerminator;
import org.omg.CORBA.ORB;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.txn.CommonAttributes.*;

/**
 * Adds the transaction management subsystem.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author Emanuel Muckenhuber
 */
class TransactionSubsystemAdd implements ModelAddOperationHandler, BootOperationHandler {

    static final TransactionSubsystemAdd INSTANCE = new TransactionSubsystemAdd();
    private static final String INTERNAL_OBJECTSTORE_PATH = "jboss.transactions.object.store.path";

    private static final Logger log = Logger.getLogger("org.jboss.as.transactions");

    private TransactionSubsystemAdd() {
        //
    }

    /** {@inheritDoc} */
    @Override
    public OperationResult execute(final OperationContext context, final ModelNode operation, final ResultHandler resultHandler) {

        if(context instanceof BootOperationContext) {
            ((BootOperationContext) context).addDeploymentProcessor(Phase.INSTALL, Phase.INSTALL_TRANSACTION_BINDINGS, new TransactionJndiBindingProcessor());
        }

        final ModelNode compensatingOperation = Util.getResourceRemoveOperation(operation.require(OP_ADDR));

        final String bindingName = operation.get(CORE_ENVIRONMENT).require(BINDING).asString();
        final String recoveryBindingName = operation.get(RECOVERY_ENVIRONMENT).require(BINDING).asString();
        final String recoveryStatusBindingName = operation.get(RECOVERY_ENVIRONMENT).require(STATUS_BINDING).asString();
        final String nodeIdentifier = operation.get(CORE_ENVIRONMENT).has(NODE_IDENTIFIER) ? operation.get(CORE_ENVIRONMENT, NODE_IDENTIFIER).asString() : "1";
        final boolean coordinatorEnableStatistics = operation.get(COORDINATOR_ENVIRONMENT, ENABLE_STATISTICS).asBoolean(false);
        final ModelNode objectStore = operation.get(OBJECT_STORE);
        final String objectStorePathRef = objectStore.hasDefined(RELATIVE_TO) ? objectStore.get(RELATIVE_TO).asString() : "jboss.server.data.dir";
        final String objectStorePath = objectStore.hasDefined(PATH) ? objectStore.get(PATH).asString() : "tx-object-store";
        final int maxPorts = 10;
        final int coordinatorDefaultTimeout = operation.get(COORDINATOR_ENVIRONMENT, DEFAULT_TIMEOUT).asInt(300);

        final ModelNode subModel = context.getSubModel();
        subModel.get(CORE_ENVIRONMENT, BINDING).set(operation.get(CORE_ENVIRONMENT).require(BINDING));
        subModel.get(CORE_ENVIRONMENT, NODE_IDENTIFIER).set(operation.get(CORE_ENVIRONMENT, NODE_IDENTIFIER));
        subModel.get(RECOVERY_ENVIRONMENT, BINDING).set(operation.get(RECOVERY_ENVIRONMENT).require(BINDING));
        subModel.get(RECOVERY_ENVIRONMENT, STATUS_BINDING).set(operation.get(RECOVERY_ENVIRONMENT, STATUS_BINDING));
        subModel.get(COORDINATOR_ENVIRONMENT, ENABLE_STATISTICS).set(operation.get(COORDINATOR_ENVIRONMENT, ENABLE_STATISTICS));
        subModel.get(COORDINATOR_ENVIRONMENT, DEFAULT_TIMEOUT).set(operation.get(COORDINATOR_ENVIRONMENT, DEFAULT_TIMEOUT));
        subModel.get(OBJECT_STORE, RELATIVE_TO).set(operation.get(OBJECT_STORE, RELATIVE_TO));
        subModel.get(OBJECT_STORE, PATH).set(operation.get(OBJECT_STORE, PATH));

        if (context.getRuntimeContext() != null) {
            context.getRuntimeContext().setRuntimeTask(new RuntimeTask() {
                @Override
                public void execute(RuntimeTaskContext context) throws OperationFailedException {
                    final ServiceTarget target = context.getServiceTarget();

                    // XATerminator has no deps, so just add it in there
                    final XATerminatorService xaTerminatorService = new XATerminatorService();
                    target.addService(TxnServices.JBOSS_TXN_XA_TERMINATOR, xaTerminatorService).setInitialMode(Mode.ACTIVE).install();

                    // Configure the ObjectStoreEnvironmentBeans
                    final ArjunaObjectStoreEnvironmentService objStoreEnvironmentService = new ArjunaObjectStoreEnvironmentService();
                    target.addService(TxnServices.JBOSS_TXN_ARJUNA_OBJECTSTORE_ENVIRONMENT, objStoreEnvironmentService)
                        .addDependency(AbstractPathService.pathNameOf(INTERNAL_OBJECTSTORE_PATH), String.class, objStoreEnvironmentService.getPathInjector())
                        .setInitialMode(Mode.ACTIVE).install();

                    final ArjunaRecoveryManagerService recoveryManagerService = new ArjunaRecoveryManagerService();
                    target.addService(TxnServices.JBOSS_TXN_ARJUNA_RECOVERY_MANAGER, recoveryManagerService)
                        .addDependency(DependencyType.OPTIONAL, ServiceName.JBOSS.append("iiop", "orb"), ORB.class, recoveryManagerService.getOrbInjector())
                        .addDependency(SocketBinding.JBOSS_BINDING_NAME.append(recoveryBindingName), SocketBinding.class, recoveryManagerService.getRecoveryBindingInjector())
                        .addDependency(SocketBinding.JBOSS_BINDING_NAME.append(recoveryStatusBindingName), SocketBinding.class, recoveryManagerService.getStatusBindingInjector())
                        .addDependency(TxnServices.JBOSS_TXN_ARJUNA_OBJECTSTORE_ENVIRONMENT)
                        .setInitialMode(Mode.ACTIVE)
                        .install();

                    final ArjunaTransactionManagerService transactionManagerService = new ArjunaTransactionManagerService(nodeIdentifier, maxPorts, coordinatorEnableStatistics, coordinatorDefaultTimeout);
                    target.addService(TxnServices.JBOSS_TXN_ARJUNA_TRANSACTION_MANAGER, transactionManagerService)
                            .addDependency(DependencyType.OPTIONAL, ServiceName.JBOSS.append("iiop", "orb"), ORB.class, transactionManagerService.getOrbInjector())
                            .addDependency(TxnServices.JBOSS_TXN_XA_TERMINATOR, JBossXATerminator.class, transactionManagerService.getXaTerminatorInjector())
                            .addDependency(SocketBinding.JBOSS_BINDING_NAME.append(bindingName), SocketBinding.class, transactionManagerService.getSocketProcessBindingInjector())
                            .addDependency(TxnServices.JBOSS_TXN_ARJUNA_OBJECTSTORE_ENVIRONMENT)
                            .addDependency(TxnServices.JBOSS_TXN_ARJUNA_RECOVERY_MANAGER)
                            .setInitialMode(Mode.ACTIVE)
                            .install();

                    TransactionManagerService.addService(target);
                    UserTransactionService.addService(target);
                    target.addService(TxnServices.JBOSS_TXN_USER_TRANSACTION_REGISTRY, new UserTransactionRegistryService()).setInitialMode(Mode.ACTIVE).install();
                    TransactionSynchronizationRegistryService.addService(target);

                    RelativePathService.addService(INTERNAL_OBJECTSTORE_PATH, objectStorePath, objectStorePathRef, target);

                    //we need to initialize this class when we have the correct TCCL set
                    //so we force it to be initialized here
                    try {
                        Class.forName("com.arjuna.ats.internal.jta.transaction.arjunacore.TransactionImple",true,getClass().getClassLoader());
                    } catch (ClassNotFoundException e) {
                        log.warn("Could not load com.arjuna.ats.internal.jta.transaction.arjunacore.TransactionImple",e);
                    }

                    resultHandler.handleResultComplete();
                }
            });
        } else {
            resultHandler.handleResultComplete();
        }

        return new BasicOperationResult(compensatingOperation);
    }




}
