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

import com.arjuna.ats.internal.arjuna.utils.UuidProcessId;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.NamingStore;
import org.jboss.as.naming.ValueManagedReferenceFactory;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.service.BinderService;
import org.jboss.as.network.SocketBinding;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.as.server.services.path.RelativePathService;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.msc.inject.InjectionException;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceBuilder.DependencyType;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.ImmediateValue;
import org.jboss.tm.JBossXATerminator;
import org.omg.CORBA.ORB;

import javax.transaction.TransactionSynchronizationRegistry;
import java.util.ArrayList;
import java.util.List;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;
import static org.jboss.as.txn.CommonAttributes.*;


/**
 * Adds the transaction management subsystem.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author Emanuel Muckenhuber
 * @author Scott Stark (sstark@redhat.com) (C) 2011 Red Hat Inc.
 */
class TransactionSubsystemAdd implements OperationStepHandler {

    static final TransactionSubsystemAdd INSTANCE = new TransactionSubsystemAdd();
    private static final ServiceName INTERNAL_OBJECTSTORE_PATH = TxnServices.JBOSS_TXN_PATHS.append("object-store");
    private static final ServiceName INTERNAL_CORE_ENV_VAR_PATH = TxnServices.JBOSS_TXN_PATHS.append("core-var-dir");

    private static final Logger log = Logger.getLogger("org.jboss.as.transactions");

    private TransactionSubsystemAdd() {
        //
    }

    /**
     * {@inheritDoc}
     */
    public void execute(OperationContext context, ModelNode operation) {
        final ModelNode opAddr = operation.get(OP_ADDR);

        final String nodeIdentifier = operation.get(CORE_ENVIRONMENT).hasDefined(NODE_IDENTIFIER) ? operation.get(CORE_ENVIRONMENT, NODE_IDENTIFIER).asString() : "1";
        final ModelNode processId = operation.get(CORE_ENVIRONMENT).require(PROCESS_ID);
        final String varDirPathRef = operation.get(CORE_ENVIRONMENT).hasDefined(RELATIVE_TO) ? operation.get(CORE_ENVIRONMENT).get(RELATIVE_TO).asString() : "jboss.server.data.dir";
        final String varDirPath = operation.get(CORE_ENVIRONMENT).hasDefined(PATH) ? operation.get(CORE_ENVIRONMENT).get(PATH).asString() : "var";
        final String recoveryBindingName = operation.get(RECOVERY_ENVIRONMENT).require(BINDING).asString();
        final String recoveryStatusBindingName = operation.get(RECOVERY_ENVIRONMENT).require(STATUS_BINDING).asString();
        final boolean recoveryListener = operation.get(RECOVERY_ENVIRONMENT, RECOVERY_LISTENER).asBoolean(false);
        final boolean coordinatorEnableStatistics = operation.get(COORDINATOR_ENVIRONMENT, ENABLE_STATISTICS).asBoolean(false);
        final boolean transactionStatusManagerEnable = operation.get(COORDINATOR_ENVIRONMENT, ENABLE_TSM_STATUS).asBoolean(false);
        final ModelNode objectStore = operation.get(OBJECT_STORE);
        final String objectStorePathRef = objectStore.hasDefined(RELATIVE_TO) ? objectStore.get(RELATIVE_TO).asString() : "jboss.server.data.dir";
        final String objectStorePath = objectStore.hasDefined(PATH) ? objectStore.get(PATH).asString() : "tx-object-store";
        final int maxPorts = 10;
        final int coordinatorDefaultTimeout = operation.get(COORDINATOR_ENVIRONMENT, DEFAULT_TIMEOUT).asInt(300);
        if(log.isDebugEnabled()) {
            log.debugf("nodeIdentifier=%s\n", nodeIdentifier);
            log.debugf("varDirPathRef=%s, varDirPath=%s\n", varDirPathRef, varDirPath);
            log.debugf("objectStorePathRef=%s, objectStorePathRef=%s\n", objectStorePathRef, objectStorePath);
            log.debugf("recoveryBindingName=%s, recoveryStatusBindingName=%s\n", recoveryBindingName, recoveryStatusBindingName);
        }


        final ModelNode subModel = context.readModelForUpdate(PathAddress.EMPTY_ADDRESS);
        subModel.get(CORE_ENVIRONMENT, PROCESS_ID).set(operation.get(CORE_ENVIRONMENT).require(PROCESS_ID));
        subModel.get(CORE_ENVIRONMENT, NODE_IDENTIFIER).set(operation.get(CORE_ENVIRONMENT, NODE_IDENTIFIER));
        subModel.get(CORE_ENVIRONMENT, RELATIVE_TO).set(operation.get(CORE_ENVIRONMENT, RELATIVE_TO));
        subModel.get(CORE_ENVIRONMENT, PATH).set(operation.get(CORE_ENVIRONMENT, PATH));
        subModel.get(RECOVERY_ENVIRONMENT, BINDING).set(operation.get(RECOVERY_ENVIRONMENT).require(BINDING));
        subModel.get(RECOVERY_ENVIRONMENT, STATUS_BINDING).set(operation.get(RECOVERY_ENVIRONMENT, STATUS_BINDING));
        subModel.get(RECOVERY_ENVIRONMENT, RECOVERY_LISTENER).set(operation.get(RECOVERY_ENVIRONMENT, RECOVERY_LISTENER));
        subModel.get(COORDINATOR_ENVIRONMENT, ENABLE_STATISTICS).set(operation.get(COORDINATOR_ENVIRONMENT, ENABLE_STATISTICS));
        subModel.get(COORDINATOR_ENVIRONMENT, ENABLE_TSM_STATUS).set(operation.get(COORDINATOR_ENVIRONMENT, ENABLE_TSM_STATUS));
        subModel.get(COORDINATOR_ENVIRONMENT, DEFAULT_TIMEOUT).set(coordinatorDefaultTimeout);  // store the default so we write it -- TODO store all the defaults
        subModel.get(OBJECT_STORE, RELATIVE_TO).set(operation.get(OBJECT_STORE, RELATIVE_TO));
        subModel.get(OBJECT_STORE,PATH).set(operation.get(OBJECT_STORE, PATH));

        boolean setReload = false;
        if (context.getType() == OperationContext.Type.SERVER) {
            if(!context.isBooting()) {
                context.reloadRequired();
                setReload = true;
            } else {
                context.addStep(new AbstractDeploymentChainStep() {
                    protected void execute(DeploymentProcessorTarget processorTarget) {
                        processorTarget.addDeploymentProcessor(Phase.INSTALL, Phase.INSTALL_TRANSACTION_BINDINGS, new TransactionJndiBindingProcessor());
                    }
                }, OperationContext.Stage.RUNTIME);


                context.addStep(new OperationStepHandler() {
                        public void execute(OperationContext context, ModelNode operation) {
                            final List<ServiceController<?>> controllers = new ArrayList<ServiceController<?>>();
                            final ServiceVerificationHandler verificationHandler = new ServiceVerificationHandler();
                            final ServiceTarget target = context.getServiceTarget();

                            // Configure the core configuration.
                            String socketBindingName = null;
                            final CoreEnvironmentService coreEnvironmentService = new CoreEnvironmentService(nodeIdentifier, varDirPath);
                            if (processId.hasDefined(ProcessIdType.UUID.getName())) {
                                // Use the UUID based id
                                UuidProcessId id = new UuidProcessId();
                                coreEnvironmentService.setProcessImplementation(id);
                            } else if (processId.hasDefined(ProcessIdType.SOCKET.getName())) {
                                // Use the socket process id
                                coreEnvironmentService.setProcessImplementationClassName(ProcessIdType.SOCKET.getClazz());
                                ModelNode socket = processId.get(ProcessIdType.SOCKET.getName());
                                socketBindingName = socket.require(BINDING).asString();
                                if (socket.hasDefined(SOCKET_PROCESS_ID_MAX_PORTS)) {
                                    int ports = socket.get(SOCKET_PROCESS_ID_MAX_PORTS).asInt(maxPorts);
                                    coreEnvironmentService.setSocketProcessIdMaxPorts(ports);
                                }
                            } else {
                                // Default to UUID implementation
                                UuidProcessId id = new UuidProcessId();
                                coreEnvironmentService.setProcessImplementation(id);
                            }
                            ServiceBuilder<?> coreEnvBuilder = target.addService(TxnServices.JBOSS_TXN_CORE_ENVIRONMENT, coreEnvironmentService);
                            if (socketBindingName != null) {
                                // Add a dependency on the socket id binding
                                ServiceName bindingName = SocketBinding.JBOSS_BINDING_NAME.append(socketBindingName);
                                coreEnvBuilder.addDependency(bindingName, SocketBinding.class, coreEnvironmentService.getSocketProcessBindingInjector());
                            }
                            ServiceController<String> varDirRPS = RelativePathService.addService(INTERNAL_CORE_ENV_VAR_PATH, varDirPath, varDirPathRef, target);
                            controllers.add(varDirRPS);
                            controllers.add(coreEnvBuilder.addDependency(varDirRPS.getName(), String.class, coreEnvironmentService.getPathInjector())
                                    .addListener(verificationHandler)
                                    .setInitialMode(Mode.ACTIVE)
                                    .install());

                            // XATerminator has no deps, so just add it in there
                            final XATerminatorService xaTerminatorService = new XATerminatorService();
                            controllers.add(target.addService(TxnServices.JBOSS_TXN_XA_TERMINATOR, xaTerminatorService).setInitialMode(Mode.ACTIVE).install());

                            // Configure the ObjectStoreEnvironmentBeans
                            ServiceController<String> objectStoreRPS = RelativePathService.addService(INTERNAL_OBJECTSTORE_PATH, objectStorePath, objectStorePathRef, target);
                            controllers.add(objectStoreRPS);
                            final ArjunaObjectStoreEnvironmentService objStoreEnvironmentService = new ArjunaObjectStoreEnvironmentService();
                            controllers.add(target.addService(TxnServices.JBOSS_TXN_ARJUNA_OBJECTSTORE_ENVIRONMENT, objStoreEnvironmentService)
                                    .addDependency(objectStoreRPS.getName(), String.class, objStoreEnvironmentService.getPathInjector())
                                    .addDependency(TxnServices.JBOSS_TXN_CORE_ENVIRONMENT)
                                    .addListener(verificationHandler).setInitialMode(Mode.ACTIVE).install());

                            final ArjunaRecoveryManagerService recoveryManagerService = new ArjunaRecoveryManagerService(recoveryListener);
                            controllers.add(target.addService(TxnServices.JBOSS_TXN_ARJUNA_RECOVERY_MANAGER, recoveryManagerService)
                                    .addDependency(DependencyType.OPTIONAL, ServiceName.JBOSS.append("iiop", "orb"), ORB.class, recoveryManagerService.getOrbInjector())
                                    .addDependency(SocketBinding.JBOSS_BINDING_NAME.append(recoveryBindingName), SocketBinding.class, recoveryManagerService.getRecoveryBindingInjector())
                                    .addDependency(SocketBinding.JBOSS_BINDING_NAME.append(recoveryStatusBindingName), SocketBinding.class, recoveryManagerService.getStatusBindingInjector())
                                    .addDependency(TxnServices.JBOSS_TXN_CORE_ENVIRONMENT)
                                    .addDependency(TxnServices.JBOSS_TXN_ARJUNA_OBJECTSTORE_ENVIRONMENT)
                                    .addListener(verificationHandler)
                                    .setInitialMode(Mode.ACTIVE)
                                    .install());

                            final ArjunaTransactionManagerService transactionManagerService = new ArjunaTransactionManagerService(coordinatorEnableStatistics, coordinatorDefaultTimeout, transactionStatusManagerEnable);
                            controllers.add(target.addService(TxnServices.JBOSS_TXN_ARJUNA_TRANSACTION_MANAGER, transactionManagerService)
                                    .addDependency(DependencyType.OPTIONAL, ServiceName.JBOSS.append("iiop", "orb"), ORB.class, transactionManagerService.getOrbInjector())
                                    .addDependency(TxnServices.JBOSS_TXN_XA_TERMINATOR, JBossXATerminator.class, transactionManagerService.getXaTerminatorInjector())
                                    .addDependency(TxnServices.JBOSS_TXN_CORE_ENVIRONMENT)
                                    .addDependency(TxnServices.JBOSS_TXN_ARJUNA_OBJECTSTORE_ENVIRONMENT)
                                    .addDependency(TxnServices.JBOSS_TXN_ARJUNA_RECOVERY_MANAGER)
                                    .addListener(verificationHandler)
                                    .setInitialMode(Mode.ACTIVE)
                                    .install());

                            controllers.add(TransactionManagerService.addService(target, verificationHandler));
                            controllers.add(UserTransactionService.addService(target, verificationHandler));
                            controllers.add(target.addService(TxnServices.JBOSS_TXN_USER_TRANSACTION_REGISTRY, new UserTransactionRegistryService())
                                    .addListener(verificationHandler).setInitialMode(Mode.ACTIVE).install());
                            controllers.add(TransactionSynchronizationRegistryService.addService(target, verificationHandler));

                            //bind the TransactionManger and the TSR into JNDI
                            final BinderService tmBinderService = new BinderService("java:jboss/TransactionManager");
                            final ServiceBuilder<ManagedReferenceFactory> tmBuilder = context.getServiceTarget().addService(ContextNames.JBOSS_CONTEXT_SERVICE_NAME.append("TransactionManager"), tmBinderService);
                            tmBuilder.addAliases(ContextNames.JAVA_CONTEXT_SERVICE_NAME.append("java:jboss/TransactionManager"));
                            tmBuilder.addDependency(ContextNames.JBOSS_CONTEXT_SERVICE_NAME, NamingStore.class, tmBinderService.getNamingStoreInjector());
                            tmBuilder.addDependency(TransactionManagerService.SERVICE_NAME, javax.transaction.TransactionManager.class, new Injector<javax.transaction.TransactionManager>() {
                                @Override
                                public void inject(final javax.transaction.TransactionManager value) throws InjectionException {
                                    tmBinderService.getManagedObjectInjector().inject(new ValueManagedReferenceFactory(new ImmediateValue<Object>(value)));
                                }

                                @Override
                                public void uninject() {
                                    tmBinderService.getNamingStoreInjector().uninject();
                                }
                            });
                            tmBuilder.install();

                            final BinderService tsrBinderService = new BinderService("java:jboss/TransactionSynchronizationRegistry");
                            final ServiceBuilder<ManagedReferenceFactory> tsrBuilder = context.getServiceTarget().addService(ContextNames.JBOSS_CONTEXT_SERVICE_NAME.append("TransactionSynchronizationRegistry"), tsrBinderService);
                            tsrBuilder.addDependency(ContextNames.JBOSS_CONTEXT_SERVICE_NAME, NamingStore.class, tsrBinderService.getNamingStoreInjector());
                            tsrBuilder.addAliases(ContextNames.JAVA_CONTEXT_SERVICE_NAME.append("java:jboss/TransactionSynchronizationRegistry"));
                            tsrBuilder.addDependency(TransactionSynchronizationRegistryService.SERVICE_NAME, TransactionSynchronizationRegistry.class, new Injector<TransactionSynchronizationRegistry>() {
                                @Override
                                public void inject(final TransactionSynchronizationRegistry value) throws InjectionException {
                                    tsrBinderService.getManagedObjectInjector().inject(new ValueManagedReferenceFactory(new ImmediateValue<Object>(value)));
                                }

                                @Override
                                public void uninject() {
                                    tsrBinderService.getNamingStoreInjector().uninject();
                                }
                            });
                            tsrBuilder.install();

                            //we need to initialize this class when we have the correct TCCL set
                            //so we force it to be initialized here
                            try {
                                Class.forName("com.arjuna.ats.internal.jta.transaction.arjunacore.TransactionImple", true, getClass().getClassLoader());
                            } catch (ClassNotFoundException e) {
                                log.warn("Could not load com.arjuna.ats.internal.jta.transaction.arjunacore.TransactionImple", e);
                            }

                            context.addStep(verificationHandler, OperationContext.Stage.VERIFY);

                            if (context.completeStep() == OperationContext.ResultAction.ROLLBACK) {
                                for (ServiceController<?> controller : controllers) {
                                    context.removeService(controller.getName());
                                }
                            }
                        }
                    }, OperationContext.Stage.RUNTIME);
            }
        }

        if (context.completeStep() == OperationContext.ResultAction.ROLLBACK && setReload) {
            context.revertReloadRequired();
        }
    }

}
