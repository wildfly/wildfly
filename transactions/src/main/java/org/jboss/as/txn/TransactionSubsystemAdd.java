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

import static org.jboss.as.txn.TransactionLogger.ROOT_LOGGER;

import java.util.List;
import java.util.Locale;

import javax.transaction.TransactionSynchronizationRegistry;

import com.arjuna.ats.internal.arjuna.utils.UuidProcessId;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.ServiceBasedNamingStore;
import org.jboss.as.naming.ValueManagedReferenceFactory;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.service.BinderService;
import org.jboss.as.network.SocketBinding;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.as.server.services.path.AbsolutePathService;
import org.jboss.as.server.services.path.RelativePathService;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.inject.InjectionException;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.ImmediateValue;
import org.jboss.tm.JBossXATerminator;
import org.omg.CORBA.ORB;


/**
 * Adds the transaction management subsystem.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author Emanuel Muckenhuber
 * @author Scott Stark (sstark@redhat.com) (C) 2011 Red Hat Inc.
 */
class TransactionSubsystemAdd extends AbstractBoottimeAddStepHandler implements DescriptionProvider {

    static final TransactionSubsystemAdd INSTANCE = new TransactionSubsystemAdd();

    //recovery environment
    public static final SimpleAttributeDefinition BINDING = new SimpleAttributeDefinitionBuilder(CommonAttributes.BINDING, ModelType.STRING, false)
            .setValidator(new StringLengthValidator(1))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setXmlName(Attribute.BINDING.getLocalName())
            .build();

    public static final SimpleAttributeDefinition STATUS_BINDING = new SimpleAttributeDefinitionBuilder(CommonAttributes.STATUS_BINDING, ModelType.STRING, false)
            .setValidator(new StringLengthValidator(1))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setXmlName(Attribute.STATUS_BINDING.getLocalName())
            .build();

    public static final SimpleAttributeDefinition RECOVERY_LISTENER = new SimpleAttributeDefinitionBuilder(CommonAttributes.RECOVERY_LISTENER, ModelType.BOOLEAN, true)
            .setDefaultValue(new ModelNode().set(false))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setXmlName(Attribute.RECOVERY_LISTENER.getLocalName())
            .build();

    //core environment
    private static final ServiceName INTERNAL_CORE_ENV_VAR_PATH = TxnServices.JBOSS_TXN_PATHS.append("core-var-dir");

    public static final SimpleAttributeDefinition NODE_IDENTIFIER = new SimpleAttributeDefinitionBuilder(CommonAttributes.NODE_IDENTIFIER, ModelType.STRING, true)
            .setDefaultValue(new ModelNode().set("1"))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition PROCESS_ID_UUID = new SimpleAttributeDefinitionBuilder("process-id-uuid", ModelType.BOOLEAN, false)
            .setAlternatives("process-id-socket-binding")
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition PROCESS_ID_SOCKET_BINDING = new SimpleAttributeDefinitionBuilder("process-id-socket-binding", ModelType.STRING, false)
            .setValidator(new StringLengthValidator(1, true))
            .setAlternatives("process-id-uuid")
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setXmlName(Attribute.BINDING.getLocalName())
            .build();

    public static final SimpleAttributeDefinition PROCESS_ID_SOCKET_MAX_PORTS = new SimpleAttributeDefinitionBuilder("process-id-socket-max-ports", ModelType.INT, true)
            .setValidator(new IntRangeValidator(1, true))
            .setDefaultValue(new ModelNode().set(10))
            .setRequires("process-id-socket-binding")
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setXmlName(Attribute.SOCKET_PROCESS_ID_MAX_PORTS.getLocalName())
            .build();

    public static final SimpleAttributeDefinition RELATIVE_TO = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.RELATIVE_TO, ModelType.STRING, true)
            .setDefaultValue(new ModelNode().set("jboss.server.data.dir"))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition PATH = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.PATH, ModelType.STRING, true)
            .setDefaultValue(new ModelNode().set("var"))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    //coordinator environment
    public static final SimpleAttributeDefinition ENABLE_STATISTICS = new SimpleAttributeDefinitionBuilder(CommonAttributes.ENABLE_STATISTICS, ModelType.BOOLEAN, true)
            .setDefaultValue(new ModelNode().set(false))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)  // TODO should be runtime-changeable
            .setXmlName(Attribute.ENABLE_STATISTICS.getLocalName())
            .build();

    public static final SimpleAttributeDefinition ENABLE_TSM_STATUS = new SimpleAttributeDefinitionBuilder(CommonAttributes.ENABLE_TSM_STATUS, ModelType.BOOLEAN, true)
            .setDefaultValue(new ModelNode().set(false))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)  // TODO is this runtime-changeable?
            .setXmlName(Attribute.ENABLE_TSM_STATUS.getLocalName())
            .build();

    public static final SimpleAttributeDefinition DEFAULT_TIMEOUT = new SimpleAttributeDefinitionBuilder(CommonAttributes.DEFAULT_TIMEOUT, ModelType.INT, true)
            .setMeasurementUnit(MeasurementUnit.SECONDS)
            .setDefaultValue(new ModelNode().set(300))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)  // TODO is this runtime-changeable?
            .setXmlName(Attribute.DEFAULT_TIMEOUT.getLocalName())
            .build();

    //object store
    static final ServiceName INTERNAL_OBJECTSTORE_PATH = TxnServices.JBOSS_TXN_PATHS.append("object-store");

    public static final SimpleAttributeDefinition OBJECT_STORE_RELATIVE_TO = new SimpleAttributeDefinitionBuilder(CommonAttributes.OBJECT_STORE_RELATIVE_TO, ModelType.STRING, true)
            .setDefaultValue(new ModelNode().set("jboss.server.data.dir"))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setXmlName(Attribute.RELATIVE_TO.getLocalName())
            .build();

    public static final SimpleAttributeDefinition OBJECT_STORE_PATH = new SimpleAttributeDefinitionBuilder(CommonAttributes.OBJECT_STORE_PATH, ModelType.STRING, true)
            .setDefaultValue(new ModelNode().set("tx-object-store"))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setXmlName(Attribute.PATH.getLocalName())
            .build();


    private TransactionSubsystemAdd() {
        //
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        return Descriptions.getSubsystemAdd(locale);
    }


    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        populateModelWithRecoveryEnvConfig(operation, model);

        populateModelWithCoreEnvConfig(operation, model);

        populateModelWithCoordinatorEnvConfig(operation, model);

        populateModelWithObjectStoreConfig(operation, model);

    }

    private void populateModelWithObjectStoreConfig(ModelNode operation, ModelNode objectStoreModel) throws OperationFailedException {

        OBJECT_STORE_RELATIVE_TO.validateAndSet(operation, objectStoreModel);
        OBJECT_STORE_PATH.validateAndSet(operation, objectStoreModel);

    }

    private void populateModelWithCoordinatorEnvConfig(ModelNode operation, ModelNode coordEnvModel) throws OperationFailedException {
        ENABLE_STATISTICS.validateAndSet(operation, coordEnvModel);
        ENABLE_TSM_STATUS.validateAndSet(operation, coordEnvModel);
        DEFAULT_TIMEOUT.validateAndSet(operation, coordEnvModel);
    }

    private void populateModelWithCoreEnvConfig(ModelNode operation, ModelNode model) throws OperationFailedException {
        //core environment
        NODE_IDENTIFIER.validateAndSet(operation, model);
        PATH.validateAndSet(operation, model);
        RELATIVE_TO.validateAndSet(operation, model);

        // We have some complex logic for the 'process-id' stuff because of the alternatives
        if (operation.hasDefined(PROCESS_ID_UUID.getName()) && operation.get(PROCESS_ID_UUID.getName()).asBoolean()) {
            PROCESS_ID_UUID.validateAndSet(operation, model);
            if (operation.hasDefined(PROCESS_ID_SOCKET_BINDING.getName())) {
                throw new OperationFailedException(new ModelNode().set(String.format("%s must be undefined if %s is 'true'.",
                        PROCESS_ID_SOCKET_BINDING.getName(), PROCESS_ID_UUID.getName())));
            } else if (operation.hasDefined(PROCESS_ID_SOCKET_MAX_PORTS.getName())) {
                throw new OperationFailedException(new ModelNode().set(String.format("%s must be undefined if %s is 'true'.",
                        PROCESS_ID_SOCKET_MAX_PORTS.getName(), PROCESS_ID_UUID.getName())));
            }
            model.get(PROCESS_ID_SOCKET_BINDING.getName());
            model.get(PROCESS_ID_SOCKET_MAX_PORTS.getName());
        } else if (operation.hasDefined(PROCESS_ID_SOCKET_BINDING.getName())) {
            PROCESS_ID_SOCKET_BINDING.validateAndSet(operation, model);
            PROCESS_ID_SOCKET_MAX_PORTS.validateAndSet(operation, model);
            model.get(PROCESS_ID_UUID.getName()).set(false);
        } else if (operation.hasDefined(PROCESS_ID_SOCKET_MAX_PORTS.getName())) {
            throw new OperationFailedException(new ModelNode().set(String.format("%s must be defined if %s is defined.",
                    PROCESS_ID_SOCKET_BINDING.getName(), PROCESS_ID_SOCKET_MAX_PORTS.getName())));
        } else {
            // not uuid and also not sockets!
            throw new OperationFailedException(new ModelNode().set(String.format("Either %s must be 'true' or  %s must be defined.",
                    PROCESS_ID_UUID.getName(), PROCESS_ID_SOCKET_BINDING.getName())));
        }
    }

    private void populateModelWithRecoveryEnvConfig(ModelNode operation, ModelNode model) throws OperationFailedException {
        //recovery environment
        BINDING.validateAndSet(operation, model);
        STATUS_BINDING.validateAndSet(operation, model);
        RECOVERY_LISTENER.validateAndSet(operation, model);
    }

    @Override
    protected void performBoottime(OperationContext context, ModelNode operation, ModelNode model,
                                  ServiceVerificationHandler verificationHandler,
                                  List<ServiceController<?>> controllers) throws OperationFailedException {

        //recovery environment
        performRecoveryEnvBoottime(context, operation, model, verificationHandler, controllers);

        //core environment
        performCoreEnvironmentBootTime(context, operation, model, verificationHandler, controllers);

        //coordinator environment
        performCoordinatorEnvBoottime(context, operation, model, verificationHandler, controllers);

        //object store
        performObjectStoreBoottime(context, operation, model, verificationHandler, controllers);

        context.addStep(new AbstractDeploymentChainStep() {
            protected void execute(DeploymentProcessorTarget processorTarget) {
                processorTarget.addDeploymentProcessor(Phase.INSTALL, Phase.INSTALL_TRANSACTION_BINDINGS, new TransactionJndiBindingProcessor());
            }
        }, OperationContext.Stage.RUNTIME);

        final ServiceTarget target = context.getServiceTarget();

        // XATerminator has no deps, so just add it in there
        final XATerminatorService xaTerminatorService = new XATerminatorService();
        controllers.add(target.addService(TxnServices.JBOSS_TXN_XA_TERMINATOR, xaTerminatorService).setInitialMode(Mode.ACTIVE).install());


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
                tmBinderService.getNamingStoreInjector().uninject();
            }
        });
        tmBuilder.addListener(verificationHandler);
        controllers.add(tmBuilder.install());

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
                tsrBinderService.getNamingStoreInjector().uninject();
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
                utBinderService.getNamingStoreInjector().uninject();
            }
        });
        utBuilder.addListener(verificationHandler);
        controllers.add(utBuilder.install());


    }

    private void performObjectStoreBoottime(OperationContext context, ModelNode operation, ModelNode recoveryEnvModel,
                                  ServiceVerificationHandler verificationHandler,
                                  List<ServiceController<?>> controllers) throws OperationFailedException {

        String objectStorePathRef = null;
        // Check for empty string value for relative-to, which disables the default
        final ModelNode relativePathNode = recoveryEnvModel.get(OBJECT_STORE_RELATIVE_TO.getName());
        if (!relativePathNode.isDefined() || relativePathNode.asString().length() > 0) {
            objectStorePathRef = OBJECT_STORE_RELATIVE_TO.validateResolvedOperation(recoveryEnvModel).asString();
        }
        final String objectStorePath = OBJECT_STORE_PATH.validateResolvedOperation(recoveryEnvModel).asString();
        if (ROOT_LOGGER.isDebugEnabled()) {
            ROOT_LOGGER.debugf("objectStorePathRef=%s, objectStorePath=%s\n", objectStorePathRef, objectStorePath);
        }

        ServiceTarget target = context.getServiceTarget();
        // Configure the ObjectStoreEnvironmentBeans
        ServiceController<String> objectStorePS = objectStorePathRef != null
                ? RelativePathService.addService(INTERNAL_OBJECTSTORE_PATH, objectStorePath, objectStorePathRef, target)
                : AbsolutePathService.addService(INTERNAL_OBJECTSTORE_PATH, objectStorePath, target);
        controllers.add(objectStorePS);

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
        final String nodeIdentifier = NODE_IDENTIFIER.validateResolvedOperation(coreEnvModel).asString();
        final CoreEnvironmentService coreEnvironmentService = new CoreEnvironmentService(nodeIdentifier);

        String socketBindingName = null;
        if (PROCESS_ID_UUID.validateResolvedOperation(coreEnvModel).asBoolean()) {
            // Use the UUID based id
            UuidProcessId id = new UuidProcessId();
            coreEnvironmentService.setProcessImplementation(id);
        } else {
            // Use the socket process id
            coreEnvironmentService.setProcessImplementationClassName(ProcessIdType.SOCKET.getClazz());
            socketBindingName = PROCESS_ID_SOCKET_BINDING.validateResolvedOperation(coreEnvModel).asString();
            int ports = PROCESS_ID_SOCKET_MAX_PORTS.validateResolvedOperation(coreEnvModel).asInt();
            coreEnvironmentService.setSocketProcessIdMaxPorts(ports);
        }

        String varDirPathRef = null;
        // Check for empty string value for relative-to, which disables the default
        final ModelNode relativePathNode = coreEnvModel.get(RELATIVE_TO.getName());
        if (!relativePathNode.isDefined() || relativePathNode.asString().length() > 0) {
            varDirPathRef = RELATIVE_TO.validateResolvedOperation(coreEnvModel).asString();
        }
        final String varDirPath = PATH.validateResolvedOperation(coreEnvModel).asString();

        if (ROOT_LOGGER.isDebugEnabled()) {
            ROOT_LOGGER.debugf("nodeIdentifier=%s\n", nodeIdentifier);
            ROOT_LOGGER.debugf("varDirPathRef=%s, varDirPath=%s\n", varDirPathRef, varDirPath);
        }

        ServiceTarget target = context.getServiceTarget();
        ServiceController<String> varDirRPS = varDirPathRef != null
                ? RelativePathService.addService(INTERNAL_CORE_ENV_VAR_PATH, varDirPath, varDirPathRef, target)
                : AbsolutePathService.addService(INTERNAL_CORE_ENV_VAR_PATH, varDirPath, target);
        controllers.add(varDirRPS);

        ServiceBuilder<?> coreEnvBuilder = context.getServiceTarget().addService(TxnServices.JBOSS_TXN_CORE_ENVIRONMENT, coreEnvironmentService);
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
                                  List<ServiceController<?>> controllers) throws OperationFailedException {
        //recovery environment
        final String recoveryBindingName = BINDING.validateResolvedOperation(model).asString();
        final String recoveryStatusBindingName = STATUS_BINDING.validateResolvedOperation(model).asString();
        final boolean recoveryListener = RECOVERY_LISTENER.validateResolvedOperation(model).asBoolean();

        final ArjunaRecoveryManagerService recoveryManagerService = new ArjunaRecoveryManagerService(recoveryListener);
        controllers.add(context.getServiceTarget().addService(TxnServices.JBOSS_TXN_ARJUNA_RECOVERY_MANAGER, recoveryManagerService)
               .addDependency(ServiceBuilder.DependencyType.OPTIONAL, ServiceName.JBOSS.append("iiop", "orb"), ORB.class, recoveryManagerService.getOrbInjector())
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
                                  List<ServiceController<?>> controllers) throws OperationFailedException {

        final boolean coordinatorEnableStatistics = ENABLE_STATISTICS.validateResolvedOperation(coordEnvModel).asBoolean();
        final boolean transactionStatusManagerEnable = ENABLE_TSM_STATUS.validateResolvedOperation(coordEnvModel).asBoolean();
        final int coordinatorDefaultTimeout = DEFAULT_TIMEOUT.validateResolvedOperation(coordEnvModel).asInt();

        final ArjunaTransactionManagerService transactionManagerService = new ArjunaTransactionManagerService(coordinatorEnableStatistics, coordinatorDefaultTimeout, transactionStatusManagerEnable);
        controllers.add(context.getServiceTarget().addService(TxnServices.JBOSS_TXN_ARJUNA_TRANSACTION_MANAGER, transactionManagerService)
                .addDependency(ServiceBuilder.DependencyType.OPTIONAL, ServiceName.JBOSS.append("iiop", "orb"), ORB.class, transactionManagerService.getOrbInjector())
                .addDependency(TxnServices.JBOSS_TXN_XA_TERMINATOR, JBossXATerminator.class, transactionManagerService.getXaTerminatorInjector())
                .addDependency(TxnServices.JBOSS_TXN_CORE_ENVIRONMENT)
                .addDependency(TxnServices.JBOSS_TXN_ARJUNA_OBJECTSTORE_ENVIRONMENT)
                .addDependency(TxnServices.JBOSS_TXN_ARJUNA_RECOVERY_MANAGER)
                .addListener(verificationHandler)
                .setInitialMode(ServiceController.Mode.ACTIVE)
                .install());


    }


}
