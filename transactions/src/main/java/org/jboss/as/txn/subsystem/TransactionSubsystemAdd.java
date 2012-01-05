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

package org.jboss.as.txn.subsystem;

import java.util.List;

import javax.transaction.TransactionSynchronizationRegistry;

import com.arjuna.ats.internal.arjuna.utils.UuidProcessId;
import com.arjuna.ats.jbossatx.jta.RecoveryManagerService;
import com.arjuna.ats.jts.common.jtsPropertyManager;
import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.ServiceBasedNamingStore;
import org.jboss.as.naming.ValueManagedReferenceFactory;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.service.BinderService;
import org.jboss.as.network.SocketBinding;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.as.server.services.path.RelativePathService;
import org.jboss.as.txn.deployment.TransactionJndiBindingProcessor;
import org.jboss.as.txn.deployment.TransactionLeakRollbackProcessor;
import org.jboss.as.txn.service.ArjunaObjectStoreEnvironmentService;
import org.jboss.as.txn.service.ArjunaRecoveryManagerService;
import org.jboss.as.txn.service.ArjunaTransactionManagerService;
import org.jboss.as.txn.service.CoreEnvironmentService;
import org.jboss.as.txn.service.TransactionManagerService;
import org.jboss.as.txn.service.TransactionSynchronizationRegistryService;
import org.jboss.as.txn.service.TxnServices;
import org.jboss.as.txn.service.UserTransactionRegistryService;
import org.jboss.as.txn.service.UserTransactionService;
import org.jboss.as.txn.service.XATerminatorService;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.inject.InjectionException;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.ImmediateValue;
import org.jboss.tm.JBossXATerminator;
import org.jboss.tm.usertx.UserTransactionRegistry;
import org.omg.CORBA.ORB;

import static org.jboss.as.txn.TransactionLogger.ROOT_LOGGER;
import static org.jboss.as.txn.subsystem.CommonAttributes.JTS;


/**
 * Adds the transaction management subsystem.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author Emanuel Muckenhuber
 * @author Scott Stark (sstark@redhat.com) (C) 2011 Red Hat Inc.
 */
class TransactionSubsystemAdd extends AbstractBoottimeAddStepHandler {

    static final TransactionSubsystemAdd INSTANCE = new TransactionSubsystemAdd();

    private static final ServiceName INTERNAL_CORE_ENV_VAR_PATH = TxnServices.JBOSS_TXN_PATHS.append("core-var-dir");
    private static final ServiceName INTERNAL_OBJECTSTORE_PATH = TxnServices.JBOSS_TXN_PATHS.append("object-store");


    private TransactionSubsystemAdd() {
        //
    }


    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        populateModelWithRecoveryEnvConfig(operation, model);

        populateModelWithCoreEnvConfig(operation, model);

        populateModelWithCoordinatorEnvConfig(operation, model);

        populateModelWithObjectStoreConfig(operation, model);

        TransactionSubsystemRootResourceDefinition.JTS.validateAndSet(operation, model);
    }

    private void populateModelWithObjectStoreConfig(ModelNode operation, ModelNode objectStoreModel) throws OperationFailedException {

        TransactionSubsystemRootResourceDefinition.OBJECT_STORE_RELATIVE_TO.validateAndSet(operation, objectStoreModel);
        TransactionSubsystemRootResourceDefinition.OBJECT_STORE_PATH.validateAndSet(operation, objectStoreModel);

    }

    private void populateModelWithCoordinatorEnvConfig(ModelNode operation, ModelNode coordEnvModel) throws OperationFailedException {
        TransactionSubsystemRootResourceDefinition.ENABLE_STATISTICS.validateAndSet(operation, coordEnvModel);
        TransactionSubsystemRootResourceDefinition.ENABLE_TSM_STATUS.validateAndSet(operation, coordEnvModel);
        TransactionSubsystemRootResourceDefinition.DEFAULT_TIMEOUT.validateAndSet(operation, coordEnvModel);
    }

    private void populateModelWithCoreEnvConfig(ModelNode operation, ModelNode model) throws OperationFailedException {
        //core environment
        TransactionSubsystemRootResourceDefinition.NODE_IDENTIFIER.validateAndSet(operation, model);
        TransactionSubsystemRootResourceDefinition.PATH.validateAndSet(operation, model);
        TransactionSubsystemRootResourceDefinition.RELATIVE_TO.validateAndSet(operation, model);

        // We have some complex logic for the 'process-id' stuff because of the alternatives
        if (operation.hasDefined(TransactionSubsystemRootResourceDefinition.PROCESS_ID_UUID.getName()) && operation.get(TransactionSubsystemRootResourceDefinition.PROCESS_ID_UUID.getName()).asBoolean()) {
            TransactionSubsystemRootResourceDefinition.PROCESS_ID_UUID.validateAndSet(operation, model);
            if (operation.hasDefined(TransactionSubsystemRootResourceDefinition.PROCESS_ID_SOCKET_BINDING.getName())) {
                throw new OperationFailedException(new ModelNode().set(String.format("%s must be undefined if %s is 'true'.",
                        TransactionSubsystemRootResourceDefinition.PROCESS_ID_SOCKET_BINDING.getName(), TransactionSubsystemRootResourceDefinition.PROCESS_ID_UUID.getName())));
            } else if (operation.hasDefined(TransactionSubsystemRootResourceDefinition.PROCESS_ID_SOCKET_MAX_PORTS.getName())) {
                throw new OperationFailedException(new ModelNode().set(String.format("%s must be undefined if %s is 'true'.",
                        TransactionSubsystemRootResourceDefinition.PROCESS_ID_SOCKET_MAX_PORTS.getName(), TransactionSubsystemRootResourceDefinition.PROCESS_ID_UUID.getName())));
            }
            //model.get(TransactionSubsystemRootResourceDefinition.PROCESS_ID_SOCKET_BINDING.getName());
            //model.get(TransactionSubsystemRootResourceDefinition.PROCESS_ID_SOCKET_MAX_PORTS.getName());
        } else if (operation.hasDefined(TransactionSubsystemRootResourceDefinition.PROCESS_ID_SOCKET_BINDING.getName())) {
            TransactionSubsystemRootResourceDefinition.PROCESS_ID_SOCKET_BINDING.validateAndSet(operation, model);
            TransactionSubsystemRootResourceDefinition.PROCESS_ID_SOCKET_MAX_PORTS.validateAndSet(operation, model);
            model.get(TransactionSubsystemRootResourceDefinition.PROCESS_ID_UUID.getName()).set(false);
        } else if (operation.hasDefined(TransactionSubsystemRootResourceDefinition.PROCESS_ID_SOCKET_MAX_PORTS.getName())) {
            throw new OperationFailedException(new ModelNode().set(String.format("%s must be defined if %s is defined.",
                    TransactionSubsystemRootResourceDefinition.PROCESS_ID_SOCKET_BINDING.getName(), TransactionSubsystemRootResourceDefinition.PROCESS_ID_SOCKET_MAX_PORTS.getName())));
        } else {
            // not uuid and also not sockets!
            throw new OperationFailedException(new ModelNode().set(String.format("Either %s must be 'true' or  %s must be defined.",
                    TransactionSubsystemRootResourceDefinition.PROCESS_ID_UUID.getName(), TransactionSubsystemRootResourceDefinition.PROCESS_ID_SOCKET_BINDING.getName())));
        }
    }

    private void populateModelWithRecoveryEnvConfig(ModelNode operation, ModelNode model) throws OperationFailedException {
        //recovery environment
        TransactionSubsystemRootResourceDefinition.BINDING.validateAndSet(operation, model);
        TransactionSubsystemRootResourceDefinition.STATUS_BINDING.validateAndSet(operation, model);
        TransactionSubsystemRootResourceDefinition.RECOVERY_LISTENER.validateAndSet(operation, model);
    }

    @Override
    protected void performBoottime(OperationContext context, ModelNode operation, ModelNode model,
                                   ServiceVerificationHandler verificationHandler,
                                   List<ServiceController<?>> controllers) throws OperationFailedException {

        boolean jts = model.hasDefined(JTS) && model.get(JTS).asBoolean();

        //recovery environment
        performRecoveryEnvBoottime(context, operation, model, verificationHandler, controllers, jts);

        //core environment
        performCoreEnvironmentBootTime(context, operation, model, verificationHandler, controllers);

        //coordinator environment
        performCoordinatorEnvBoottime(context, operation, model, verificationHandler, controllers, jts);

        //object store
        performObjectStoreBoottime(context, operation, model, verificationHandler, controllers);


        //always propagate the transaction context
        //TODO: need a better way to do this, but this value gets cached in a static
        //so we need to make sure we set it before anything tries to read it
        jtsPropertyManager.getJTSEnvironmentBean().setAlwaysPropagateContext(true);

        context.addStep(new AbstractDeploymentChainStep() {
            protected void execute(final DeploymentProcessorTarget processorTarget) {
                processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_TRANSACTION_ROLLBACK_ACTION, new TransactionLeakRollbackProcessor());
                processorTarget.addDeploymentProcessor(Phase.INSTALL, Phase.INSTALL_TRANSACTION_BINDINGS, new TransactionJndiBindingProcessor());
            }
        }, OperationContext.Stage.RUNTIME);

        final ServiceTarget target = context.getServiceTarget();

        //bind the TransactionManger and the TSR into JNDI
        final BinderService tmBinderService = new BinderService("TransactionManager");
        final ServiceBuilder<ManagedReferenceFactory> tmBuilder = context.getServiceTarget().addService(ContextNames.JBOSS_CONTEXT_SERVICE_NAME.append("TransactionManager"), tmBinderService);
        tmBuilder.addDependency(ContextNames.JBOSS_CONTEXT_SERVICE_NAME, ServiceBasedNamingStore.class, tmBinderService.getNamingStoreInjector());
        tmBuilder.addDependency(TransactionManagerService.SERVICE_NAME, javax.transaction.TransactionManager.class, new Injector<javax.transaction.TransactionManager>() {
            @Override
            public void inject(final javax.transaction.TransactionManager value) throws InjectionException {
                tmBinderService.getManagedObjectInjector().inject(new ValueManagedReferenceFactory(new ImmediateValue<Object>(value)));
            }

            @Override
            public void uninject() {
                tmBinderService.getManagedObjectInjector().uninject();
            }
        });
        tmBuilder.addListener(verificationHandler);
        controllers.add(tmBuilder.install());

        final BinderService tmLegacyBinderService = new BinderService("TransactionManager");
        final ServiceBuilder<ManagedReferenceFactory> tmLegacyBuilder = context.getServiceTarget().addService(ContextNames.JAVA_CONTEXT_SERVICE_NAME.append("TransactionManager"), tmLegacyBinderService);
        tmLegacyBuilder.addDependency(ContextNames.JAVA_CONTEXT_SERVICE_NAME, ServiceBasedNamingStore.class, tmLegacyBinderService.getNamingStoreInjector());
        tmLegacyBuilder.addDependency(TransactionManagerService.SERVICE_NAME, javax.transaction.TransactionManager.class, new Injector<javax.transaction.TransactionManager>() {
            @Override
            public void inject(final javax.transaction.TransactionManager value) throws InjectionException {
                tmLegacyBinderService.getManagedObjectInjector().inject(new ValueManagedReferenceFactory(new ImmediateValue<Object>(value)));
            }

            @Override
            public void uninject() {
                tmLegacyBinderService.getManagedObjectInjector().uninject();
            }
        });
        tmLegacyBuilder.addListener(verificationHandler);
        controllers.add(tmLegacyBuilder.install());

        final BinderService tsrBinderService = new BinderService("TransactionSynchronizationRegistry");
        final ServiceBuilder<ManagedReferenceFactory> tsrBuilder = context.getServiceTarget().addService(ContextNames.JBOSS_CONTEXT_SERVICE_NAME.append("TransactionSynchronizationRegistry"), tsrBinderService);
        tsrBuilder.addDependency(ContextNames.JBOSS_CONTEXT_SERVICE_NAME, ServiceBasedNamingStore.class, tsrBinderService.getNamingStoreInjector());
        tsrBuilder.addDependency(TransactionSynchronizationRegistryService.SERVICE_NAME, TransactionSynchronizationRegistry.class, new Injector<TransactionSynchronizationRegistry>() {
            @Override
            public void inject(final TransactionSynchronizationRegistry value) throws InjectionException {
                tsrBinderService.getManagedObjectInjector().inject(new ValueManagedReferenceFactory(new ImmediateValue<Object>(value)));
            }

            @Override
            public void uninject() {
                tsrBinderService.getManagedObjectInjector().uninject();
            }
        });
        tsrBuilder.addListener(verificationHandler);
        controllers.add(tsrBuilder.install());

        // Bind the UserTransaction into JNDI
        final BinderService utBinderService = new BinderService("UserTransaction");
        final ServiceBuilder<ManagedReferenceFactory> utBuilder = context.getServiceTarget().addService(ContextNames.JBOSS_CONTEXT_SERVICE_NAME.append("UserTransaction"), utBinderService);
        utBuilder.addDependency(ContextNames.JBOSS_CONTEXT_SERVICE_NAME, ServiceBasedNamingStore.class, utBinderService.getNamingStoreInjector());
        utBuilder.addDependency(UserTransactionService.SERVICE_NAME, javax.transaction.UserTransaction.class, new Injector<javax.transaction.UserTransaction>() {
            @Override
            public void inject(final javax.transaction.UserTransaction value) throws InjectionException {
                utBinderService.getManagedObjectInjector().inject(new ValueManagedReferenceFactory(new ImmediateValue<Object>(value)));
            }

            @Override
            public void uninject() {
                utBinderService.getManagedObjectInjector().uninject();
            }
        });
        utBuilder.addListener(verificationHandler);
        controllers.add(utBuilder.install());


    }

    private void performObjectStoreBoottime(OperationContext context, ModelNode operation, ModelNode recoveryEnvModel,
                                            ServiceVerificationHandler verificationHandler,
                                            List<ServiceController<?>> controllers) throws OperationFailedException {

        final String objectStorePathRef =TransactionSubsystemRootResourceDefinition.OBJECT_STORE_RELATIVE_TO.resolveModelAttribute(context, recoveryEnvModel).asString();
        final String objectStorePath = TransactionSubsystemRootResourceDefinition.OBJECT_STORE_PATH.resolveModelAttribute(context, recoveryEnvModel).asString();
        if (ROOT_LOGGER.isDebugEnabled()) {
            ROOT_LOGGER.debugf("objectStorePathRef=%s, objectStorePath=%s\n", objectStorePathRef, objectStorePath);
        }

        ServiceTarget target = context.getServiceTarget();
        // Configure the ObjectStoreEnvironmentBeans
        RelativePathService.addService(INTERNAL_OBJECTSTORE_PATH, objectStorePath, true, objectStorePathRef, target, controllers, verificationHandler);

        final boolean useHornetqJournalStore = "true".equals(System.getProperty("usehornetqstore")); // TODO wire to domain model instead.

        final ArjunaObjectStoreEnvironmentService objStoreEnvironmentService = new ArjunaObjectStoreEnvironmentService(useHornetqJournalStore);
        controllers.add(target.addService(TxnServices.JBOSS_TXN_ARJUNA_OBJECTSTORE_ENVIRONMENT, objStoreEnvironmentService)
                .addDependency(INTERNAL_OBJECTSTORE_PATH, String.class, objStoreEnvironmentService.getPathInjector())
                .addDependency(TxnServices.JBOSS_TXN_CORE_ENVIRONMENT)
                .addListener(verificationHandler).setInitialMode(ServiceController.Mode.ACTIVE).install());

        controllers.add(TransactionManagerService.addService(target, verificationHandler));
        controllers.add(UserTransactionService.addService(target, verificationHandler));
        controllers.add(target.addService(TxnServices.JBOSS_TXN_USER_TRANSACTION_REGISTRY, new UserTransactionRegistryService())
                .addListener(verificationHandler).setInitialMode(ServiceController.Mode.ACTIVE).install());
        controllers.add(TransactionSynchronizationRegistryService.addService(target, verificationHandler));

    }

    private void performCoreEnvironmentBootTime(OperationContext context, ModelNode operation, ModelNode coreEnvModel,
                                                ServiceVerificationHandler verificationHandler,
                                                List<ServiceController<?>> controllers) throws OperationFailedException {
        // Configure the core configuration.
        final String nodeIdentifier = TransactionSubsystemRootResourceDefinition.NODE_IDENTIFIER.resolveModelAttribute(context, coreEnvModel).asString();
        final CoreEnvironmentService coreEnvironmentService = new CoreEnvironmentService(nodeIdentifier);

        String socketBindingName = null;
        if (TransactionSubsystemRootResourceDefinition.PROCESS_ID_UUID.resolveModelAttribute(context, coreEnvModel).asBoolean()) {
            // Use the UUID based id
            UuidProcessId id = new UuidProcessId();
            coreEnvironmentService.setProcessImplementation(id);
        } else {
            // Use the socket process id
            coreEnvironmentService.setProcessImplementationClassName(ProcessIdType.SOCKET.getClazz());
            socketBindingName = TransactionSubsystemRootResourceDefinition.PROCESS_ID_SOCKET_BINDING.resolveModelAttribute(context, coreEnvModel).asString();
            int ports = TransactionSubsystemRootResourceDefinition.PROCESS_ID_SOCKET_MAX_PORTS.resolveModelAttribute(context, coreEnvModel).asInt();
            coreEnvironmentService.setSocketProcessIdMaxPorts(ports);
        }

        final String varDirPathRef = TransactionSubsystemRootResourceDefinition.RELATIVE_TO.resolveModelAttribute(context, coreEnvModel).asString();
        final String varDirPath = TransactionSubsystemRootResourceDefinition.PATH.resolveModelAttribute(context, coreEnvModel).asString();

        if (ROOT_LOGGER.isDebugEnabled()) {
            ROOT_LOGGER.debugf("nodeIdentifier=%s\n", nodeIdentifier);
            ROOT_LOGGER.debugf("varDirPathRef=%s, varDirPath=%s\n", varDirPathRef, varDirPath);
        }

        ServiceTarget target = context.getServiceTarget();
        RelativePathService.addService(INTERNAL_CORE_ENV_VAR_PATH, varDirPath, true, varDirPathRef, target, controllers, verificationHandler);

        final ServiceBuilder<?> coreEnvBuilder = context.getServiceTarget().addService(TxnServices.JBOSS_TXN_CORE_ENVIRONMENT, coreEnvironmentService);
        if (socketBindingName != null) {
            // Add a dependency on the socket id binding
            ServiceName bindingName = SocketBinding.JBOSS_BINDING_NAME.append(socketBindingName);
            coreEnvBuilder.addDependency(bindingName, SocketBinding.class, coreEnvironmentService.getSocketProcessBindingInjector());
        }
        controllers.add(coreEnvBuilder.addDependency(INTERNAL_CORE_ENV_VAR_PATH, String.class, coreEnvironmentService.getPathInjector())
                .addListener(verificationHandler)
                .setInitialMode(ServiceController.Mode.ACTIVE)
                .install());
    }

    private void performRecoveryEnvBoottime(OperationContext context, ModelNode operation, ModelNode model,
                                            ServiceVerificationHandler verificationHandler,
                                            List<ServiceController<?>> controllers,
                                            final boolean jts) throws OperationFailedException {
        //recovery environment
        final String recoveryBindingName = TransactionSubsystemRootResourceDefinition.BINDING.resolveModelAttribute(context, model).asString();
        final String recoveryStatusBindingName = TransactionSubsystemRootResourceDefinition.STATUS_BINDING.resolveModelAttribute(context, model).asString();
        final boolean recoveryListener = TransactionSubsystemRootResourceDefinition.RECOVERY_LISTENER.resolveModelAttribute(context, model).asBoolean();

        // XATerminator has no deps, so just add it in there
        final XATerminatorService xaTerminatorService = new XATerminatorService(jts);
        controllers.add(context.getServiceTarget().addService(TxnServices.JBOSS_TXN_XA_TERMINATOR, xaTerminatorService).setInitialMode(Mode.ACTIVE).install());


        final ArjunaRecoveryManagerService recoveryManagerService = new ArjunaRecoveryManagerService(recoveryListener, jts);
        final ServiceBuilder<RecoveryManagerService> recoveryManagerServiceServiceBuilder = context.getServiceTarget().addService(TxnServices.JBOSS_TXN_ARJUNA_RECOVERY_MANAGER, recoveryManagerService);

        if(jts) {
            recoveryManagerServiceServiceBuilder.addDependency(ServiceName.JBOSS.append("jacorb", "orb-service"), ORB.class, recoveryManagerService.getOrbInjector());
        }

        controllers.add(recoveryManagerServiceServiceBuilder
                .addDependency(SocketBinding.JBOSS_BINDING_NAME.append(recoveryBindingName), SocketBinding.class, recoveryManagerService.getRecoveryBindingInjector())
                .addDependency(SocketBinding.JBOSS_BINDING_NAME.append(recoveryStatusBindingName), SocketBinding.class, recoveryManagerService.getStatusBindingInjector())
                .addDependency(TxnServices.JBOSS_TXN_CORE_ENVIRONMENT)
                .addDependency(TxnServices.JBOSS_TXN_ARJUNA_OBJECTSTORE_ENVIRONMENT)
                .addListener(verificationHandler)
                .setInitialMode(ServiceController.Mode.ACTIVE)
                .install());
    }

    private void performCoordinatorEnvBoottime(OperationContext context, ModelNode operation, ModelNode coordEnvModel,
                                               ServiceVerificationHandler verificationHandler,
                                               List<ServiceController<?>> controllers, final boolean jts) throws OperationFailedException {

        final boolean coordinatorEnableStatistics = TransactionSubsystemRootResourceDefinition.ENABLE_STATISTICS.resolveModelAttribute(context, coordEnvModel).asBoolean();
        final boolean transactionStatusManagerEnable = TransactionSubsystemRootResourceDefinition.ENABLE_TSM_STATUS.resolveModelAttribute(context, coordEnvModel).asBoolean();
        final int coordinatorDefaultTimeout = TransactionSubsystemRootResourceDefinition.DEFAULT_TIMEOUT.resolveModelAttribute(context, coordEnvModel).asInt();

        final String nodeIdentifier = TransactionSubsystemRootResourceDefinition.NODE_IDENTIFIER.resolveModelAttribute(context, coordEnvModel).asString();


        final ArjunaTransactionManagerService transactionManagerService = new ArjunaTransactionManagerService(coordinatorEnableStatistics, coordinatorDefaultTimeout, transactionStatusManagerEnable, jts, nodeIdentifier);
        final ServiceBuilder<com.arjuna.ats.jbossatx.jta.TransactionManagerService> transactionManagerServiceServiceBuilder = context.getServiceTarget().addService(TxnServices.JBOSS_TXN_ARJUNA_TRANSACTION_MANAGER, transactionManagerService);

        //if jts is enabled we need the ORB
        if (jts) {
            transactionManagerServiceServiceBuilder.addDependency(ServiceName.JBOSS.append("jacorb", "orb-service"), ORB.class, transactionManagerService.getOrbInjector());
        }

        controllers.add(transactionManagerServiceServiceBuilder
                .addDependency(TxnServices.JBOSS_TXN_XA_TERMINATOR, JBossXATerminator.class, transactionManagerService.getXaTerminatorInjector())
                .addDependency(TxnServices.JBOSS_TXN_USER_TRANSACTION_REGISTRY, UserTransactionRegistry.class, transactionManagerService.getUserTransactionRegistry())
                .addDependency(TxnServices.JBOSS_TXN_CORE_ENVIRONMENT)
                .addDependency(TxnServices.JBOSS_TXN_ARJUNA_OBJECTSTORE_ENVIRONMENT)
                .addDependency(TxnServices.JBOSS_TXN_ARJUNA_RECOVERY_MANAGER)
                .addListener(verificationHandler)
                .setInitialMode(ServiceController.Mode.ACTIVE)
                .install());


    }


}
