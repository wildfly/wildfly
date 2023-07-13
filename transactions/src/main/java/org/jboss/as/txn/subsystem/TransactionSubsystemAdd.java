/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.txn.subsystem;

import static org.jboss.as.controller.resource.AbstractSocketBindingResourceDefinition.SOCKET_BINDING_CAPABILITY_NAME;
import static org.jboss.as.txn.subsystem.CommonAttributes.CM_RESOURCE;
import static org.jboss.as.txn.subsystem.CommonAttributes.JDBC_STORE_DATASOURCE;
import static org.jboss.as.txn.subsystem.CommonAttributes.JDBC_STORE_DATASOURCE_NAME;
import static org.jboss.as.txn.subsystem.CommonAttributes.JTS;
import static org.jboss.as.txn.subsystem.CommonAttributes.USE_JOURNAL_STORE;
import static org.jboss.as.txn.subsystem.CommonAttributes.USE_JDBC_STORE;
import static org.jboss.as.txn.subsystem.TransactionSubsystemRootResourceDefinition.DATA_SOURCE_CAPABILITY_NAME;
import static org.jboss.as.txn.subsystem.TransactionSubsystemRootResourceDefinition.REMOTE_TRANSACTION_SERVICE_CAPABILITY;
import static org.jboss.as.txn.subsystem.TransactionSubsystemRootResourceDefinition.TRANSACTION_CAPABILITY;
import static org.jboss.as.txn.subsystem.TransactionSubsystemRootResourceDefinition.XA_RESOURCE_RECOVERY_REGISTRY_CAPABILITY;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.arjuna.ats.jbossatx.jta.RecoveryManagerService;
import jakarta.transaction.TransactionSynchronizationRegistry;
import jakarta.transaction.UserTransaction;

import io.undertow.server.handlers.PathHandler;
import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.CapabilityServiceBuilder;
import org.jboss.as.controller.CapabilityServiceTarget;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ProcessStateNotifier;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.services.path.PathManager;
import org.jboss.as.controller.services.path.PathManagerService;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.ManagedReferenceInjector;
import org.jboss.as.naming.ServiceBasedNamingStore;
import org.jboss.as.naming.ValueManagedReferenceFactory;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.service.BinderService;
import org.jboss.as.network.SocketBinding;
import org.jboss.as.network.SocketBindingManager;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.ServerEnvironmentService;
import org.jboss.as.server.deployment.Phase;
import org.jboss.as.server.suspend.SuspendController;
import org.jboss.as.txn.deployment.TransactionDependenciesProcessor;
import org.jboss.as.txn.deployment.TransactionJndiBindingProcessor;
import org.jboss.as.txn.deployment.TransactionLeakRollbackProcessor;
import org.jboss.as.txn.logging.TransactionLogger;
import org.jboss.as.txn.service.ArjunaObjectStoreEnvironmentService;
import org.jboss.as.txn.service.ArjunaRecoveryManagerService;
import org.jboss.as.txn.service.ArjunaTransactionManagerService;
import org.jboss.as.txn.service.JBossContextXATerminatorService;
import org.jboss.as.txn.service.CoreEnvironmentService;
import org.jboss.as.txn.service.ExtendedJBossXATerminatorService;
import org.jboss.as.txn.service.JTAEnvironmentBeanService;
import org.jboss.as.txn.service.LocalTransactionContextService;
import org.jboss.as.txn.service.RemotingTransactionServiceService;
import org.jboss.as.txn.service.TransactionManagerService;
import org.jboss.as.txn.service.TransactionRemoteHTTPService;
import org.jboss.as.txn.service.TransactionSynchronizationRegistryService;
import org.jboss.as.txn.service.TxnServices;
import org.jboss.as.txn.service.UserTransactionAccessControlService;
import org.jboss.as.txn.service.UserTransactionBindingService;
import org.jboss.as.txn.service.UserTransactionRegistryService;
import org.jboss.as.txn.service.UserTransactionService;
import org.jboss.as.txn.service.XATerminatorService;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.inject.InjectionException;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.remoting3.Endpoint;
import org.jboss.tm.ExtendedJBossXATerminator;
import org.jboss.tm.JBossXATerminator;
import org.jboss.tm.XAResourceRecoveryRegistry;
import org.jboss.tm.usertx.UserTransactionRegistry;
import org.omg.CORBA.ORB;
import org.wildfly.common.function.Functions;
import org.wildfly.iiop.openjdk.service.CorbaNamingService;

import com.arjuna.ats.internal.arjuna.utils.UuidProcessId;
import com.arjuna.ats.jta.common.JTAEnvironmentBean;
import com.arjuna.ats.jts.common.jtsPropertyManager;
import org.wildfly.transaction.client.ContextTransactionManager;
import org.wildfly.transaction.client.LocalTransactionContext;

import javax.sql.DataSource;

/**
 * Adds the transaction management subsystem.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author Emanuel Muckenhuber
 * @author Scott Stark (sstark@redhat.com) (C) 2011 Red Hat Inc.
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
class TransactionSubsystemAdd extends AbstractBoottimeAddStepHandler {

    static final TransactionSubsystemAdd INSTANCE = new TransactionSubsystemAdd();

    private static final String UNDERTOW_HTTP_INVOKER_CAPABILITY_NAME = "org.wildfly.undertow.http-invoker";
    private static final String REMOTING_ENDPOINT_CAPABILITY_NAME = "org.wildfly.remoting.endpoint";

    private TransactionSubsystemAdd() {
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        populateModelWithRecoveryEnvConfig(operation, model);

        populateModelWithCoreEnvConfig(operation, model);

        populateModelWithCoordinatorEnvConfig(operation, model);

        populateModelWithObjectStoreConfig(operation, model);

        TransactionSubsystemRootResourceDefinition.JTS.validateAndSet(operation, model);

        validateStoreConfig(operation, model);

        TransactionSubsystemRootResourceDefinition.USE_JOURNAL_STORE.validateAndSet(operation, model);

        for (AttributeDefinition ad : TransactionSubsystemRootResourceDefinition.attributes_1_2) {
            ad.validateAndSet(operation, model);
        }

        TransactionSubsystemRootResourceDefinition.JOURNAL_STORE_ENABLE_ASYNC_IO.validateAndSet(operation, model);

        TransactionSubsystemRootResourceDefinition.STALE_TRANSACTION_TIME.validateAndSet(operation, model);

        TransactionSubsystemRootResourceDefinition.JDBC_STORE_DATASOURCE_NAME.validateAndSet(operation, model);
    }

    private void populateModelWithObjectStoreConfig(ModelNode operation, ModelNode objectStoreModel) throws OperationFailedException {

        TransactionSubsystemRootResourceDefinition.OBJECT_STORE_RELATIVE_TO.validateAndSet(operation, objectStoreModel);
        TransactionSubsystemRootResourceDefinition.OBJECT_STORE_PATH.validateAndSet(operation, objectStoreModel);

        ModelNode relativeVal = objectStoreModel.get(TransactionSubsystemRootResourceDefinition.OBJECT_STORE_RELATIVE_TO.getName());
        ModelNode pathVal =  objectStoreModel.get(TransactionSubsystemRootResourceDefinition.OBJECT_STORE_PATH.getName());

        if (!relativeVal.isDefined() &&
                (!pathVal.isDefined() || pathVal.asString().equals(TransactionSubsystemRootResourceDefinition.OBJECT_STORE_PATH.getDefaultValue().asString()))) {
            relativeVal.set(new ModelNode().set("jboss.server.data.dir"));
            TransactionLogger.ROOT_LOGGER.objectStoreRelativeToIsSetToDefault();
        }

    }

    private void populateModelWithCoordinatorEnvConfig(ModelNode operation, ModelNode coordEnvModel) throws OperationFailedException {
        TransactionSubsystemRootResourceDefinition.STATISTICS_ENABLED.validateAndSet(operation, coordEnvModel);
        TransactionSubsystemRootResourceDefinition.ENABLE_STATISTICS.validateAndSet(operation, coordEnvModel);
        TransactionSubsystemRootResourceDefinition.ENABLE_TSM_STATUS.validateAndSet(operation, coordEnvModel);
        TransactionSubsystemRootResourceDefinition.DEFAULT_TIMEOUT.validateAndSet(operation, coordEnvModel);
        TransactionSubsystemRootResourceDefinition.MAXIMUM_TIMEOUT.validateAndSet(operation, coordEnvModel);

        ModelNode mceVal = coordEnvModel.get(TransactionSubsystemRootResourceDefinition.ENABLE_STATISTICS.getName());
        if (mceVal.isDefined()) {
            ModelNode seVal = coordEnvModel.get(TransactionSubsystemRootResourceDefinition.STATISTICS_ENABLED.getName());
            if (seVal.isDefined() && !seVal.equals(mceVal)) {
                throw TransactionLogger.ROOT_LOGGER.inconsistentStatisticsSettings(TransactionSubsystemRootResourceDefinition.STATISTICS_ENABLED.getName(),
                        TransactionSubsystemRootResourceDefinition.ENABLE_STATISTICS.getName());
            }
            seVal.set(mceVal);
            mceVal.set(new ModelNode());
        }
    }

    private void populateModelWithCoreEnvConfig(ModelNode operation, ModelNode model) throws OperationFailedException {
        //core environment
        TransactionSubsystemRootResourceDefinition.NODE_IDENTIFIER.validateAndSet(operation, model);

        // We have some complex logic for the 'process-id' stuff because of the alternatives
        if (operation.hasDefined(TransactionSubsystemRootResourceDefinition.PROCESS_ID_UUID.getName()) && operation.get(TransactionSubsystemRootResourceDefinition.PROCESS_ID_UUID.getName()).asBoolean()) {
            TransactionSubsystemRootResourceDefinition.PROCESS_ID_UUID.validateAndSet(operation, model);
            if (operation.hasDefined(TransactionSubsystemRootResourceDefinition.PROCESS_ID_SOCKET_BINDING.getName())) {
                throw new OperationFailedException(String.format("%s must be undefined if %s is 'true'.",
                        TransactionSubsystemRootResourceDefinition.PROCESS_ID_SOCKET_BINDING.getName(), TransactionSubsystemRootResourceDefinition.PROCESS_ID_UUID.getName()));
            } else if (operation.hasDefined(TransactionSubsystemRootResourceDefinition.PROCESS_ID_SOCKET_MAX_PORTS.getName())) {
                throw new OperationFailedException(String.format("%s must be undefined if %s is 'true'.",
                        TransactionSubsystemRootResourceDefinition.PROCESS_ID_SOCKET_MAX_PORTS.getName(), TransactionSubsystemRootResourceDefinition.PROCESS_ID_UUID.getName()));
            }
            //model.get(TransactionSubsystemRootResourceDefinition.PROCESS_ID_SOCKET_BINDING.getName());
            //model.get(TransactionSubsystemRootResourceDefinition.PROCESS_ID_SOCKET_MAX_PORTS.getName());
        } else if (operation.hasDefined(TransactionSubsystemRootResourceDefinition.PROCESS_ID_SOCKET_BINDING.getName())) {
            TransactionSubsystemRootResourceDefinition.PROCESS_ID_SOCKET_BINDING.validateAndSet(operation, model);
            TransactionSubsystemRootResourceDefinition.PROCESS_ID_SOCKET_MAX_PORTS.validateAndSet(operation, model);
        } else if (operation.hasDefined(TransactionSubsystemRootResourceDefinition.PROCESS_ID_SOCKET_MAX_PORTS.getName())) {
            throw new OperationFailedException(String.format("%s must be defined if %s is defined.",
                    TransactionSubsystemRootResourceDefinition.PROCESS_ID_SOCKET_BINDING.getName(), TransactionSubsystemRootResourceDefinition.PROCESS_ID_SOCKET_MAX_PORTS.getName()));
        } else {
            // not uuid and also not sockets!
            throw new OperationFailedException(String.format("Either %s must be 'true' or  %s must be defined.",
                    TransactionSubsystemRootResourceDefinition.PROCESS_ID_UUID.getName(), TransactionSubsystemRootResourceDefinition.PROCESS_ID_SOCKET_BINDING.getName()));
        }
    }

    private void populateModelWithRecoveryEnvConfig(ModelNode operation, ModelNode model) throws OperationFailedException {
        //recovery environment
        TransactionSubsystemRootResourceDefinition.BINDING.validateAndSet(operation, model);
        TransactionSubsystemRootResourceDefinition.STATUS_BINDING.validateAndSet(operation, model);
        TransactionSubsystemRootResourceDefinition.RECOVERY_LISTENER.validateAndSet(operation, model);
    }

    private void validateStoreConfig(ModelNode operation, ModelNode model) throws OperationFailedException {
        if (operation.hasDefined(USE_JDBC_STORE) && operation.get(USE_JDBC_STORE).asBoolean()
                && operation.hasDefined(USE_JOURNAL_STORE) && operation.get(USE_JOURNAL_STORE).asBoolean()) {
            throw TransactionLogger.ROOT_LOGGER.onlyOneCanBeTrue(USE_JDBC_STORE, USE_JOURNAL_STORE);
        }
        if (operation.hasDefined(USE_JDBC_STORE) && operation.get(USE_JDBC_STORE).asBoolean()
                && !(operation.hasDefined(JDBC_STORE_DATASOURCE_NAME) || operation.hasDefined(JDBC_STORE_DATASOURCE))) {
            throw TransactionLogger.ROOT_LOGGER.mustBeDefinedIfTrue(JDBC_STORE_DATASOURCE_NAME + " or " + JDBC_STORE_DATASOURCE, USE_JDBC_STORE );
        }
    }

    @Override
    protected void performBoottime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {

        checkIfNodeIdentifierIsDefault(context, model);

        boolean jts = model.hasDefined(JTS) && model.get(JTS).asBoolean();

        final Resource subsystemResource = context.readResourceFromRoot(PathAddress.pathAddress(TransactionExtension.SUBSYSTEM_PATH), false);
        final List<ServiceName> deps = new LinkedList<>();

        for (String name : subsystemResource.getChildrenNames(CM_RESOURCE)) {
            deps.add(TxnServices.JBOSS_TXN_CMR.append(name));
        }

        //recovery environment
        performRecoveryEnvBoottime(context, model, jts, deps);

        //core environment
        performCoreEnvironmentBootTime(context, model);

        //coordinator environment
        performCoordinatorEnvBoottime(context, model, jts);

        //object store
        performObjectStoreBoottime(context, model);

        //always propagate the transaction context
        //TODO: need a better way to do this, but this value gets cached in a static
        //so we need to make sure we set it before anything tries to read it
        jtsPropertyManager.getJTSEnvironmentBean().setAlwaysPropagateContext(true);

        context.addStep(new AbstractDeploymentChainStep() {
            protected void execute(final DeploymentProcessorTarget processorTarget) {
                processorTarget.addDeploymentProcessor(TransactionExtension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_TRANSACTION_ROLLBACK_ACTION, new TransactionLeakRollbackProcessor());
                processorTarget.addDeploymentProcessor(TransactionExtension.SUBSYSTEM_NAME, Phase.INSTALL, Phase.INSTALL_TRANSACTION_BINDINGS, new TransactionJndiBindingProcessor());
                processorTarget.addDeploymentProcessor(TransactionExtension.SUBSYSTEM_NAME, Phase.DEPENDENCIES, Phase.DEPENDENCIES_TRANSACTIONS, new TransactionDependenciesProcessor());
            }
        }, OperationContext.Stage.RUNTIME);

        //bind the TransactionManger and the TSR into JNDI
        final BinderService tmBinderService = new BinderService("TransactionManager");
        final ServiceBuilder<ManagedReferenceFactory> tmBuilder = context.getServiceTarget().addService(ContextNames.JBOSS_CONTEXT_SERVICE_NAME.append("TransactionManager"), tmBinderService);
        tmBuilder.addDependency(ContextNames.JBOSS_CONTEXT_SERVICE_NAME, ServiceBasedNamingStore.class, tmBinderService.getNamingStoreInjector());
        tmBuilder.addDependency(TransactionManagerService.INTERNAL_SERVICE_NAME, jakarta.transaction.TransactionManager.class, new Injector<jakarta.transaction.TransactionManager>() {
            @Override
            public void inject(final jakarta.transaction.TransactionManager value) throws InjectionException {
                tmBinderService.getManagedObjectInjector().inject(new ValueManagedReferenceFactory(value));
            }

            @Override
            public void uninject() {
                tmBinderService.getManagedObjectInjector().uninject();
            }
        });
        tmBuilder.install();

        final BinderService tmLegacyBinderService = new BinderService("TransactionManager");
        final ServiceBuilder<ManagedReferenceFactory> tmLegacyBuilder = context.getServiceTarget().addService(ContextNames.JAVA_CONTEXT_SERVICE_NAME.append("TransactionManager"), tmLegacyBinderService);
        tmLegacyBuilder.addDependency(ContextNames.JAVA_CONTEXT_SERVICE_NAME, ServiceBasedNamingStore.class, tmLegacyBinderService.getNamingStoreInjector());
        tmLegacyBuilder.addDependency(TransactionManagerService.INTERNAL_SERVICE_NAME, jakarta.transaction.TransactionManager.class, new Injector<jakarta.transaction.TransactionManager>() {
            @Override
            public void inject(final jakarta.transaction.TransactionManager value) throws InjectionException {
                tmLegacyBinderService.getManagedObjectInjector().inject(new ValueManagedReferenceFactory(value));
            }

            @Override
            public void uninject() {
                tmLegacyBinderService.getManagedObjectInjector().uninject();
            }
        });
        tmLegacyBuilder.install();

        final BinderService tsrBinderService = new BinderService("TransactionSynchronizationRegistry");
        final ServiceBuilder<ManagedReferenceFactory> tsrBuilder = context.getServiceTarget().addService(ContextNames.JBOSS_CONTEXT_SERVICE_NAME.append("TransactionSynchronizationRegistry"), tsrBinderService);
        tsrBuilder.addDependency(ContextNames.JBOSS_CONTEXT_SERVICE_NAME, ServiceBasedNamingStore.class, tsrBinderService.getNamingStoreInjector());
        tsrBuilder.addDependency(TransactionSynchronizationRegistryService.INTERNAL_SERVICE_NAME, TransactionSynchronizationRegistry.class, new Injector<TransactionSynchronizationRegistry>() {
            @Override
            public void inject(final TransactionSynchronizationRegistry value) throws InjectionException {
                tsrBinderService.getManagedObjectInjector().inject(new ValueManagedReferenceFactory(value));
            }

            @Override
            public void uninject() {
                tsrBinderService.getManagedObjectInjector().uninject();
            }
        });
        tsrBuilder.install();

        // Install the UserTransactionAccessControlService
        final UserTransactionAccessControlService lookupControlService = new UserTransactionAccessControlService();
        context.getServiceTarget().addService(UserTransactionAccessControlService.SERVICE_NAME, lookupControlService).install();

        // Bind the UserTransaction into JNDI
        final UserTransactionBindingService userTransactionBindingService = new UserTransactionBindingService("UserTransaction");
        final ServiceBuilder<ManagedReferenceFactory> utBuilder = context.getServiceTarget().addService(ContextNames.JBOSS_CONTEXT_SERVICE_NAME.append("UserTransaction"), userTransactionBindingService);
        utBuilder.addDependency(ContextNames.JBOSS_CONTEXT_SERVICE_NAME, ServiceBasedNamingStore.class, userTransactionBindingService.getNamingStoreInjector())
                .addDependency(UserTransactionAccessControlService.SERVICE_NAME, UserTransactionAccessControlService.class, userTransactionBindingService.getUserTransactionAccessControlServiceInjector())
                .addDependency(UserTransactionService.INTERNAL_SERVICE_NAME, UserTransaction.class,
                        new ManagedReferenceInjector<UserTransaction>(userTransactionBindingService.getManagedObjectInjector()));
        utBuilder.install();
    }

    private void performObjectStoreBoottime(OperationContext context, ModelNode model) throws OperationFailedException {
        boolean useJournalStore = model.hasDefined(USE_JOURNAL_STORE) && model.get(USE_JOURNAL_STORE).asBoolean();
        final boolean enableAsyncIO = TransactionSubsystemRootResourceDefinition.JOURNAL_STORE_ENABLE_ASYNC_IO.resolveModelAttribute(context, model).asBoolean();
        final String objectStorePathRef = TransactionSubsystemRootResourceDefinition.OBJECT_STORE_RELATIVE_TO.resolveModelAttribute(context, model).isDefined() ?
                TransactionSubsystemRootResourceDefinition.OBJECT_STORE_RELATIVE_TO.resolveModelAttribute(context, model).asString(): null;
        final String objectStorePath = TransactionSubsystemRootResourceDefinition.OBJECT_STORE_PATH.resolveModelAttribute(context, model).asString();
        final boolean useJdbcStore = model.hasDefined(USE_JDBC_STORE) && model.get(USE_JDBC_STORE).asBoolean();
        final String dataSourceName = TransactionSubsystemRootResourceDefinition.JDBC_STORE_DATASOURCE_NAME.resolveModelAttribute(context, model).asString();
        final String dataSourceJndiName = TransactionSubsystemRootResourceDefinition.JDBC_STORE_DATASOURCE.resolveModelAttribute(context, model).asString();

        ArjunaObjectStoreEnvironmentService.JdbcStoreConfigBulder confiBuilder = new ArjunaObjectStoreEnvironmentService.JdbcStoreConfigBulder();
        confiBuilder.setActionDropTable(TransactionSubsystemRootResourceDefinition.JDBC_ACTION_STORE_DROP_TABLE.resolveModelAttribute(context, model).asBoolean())
                .setStateDropTable(TransactionSubsystemRootResourceDefinition.JDBC_STATE_STORE_DROP_TABLE.resolveModelAttribute(context, model).asBoolean())
                .setCommunicationDropTable(TransactionSubsystemRootResourceDefinition.JDBC_COMMUNICATION_STORE_DROP_TABLE.resolveModelAttribute(context, model).asBoolean());

        if (model.hasDefined(TransactionSubsystemRootResourceDefinition.JDBC_ACTION_STORE_TABLE_PREFIX.getName()))
            confiBuilder.setActionTablePrefix(TransactionSubsystemRootResourceDefinition.JDBC_ACTION_STORE_TABLE_PREFIX.resolveModelAttribute(context, model).asString());
        if (model.hasDefined(TransactionSubsystemRootResourceDefinition.JDBC_STATE_STORE_TABLE_PREFIX.getName()))
            confiBuilder.setStateTablePrefix(TransactionSubsystemRootResourceDefinition.JDBC_STATE_STORE_TABLE_PREFIX.resolveModelAttribute(context, model).asString());

        if (model.hasDefined(TransactionSubsystemRootResourceDefinition.JDBC_COMMUNICATION_STORE_TABLE_PREFIX.getName()))
            confiBuilder.setCommunicationTablePrefix(TransactionSubsystemRootResourceDefinition.JDBC_COMMUNICATION_STORE_TABLE_PREFIX.resolveModelAttribute(context, model).asString());

        TransactionLogger.ROOT_LOGGER.debugf("objectStorePathRef=%s, objectStorePath=%s%n", objectStorePathRef, objectStorePath);

        CapabilityServiceTarget target = context.getCapabilityServiceTarget();
        // Configure the ObjectStoreEnvironmentBeans
        DataSource dataSource = null;
        if (useJdbcStore && dataSourceName != null) {
            CapabilityServiceBuilder sb = target.addCapability(TRANSACTION_CAPABILITY);
            Supplier<DataSource> dsSupplier = sb.requiresCapability(DATA_SOURCE_CAPABILITY_NAME, DataSource.class, dataSourceName);
            dataSource = dsSupplier.get();
        }
        final ArjunaObjectStoreEnvironmentService objStoreEnvironmentService = new ArjunaObjectStoreEnvironmentService(useJournalStore, enableAsyncIO, objectStorePath, objectStorePathRef, useJdbcStore, dataSource, dataSourceJndiName, confiBuilder.build());
        ServiceBuilder<Void> builder = target.addService(TxnServices.JBOSS_TXN_ARJUNA_OBJECTSTORE_ENVIRONMENT, objStoreEnvironmentService);
        builder.addDependency(PathManagerService.SERVICE_NAME, PathManager.class, objStoreEnvironmentService.getPathManagerInjector());
        builder.requires(TxnServices.JBOSS_TXN_CORE_ENVIRONMENT);

        if (useJdbcStore && dataSourceName == null) {
            final ContextNames.BindInfo bindInfo = ContextNames.bindInfoFor(dataSourceJndiName);
            builder.requires(bindInfo.getBinderServiceName());
        }
        builder.setInitialMode(Mode.ACTIVE).install();

        TransactionManagerService.addService(target);
        UserTransactionService.addService(target);
        target.addService(TxnServices.JBOSS_TXN_USER_TRANSACTION_REGISTRY, new UserTransactionRegistryService())
                .setInitialMode(Mode.ACTIVE).install();
        TransactionSynchronizationRegistryService.addService(target);

    }

    private void performCoreEnvironmentBootTime(OperationContext context, ModelNode coreEnvModel) throws OperationFailedException {

        // Configure the core configuration.
        final String nodeIdentifier = TransactionSubsystemRootResourceDefinition.NODE_IDENTIFIER.resolveModelAttribute(context, coreEnvModel).asString();
        TransactionLogger.ROOT_LOGGER.debugf("nodeIdentifier=%s%n", nodeIdentifier);
        final CapabilityServiceBuilder<?> builder = context.getCapabilityServiceTarget().addCapability(TRANSACTION_CAPABILITY);
        Supplier<SocketBinding> socketBindingSupplier =  Functions.constantSupplier(null);

        String socketBindingName = null;
        if (!TransactionSubsystemRootResourceDefinition.PROCESS_ID_UUID.resolveModelAttribute(context, coreEnvModel).asBoolean(false)) {
            socketBindingName = TransactionSubsystemRootResourceDefinition.PROCESS_ID_SOCKET_BINDING.resolveModelAttribute(context, coreEnvModel).asString();
            socketBindingSupplier = builder.requiresCapability(SOCKET_BINDING_CAPABILITY_NAME, SocketBinding.class, socketBindingName);
        }

        final CoreEnvironmentService coreEnvironmentService = new CoreEnvironmentService(nodeIdentifier, socketBindingSupplier);

        if (TransactionSubsystemRootResourceDefinition.PROCESS_ID_UUID.resolveModelAttribute(context, coreEnvModel).asBoolean(false)) {
            // Use the UUID based id
            UuidProcessId id = new UuidProcessId();
            coreEnvironmentService.setProcessImplementation(id);
        } else {
            // Use the socket process id
            coreEnvironmentService.setProcessImplementationClassName(ProcessIdType.SOCKET.getClazz());
            int ports = TransactionSubsystemRootResourceDefinition.PROCESS_ID_SOCKET_MAX_PORTS.resolveModelAttribute(context, coreEnvModel).asInt();
            coreEnvironmentService.setSocketProcessIdMaxPorts(ports);
        }

        builder.setInstance(coreEnvironmentService).addAliases(TxnServices.JBOSS_TXN_CORE_ENVIRONMENT).setInitialMode(Mode.ACTIVE).install();
    }

    private void performRecoveryEnvBoottime(OperationContext context, ModelNode model, final boolean jts, List<ServiceName> deps) throws OperationFailedException {
        CapabilityServiceTarget serviceTarget = context.getCapabilityServiceTarget();

        //recovery environment
        final String recoveryBindingName = TransactionSubsystemRootResourceDefinition.BINDING.resolveModelAttribute(context, model).asString();
        final String recoveryStatusBindingName = TransactionSubsystemRootResourceDefinition.STATUS_BINDING.resolveModelAttribute(context, model).asString();
        final boolean recoveryListener = TransactionSubsystemRootResourceDefinition.RECOVERY_LISTENER.resolveModelAttribute(context, model).asBoolean();

        final CapabilityServiceBuilder<?> recoveryManagerServiceServiceBuilder = serviceTarget.addCapability(XA_RESOURCE_RECOVERY_REGISTRY_CAPABILITY);
        final Consumer<RecoveryManagerService> consumer = recoveryManagerServiceServiceBuilder.provides(XA_RESOURCE_RECOVERY_REGISTRY_CAPABILITY);
        final Supplier<SocketBinding> recoveryBindingSupplier = recoveryManagerServiceServiceBuilder.requiresCapability("org.wildfly.network.socket-binding", SocketBinding.class, recoveryBindingName);
        final Supplier<SocketBinding> statusBindingSupplier = recoveryManagerServiceServiceBuilder.requiresCapability("org.wildfly.network.socket-binding", SocketBinding.class, recoveryStatusBindingName);
        final Supplier<SocketBindingManager> bindingManagerSupplier = recoveryManagerServiceServiceBuilder.requiresCapability("org.wildfly.management.socket-binding-manager", SocketBindingManager.class);
        final Supplier<SuspendController> suspendControllerSupplier = recoveryManagerServiceServiceBuilder.requiresCapability("org.wildfly.server.suspend-controller", SuspendController.class);
        final Supplier<ProcessStateNotifier> processStateSupplier = recoveryManagerServiceServiceBuilder.requiresCapability("org.wildfly.management.process-state-notifier", ProcessStateNotifier.class);
        recoveryManagerServiceServiceBuilder.requires(TxnServices.JBOSS_TXN_CORE_ENVIRONMENT);
        recoveryManagerServiceServiceBuilder.requires(TxnServices.JBOSS_TXN_ARJUNA_OBJECTSTORE_ENVIRONMENT);
        recoveryManagerServiceServiceBuilder.addAliases(TxnServices.JBOSS_TXN_ARJUNA_RECOVERY_MANAGER);


        // add dependency on Jakarta Transactions environment bean
        for (final ServiceName dep : deps) {
            recoveryManagerServiceServiceBuilder.requires(dep);
        }

        // Register WildFly transaction services - TODO: this should eventually be separated from the Narayana subsystem
        final int staleTransactionTime = TransactionSubsystemRootResourceDefinition.STALE_TRANSACTION_TIME.resolveModelAttribute(context, model).asInt();
        final LocalTransactionContextService localTransactionContextService = new LocalTransactionContextService(staleTransactionTime);
        serviceTarget.addService(TxnServices.JBOSS_TXN_LOCAL_TRANSACTION_CONTEXT, localTransactionContextService)
                .addDependency(TxnServices.JBOSS_TXN_EXTENDED_JBOSS_XA_TERMINATOR, ExtendedJBossXATerminator.class, localTransactionContextService.getExtendedJBossXATerminatorInjector())
                .addDependency(TxnServices.JBOSS_TXN_ARJUNA_TRANSACTION_MANAGER, com.arjuna.ats.jbossatx.jta.TransactionManagerService.class, localTransactionContextService.getTransactionManagerInjector())
                .addDependency(XA_RESOURCE_RECOVERY_REGISTRY_CAPABILITY.getCapabilityServiceName(), XAResourceRecoveryRegistry.class, localTransactionContextService.getXAResourceRecoveryRegistryInjector())
                .addDependency(ServerEnvironmentService.SERVICE_NAME, ServerEnvironment.class, localTransactionContextService.getServerEnvironmentInjector())
                .setInitialMode(Mode.ACTIVE)
                .install();

        if (context.hasOptionalCapability(REMOTING_ENDPOINT_CAPABILITY_NAME, TRANSACTION_CAPABILITY.getName(),null)) {
            final RemotingTransactionServiceService remoteTransactionServiceService = new RemotingTransactionServiceService();
            serviceTarget.addCapability(REMOTE_TRANSACTION_SERVICE_CAPABILITY)
                .setInstance(remoteTransactionServiceService)
                .addDependency(TxnServices.JBOSS_TXN_LOCAL_TRANSACTION_CONTEXT, LocalTransactionContext.class, remoteTransactionServiceService.getLocalTransactionContextInjector())
                .addDependency(context.getCapabilityServiceName(REMOTING_ENDPOINT_CAPABILITY_NAME, Endpoint.class), Endpoint.class, remoteTransactionServiceService.getEndpointInjector())
                .setInitialMode(Mode.ACTIVE)
                .install();
        }

        if(context.hasOptionalCapability(UNDERTOW_HTTP_INVOKER_CAPABILITY_NAME, TRANSACTION_CAPABILITY.getName(), null)) {
            final TransactionRemoteHTTPService remoteHTTPService = new TransactionRemoteHTTPService();
            serviceTarget.addService(TxnServices.JBOSS_TXN_HTTP_REMOTE_TRANSACTION_SERVICE, remoteHTTPService)
                .addDependency(TxnServices.JBOSS_TXN_LOCAL_TRANSACTION_CONTEXT, LocalTransactionContext.class, remoteHTTPService.getLocalTransactionContextInjectedValue())
                .addDependency(context.getCapabilityServiceName(UNDERTOW_HTTP_INVOKER_CAPABILITY_NAME, PathHandler.class), PathHandler.class, remoteHTTPService.getPathHandlerInjectedValue())
                    .install();
        }

        final String nodeIdentifier = TransactionSubsystemRootResourceDefinition.NODE_IDENTIFIER.resolveModelAttribute(context, model).asString();
        // install Jakarta Transactions environment bean service
        final JTAEnvironmentBeanService jtaEnvironmentBeanService = new JTAEnvironmentBeanService(nodeIdentifier);
        serviceTarget.addService(TxnServices.JBOSS_TXN_JTA_ENVIRONMENT, jtaEnvironmentBeanService)
                .setInitialMode(Mode.ACTIVE)
                .install();

        final XATerminatorService xaTerminatorService;
        final ExtendedJBossXATerminatorService extendedJBossXATerminatorService;

        Supplier<ORB> orbSupplier = null;
        if (jts) {
            jtaEnvironmentBeanService.getValue()
                .setTransactionManagerClassName(com.arjuna.ats.jbossatx.jts.TransactionManagerDelegate.class.getName());

            orbSupplier = recoveryManagerServiceServiceBuilder.requires(ServiceName.JBOSS.append("iiop-openjdk", "orb-service"));

            com.arjuna.ats.internal.jbossatx.jts.jca.XATerminator terminator = new com.arjuna.ats.internal.jbossatx.jts.jca.XATerminator();
            xaTerminatorService = new XATerminatorService(terminator);
            extendedJBossXATerminatorService = new ExtendedJBossXATerminatorService(terminator);
        } else {
            jtaEnvironmentBeanService.getValue()
                .setTransactionManagerClassName(com.arjuna.ats.jbossatx.jta.TransactionManagerDelegate.class.getName());
            com.arjuna.ats.internal.jbossatx.jta.jca.XATerminator terminator = new com.arjuna.ats.internal.jbossatx.jta.jca.XATerminator();
            xaTerminatorService = new XATerminatorService(terminator);
            extendedJBossXATerminatorService = new ExtendedJBossXATerminatorService(terminator);
        }

        serviceTarget.addService(TxnServices.JBOSS_TXN_XA_TERMINATOR, xaTerminatorService)
                .setInitialMode(Mode.ACTIVE).install();
        serviceTarget
                .addService(TxnServices.JBOSS_TXN_EXTENDED_JBOSS_XA_TERMINATOR, extendedJBossXATerminatorService)
                .setInitialMode(Mode.ACTIVE).install();

        final JBossContextXATerminatorService contextXATerminatorService = new JBossContextXATerminatorService();
        serviceTarget
                .addService(TxnServices.JBOSS_TXN_CONTEXT_XA_TERMINATOR, contextXATerminatorService)
                .addDependency(TxnServices.JBOSS_TXN_XA_TERMINATOR, JBossXATerminator.class, contextXATerminatorService.getJBossXATerminatorInjector())
                .addDependency(TxnServices.JBOSS_TXN_LOCAL_TRANSACTION_CONTEXT, LocalTransactionContext.class, contextXATerminatorService.getLocalTransactionContextInjector())
                .setInitialMode(Mode.ACTIVE).install();

        final ArjunaRecoveryManagerService recoveryManagerService = new ArjunaRecoveryManagerService(consumer, recoveryBindingSupplier, statusBindingSupplier, bindingManagerSupplier, suspendControllerSupplier, processStateSupplier, orbSupplier, recoveryListener, jts);
        recoveryManagerServiceServiceBuilder.setInstance(recoveryManagerService);
        recoveryManagerServiceServiceBuilder.install();
    }

    private void performCoordinatorEnvBoottime(OperationContext context, ModelNode coordEnvModel, final boolean jts) throws OperationFailedException {

        final boolean coordinatorEnableStatistics = TransactionSubsystemRootResourceDefinition.STATISTICS_ENABLED.resolveModelAttribute(context, coordEnvModel).asBoolean();
        final boolean transactionStatusManagerEnable = TransactionSubsystemRootResourceDefinition.ENABLE_TSM_STATUS.resolveModelAttribute(context, coordEnvModel).asBoolean();
        final int coordinatorDefaultTimeout = TransactionSubsystemRootResourceDefinition.DEFAULT_TIMEOUT.resolveModelAttribute(context, coordEnvModel).asInt();
        final int maximumTimeout = TransactionSubsystemRootResourceDefinition.MAXIMUM_TIMEOUT.resolveModelAttribute(context, coordEnvModel).asInt();

        // WFLY-9955 Allow the timeout set to "0" while translating into the maximum timeout
        if (coordinatorDefaultTimeout == 0) {
            ContextTransactionManager.setGlobalDefaultTransactionTimeout(maximumTimeout);
            TransactionLogger.ROOT_LOGGER.timeoutValueIsSetToMaximum(maximumTimeout);
        } else {
            ContextTransactionManager.setGlobalDefaultTransactionTimeout(coordinatorDefaultTimeout);
        }
        final ArjunaTransactionManagerService transactionManagerService = new ArjunaTransactionManagerService(coordinatorEnableStatistics, coordinatorDefaultTimeout, transactionStatusManagerEnable, jts);
        final ServiceBuilder<com.arjuna.ats.jbossatx.jta.TransactionManagerService> transactionManagerServiceServiceBuilder = context.getServiceTarget().addService(TxnServices.JBOSS_TXN_ARJUNA_TRANSACTION_MANAGER, transactionManagerService);
        // add dependency on Jakarta Transactions environment bean service
        transactionManagerServiceServiceBuilder.addDependency(TxnServices.JBOSS_TXN_JTA_ENVIRONMENT, JTAEnvironmentBean.class, transactionManagerService.getJTAEnvironmentBeanInjector());

        //if jts is enabled we need the ORB
        if (jts) {
            transactionManagerServiceServiceBuilder.addDependency(ServiceName.JBOSS.append("iiop-openjdk", "orb-service"), ORB.class, transactionManagerService.getOrbInjector());
            transactionManagerServiceServiceBuilder.requires(CorbaNamingService.SERVICE_NAME);
        }

        transactionManagerServiceServiceBuilder.addDependency(TxnServices.JBOSS_TXN_XA_TERMINATOR, JBossXATerminator.class, transactionManagerService.getXaTerminatorInjector());
        transactionManagerServiceServiceBuilder.addDependency(TxnServices.JBOSS_TXN_USER_TRANSACTION_REGISTRY, UserTransactionRegistry.class, transactionManagerService.getUserTransactionRegistry());
        transactionManagerServiceServiceBuilder.requires(TxnServices.JBOSS_TXN_CORE_ENVIRONMENT);
        transactionManagerServiceServiceBuilder.requires(TxnServices.JBOSS_TXN_ARJUNA_OBJECTSTORE_ENVIRONMENT);
        transactionManagerServiceServiceBuilder.requires(XA_RESOURCE_RECOVERY_REGISTRY_CAPABILITY.getCapabilityServiceName());
        transactionManagerServiceServiceBuilder.setInitialMode(Mode.ACTIVE);
        transactionManagerServiceServiceBuilder.install();
    }

    private void checkIfNodeIdentifierIsDefault(final OperationContext context, final ModelNode model) throws OperationFailedException {
        final String nodeIdentifier = TransactionSubsystemRootResourceDefinition.NODE_IDENTIFIER.resolveModelAttribute(context, model).asString();
        final String defaultNodeIdentifier = TransactionSubsystemRootResourceDefinition.NODE_IDENTIFIER.getDefaultValue().asString();

        if (defaultNodeIdentifier.equals(nodeIdentifier)) {
            TransactionLogger.ROOT_LOGGER.nodeIdentifierIsSetToDefault(CommonAttributes.NODE_IDENTIFIER, context.getCurrentAddress().toCLIStyleString());
        }
    }

}
