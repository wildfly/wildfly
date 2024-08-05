/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.subsystem;

import static org.jboss.as.controller.SimpleAttributeDefinitionBuilder.create;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.ObjectListAttributeDefinition;
import org.jboss.as.controller.ObjectTypeAttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.operations.global.ReadAttributeHandler;
import org.jboss.as.controller.operations.global.WriteAttributeHandler;
import org.jboss.as.controller.operations.validation.LongRangeValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.controller.services.path.PathManager;
import org.jboss.as.ejb3.component.pool.PoolConfig;
import org.jboss.as.ejb3.component.stateful.cache.StatefulSessionBeanCacheProvider;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.security.ApplicationSecurityDomainConfig;
import org.jboss.as.threads.EnhancedQueueExecutorResourceDefinition;
import org.jboss.as.threads.ThreadFactoryResolver;
import org.jboss.as.threads.ThreadsServices;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.service.descriptor.NullaryServiceDescriptor;
import org.wildfly.service.descriptor.UnaryServiceDescriptor;

/**
 * {@link org.jboss.as.controller.ResourceDefinition} for the EJB3 subsystem's root management resource.
 *
 * NOTE: References in this file to Enterprise JavaBeans (EJB) refer to the Jakarta Enterprise Beans unless otherwise noted.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class EJB3SubsystemRootResourceDefinition extends SimpleResourceDefinition {

    // TODO: put capability definitions up here
    public static final String DEFAULT_SLSB_POOL_CONFIG_CAPABILITY_NAME = "org.wildfly.ejb3.pool-config.slsb-default";
    public static final String DEFAULT_MDB_POOL_CONFIG_CAPABILITY_NAME = "org.wildfly.ejb3.pool-config.mdb-default";
    public static final String DEFAULT_ENTITY_POOL_CONFIG_CAPABILITY_NAME = "org.wildfly.ejb3.pool-config.entity-default";

    private static final String EJB_CAPABILITY_NAME = "org.wildfly.ejb3";
    private static final String EJB_CLIENT_CONFIGURATOR_CAPABILITY_NAME = "org.wildfly.ejb3.remote.client-configurator";
    private static final String TRANSACTION_GLOBAL_DEFAULT_LOCAL_PROVIDER_CAPABILITY_NAME = "org.wildfly.transactions.global-default-local-provider";

    // TODO Relocate this ServiceDescriptor the the threads subsystem in wildfly-core
    public static final UnaryServiceDescriptor<Executor> EXECUTOR_SERVICE_DESCRIPTOR = UnaryServiceDescriptor.of(ThreadsServices.getCapabilityBaseName(EJB3SubsystemModel.BASE_EJB_THREAD_POOL_NAME), Executor.class);

    static final SimpleAttributeDefinition DEFAULT_SLSB_INSTANCE_POOL =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.DEFAULT_SLSB_INSTANCE_POOL, ModelType.STRING, true)
                    .setAllowExpression(true).build();
    static final SimpleAttributeDefinition DEFAULT_MDB_INSTANCE_POOL =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.DEFAULT_MDB_INSTANCE_POOL, ModelType.STRING, true)
                    .setAllowExpression(true).build();
    static final SimpleAttributeDefinition DEFAULT_RESOURCE_ADAPTER_NAME =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.DEFAULT_RESOURCE_ADAPTER_NAME, ModelType.STRING, true)
                    .setDefaultValue(new ModelNode("activemq-ra"))
                    .setAllowExpression(true).build();
    @Deprecated
    static final SimpleAttributeDefinition DEFAULT_ENTITY_BEAN_INSTANCE_POOL =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.DEFAULT_ENTITY_BEAN_INSTANCE_POOL, ModelType.STRING, true)
                    .setDeprecated(EJB3Model.VERSION_10_0_0.getVersion())
                    .setAllowExpression(true).build();
    @Deprecated
    static final SimpleAttributeDefinition DEFAULT_ENTITY_BEAN_OPTIMISTIC_LOCKING =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.DEFAULT_ENTITY_BEAN_OPTIMISTIC_LOCKING, ModelType.BOOLEAN, true)
                    .setDeprecated(EJB3Model.VERSION_10_0_0.getVersion())
                    .setAllowExpression(true).build();

    static final SimpleAttributeDefinition DEFAULT_STATEFUL_BEAN_ACCESS_TIMEOUT =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.DEFAULT_STATEFUL_BEAN_ACCESS_TIMEOUT, ModelType.LONG, true)
                    .setXmlName(EJB3SubsystemXMLAttribute.DEFAULT_ACCESS_TIMEOUT.getLocalName())
                    .setDefaultValue(new ModelNode().set(5000)) // TODO: this should come from component description
                    .setAllowExpression(true) // we allow expression for setting a timeout value
                    .setValidator(new LongRangeValidator(1, Integer.MAX_VALUE, true, true))
                    .setFlags(AttributeAccess.Flag.RESTART_NONE)
                    .build();

    static final SimpleAttributeDefinition DEFAULT_STATEFUL_BEAN_SESSION_TIMEOUT =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.DEFAULT_STATEFUL_BEAN_SESSION_TIMEOUT, ModelType.LONG, true)
                    .setXmlName(EJB3SubsystemXMLAttribute.DEFAULT_SESSION_TIMEOUT.getLocalName())
                    .setAllowExpression(true) // we allow expression for setting a timeout value
                    .setValidator(new LongRangeValidator(-1, Integer.MAX_VALUE, true, true))
                    .setFlags(AttributeAccess.Flag.RESTART_NONE)
                    .build();

    static final SimpleAttributeDefinition DEFAULT_SINGLETON_BEAN_ACCESS_TIMEOUT =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.DEFAULT_SINGLETON_BEAN_ACCESS_TIMEOUT, ModelType.LONG, true)
                    .setXmlName(EJB3SubsystemXMLAttribute.DEFAULT_ACCESS_TIMEOUT.getLocalName())
                    .setDefaultValue(new ModelNode().set(5000)) // TODO: this should come from component description
                    .setAllowExpression(true) // we allow expression for setting a timeout value
                    .setValidator(new LongRangeValidator(1, Integer.MAX_VALUE, true, true))
                    .setFlags(AttributeAccess.Flag.RESTART_NONE)
                    .build();
    static final SimpleAttributeDefinition DEFAULT_SFSB_CACHE =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.DEFAULT_SFSB_CACHE, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .build();
    static final SimpleAttributeDefinition DEFAULT_SFSB_PASSIVATION_DISABLED_CACHE =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.DEFAULT_SFSB_PASSIVATION_DISABLED_CACHE, ModelType.STRING, true)
                    .setXmlName(EJB3SubsystemXMLAttribute.PASSIVATION_DISABLED_CACHE_REF.getLocalName())
                    .setAllowExpression(true)
                    .build();
    static final SimpleAttributeDefinition DEFAULT_CLUSTERED_SFSB_CACHE =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.DEFAULT_CLUSTERED_SFSB_CACHE, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setRequired(false)
                    .setDeprecated(ModelVersion.create(2))
                    .build();

    static final SimpleAttributeDefinition ENABLE_STATISTICS =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.ENABLE_STATISTICS, ModelType.BOOLEAN, true)
                    .setAllowExpression(true)
                    .setDeprecated(ModelVersion.create(5))
                    .setFlags(AttributeAccess.Flag.ALIAS)
                    .build();

    static final SimpleAttributeDefinition STATISTICS_ENABLED =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.STATISTICS_ENABLED, ModelType.BOOLEAN, true)
                    .setAllowExpression(true)
                    .setDefaultValue(ModelNode.FALSE)
                    .build();

    static final SimpleAttributeDefinition DEFAULT_DISTINCT_NAME =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.DEFAULT_DISTINCT_NAME, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setValidator(new StringLengthValidator(0, true))
                    .build();

    public static final SimpleAttributeDefinition DEFAULT_SECURITY_DOMAIN =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.DEFAULT_SECURITY_DOMAIN, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.SECURITY_DOMAIN_REF)
                    .setNullSignificant(true)
                    .build();

    public static final SimpleAttributeDefinition PASS_BY_VALUE =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.IN_VM_REMOTE_INTERFACE_INVOCATION_PASS_BY_VALUE, ModelType.BOOLEAN, true)
                    .setAllowExpression(true)
                    .setDefaultValue(ModelNode.TRUE)
                    .build();


    public static final SimpleAttributeDefinition DEFAULT_MISSING_METHOD_PERMISSIONS_DENY_ACCESS =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.DEFAULT_MISSING_METHOD_PERMISSIONS_DENY_ACCESS, ModelType.BOOLEAN, true)
                    .setAllowExpression(true)
                    .setDefaultValue(ModelNode.FALSE)
                    .build();

    public static final SimpleAttributeDefinition DISABLE_DEFAULT_EJB_PERMISSIONS =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.DISABLE_DEFAULT_EJB_PERMISSIONS, ModelType.BOOLEAN, true)
                    .setDeprecated(ModelVersion.create(3, 0, 0))
                    .setAllowExpression(true)
                    .setDefaultValue(ModelNode.TRUE)
                    .build();

    public static final SimpleAttributeDefinition ENABLE_GRACEFUL_TXN_SHUTDOWN =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.ENABLE_GRACEFUL_TXN_SHUTDOWN, ModelType.BOOLEAN, true)
                    .setAllowExpression(true)
                    .setDefaultValue(ModelNode.FALSE)
                    .build();

    public static final SimpleAttributeDefinition LOG_EJB_EXCEPTIONS =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.LOG_SYSTEM_EXCEPTIONS, ModelType.BOOLEAN, true)
                    .setAllowExpression(true)
                    .setDefaultValue(ModelNode.TRUE)
                    .build();

    public static final SimpleAttributeDefinition ALLOW_EJB_NAME_REGEX =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.ALLOW_EJB_NAME_REGEX, ModelType.BOOLEAN, true)
            .setDefaultValue(ModelNode.FALSE)
            .setAllowExpression(true)
            .build();

    private static final ObjectTypeAttributeDefinition SERVER_INTERCEPTOR = ObjectTypeAttributeDefinition.Builder.of(EJB3SubsystemModel.SERVER_INTERCEPTOR,
            create(EJB3SubsystemModel.CLASS, ModelType.STRING, false)
                    .setAllowExpression(false)
                    .build(),
            create(EJB3SubsystemModel.MODULE, ModelType.STRING, false)
                    .setAllowExpression(false)
                    .build())
            .build();

    public static final ObjectListAttributeDefinition SERVER_INTERCEPTORS = ObjectListAttributeDefinition.Builder.of(EJB3SubsystemModel.SERVER_INTERCEPTORS, SERVER_INTERCEPTOR)
            .setRequired(false)
            .setAllowExpression(false)
            .setMinSize(1)
            .setMaxSize(Integer.MAX_VALUE)
            .build();

    private static final ObjectTypeAttributeDefinition CLIENT_INTERCEPTOR = ObjectTypeAttributeDefinition.Builder.of(EJB3SubsystemModel.CLIENT_INTERCEPTOR,
            create(EJB3SubsystemModel.CLASS, ModelType.STRING, false)
                    .setAllowExpression(false)
                    .build(),
            create(EJB3SubsystemModel.MODULE, ModelType.STRING, false)
                    .setAllowExpression(false)
                    .build())
            .build();

    public static final ObjectListAttributeDefinition CLIENT_INTERCEPTORS = ObjectListAttributeDefinition.Builder.of(EJB3SubsystemModel.CLIENT_INTERCEPTORS, CLIENT_INTERCEPTOR)
            .setRequired(false)
            .setAllowExpression(false)
            .setMinSize(1)
            .setMaxSize(Integer.MAX_VALUE)
            .build();

    public static final NullaryServiceDescriptor<Void> CLUSTERED_SINGLETON_BARRIER = NullaryServiceDescriptor.of("org.wildfly.ejb3.clustered.singleton.barrier", Void.class);
    public static final NullaryServiceDescriptor<Void> CLUSTERED_SINGLETON = NullaryServiceDescriptor.of("org.wildfly.ejb3.clustered.singleton", Void.class);
    static final RuntimeCapability<Void> CLUSTERED_SINGLETON_CAPABILITY =  RuntimeCapability.Builder.of(CLUSTERED_SINGLETON).build();

    public static final RuntimeCapability<Void> EJB_CAPABILITY =  RuntimeCapability.Builder.of(EJB_CAPABILITY_NAME, Void.class)
            // EJBComponentDescription adds a create dependency on the local tx provider to all components,
            // so in the absence of relevant finer grained EJB capabilities, we'll say that EJB overall requires the local provider
            .addRequirements(TRANSACTION_GLOBAL_DEFAULT_LOCAL_PROVIDER_CAPABILITY_NAME)
            .build();

    //We don't want to actually expose the service, we just want to use optional deps
    public static final RuntimeCapability<Void> EJB_CLIENT_CONFIGURATOR_CAPABILITY = RuntimeCapability.Builder.of(EJB_CLIENT_CONFIGURATOR_CAPABILITY_NAME, Void.class)
            .build();

    // default pool capabilities, defined here but registered conditionally
    // TODO: these are not guaranteed to be defined but their use as dependants is guarded by predicate which knows if the pool is available or not
    public static final RuntimeCapability<Void> DEFAULT_SLSB_POOL_CONFIG_CAPABILITY =
            RuntimeCapability.Builder.of(DEFAULT_SLSB_POOL_CONFIG_CAPABILITY_NAME, PoolConfig.class)
                    .build();

    public static final RuntimeCapability<Void> DEFAULT_MDB_POOL_CONFIG_CAPABILITY =
            RuntimeCapability.Builder.of(DEFAULT_MDB_POOL_CONFIG_CAPABILITY_NAME, PoolConfig.class)
                    .build();

    public static final RuntimeCapability<Void> DEFAULT_ENTITY_POOL_CONFIG_CAPABILITY =
            RuntimeCapability.Builder.of(DEFAULT_ENTITY_POOL_CONFIG_CAPABILITY_NAME, PoolConfig.class)
                    .build();

    static final RuntimeCapability<Void> DEFAULT_STATEFUL_BEAN_CACHE = RuntimeCapability.Builder.of(StatefulSessionBeanCacheProvider.DEFAULT_SERVICE_DESCRIPTOR).build();
    static final RuntimeCapability<Void> PASSIVATION_DISABLED_STATEFUL_BEAN_CACHE = RuntimeCapability.Builder.of(StatefulSessionBeanCacheProvider.PASSIVATION_DISABLED_SERVICE_DESCRIPTOR).build();

    private final boolean registerRuntimeOnly;
    private final PathManager pathManager;
    private final AtomicReference<String> defaultSecurityDomainName;
    private final Set<ApplicationSecurityDomainConfig> knownApplicationSecurityDomains;
    private final List<String> outflowSecurityDomains;
    private final AtomicBoolean denyAccessByDefault;

    EJB3SubsystemRootResourceDefinition(boolean registerRuntimeOnly, PathManager pathManager) {
        this(registerRuntimeOnly, pathManager, new AtomicReference<>(), new CopyOnWriteArraySet<>(), new CopyOnWriteArrayList<>(), new AtomicBoolean(false));
    }

    private EJB3SubsystemRootResourceDefinition(boolean registerRuntimeOnly, PathManager pathManager, AtomicReference<String> defaultSecurityDomainName, Set<ApplicationSecurityDomainConfig> knownApplicationSecurityDomains, List<String> outflowSecurityDomains, AtomicBoolean denyAccessByDefault) {
        super(new Parameters(PathElement.pathElement(SUBSYSTEM, EJB3Extension.SUBSYSTEM_NAME), EJB3Extension.getResourceDescriptionResolver(EJB3Extension.SUBSYSTEM_NAME))
                .setAddHandler(new EJB3SubsystemAdd(defaultSecurityDomainName, knownApplicationSecurityDomains, outflowSecurityDomains, denyAccessByDefault))
                .setRemoveHandler(EJB3SubsystemRemove.INSTANCE)
                .setAddRestartLevel(OperationEntry.Flag.RESTART_ALL_SERVICES)
                .setRemoveRestartLevel(OperationEntry.Flag.RESTART_ALL_SERVICES)
                .setCapabilities(CLUSTERED_SINGLETON_CAPABILITY, EJB_CLIENT_CONFIGURATOR_CAPABILITY, EJB_CAPABILITY, DEFAULT_STATEFUL_BEAN_CACHE, PASSIVATION_DISABLED_STATEFUL_BEAN_CACHE)
        );
        this.registerRuntimeOnly = registerRuntimeOnly;
        this.pathManager = pathManager;
        this.defaultSecurityDomainName = defaultSecurityDomainName;
        this.knownApplicationSecurityDomains = knownApplicationSecurityDomains;
        this.outflowSecurityDomains = outflowSecurityDomains;
        this.denyAccessByDefault = denyAccessByDefault;
    }

    static final AttributeDefinition[] ATTRIBUTES = {
            DEFAULT_ENTITY_BEAN_INSTANCE_POOL,
            DEFAULT_ENTITY_BEAN_OPTIMISTIC_LOCKING,
            DEFAULT_MDB_INSTANCE_POOL,
            DEFAULT_RESOURCE_ADAPTER_NAME,
            DEFAULT_SFSB_CACHE,
            DEFAULT_SINGLETON_BEAN_ACCESS_TIMEOUT,
            DEFAULT_SLSB_INSTANCE_POOL,
            DEFAULT_STATEFUL_BEAN_ACCESS_TIMEOUT,
            DEFAULT_STATEFUL_BEAN_SESSION_TIMEOUT,
            STATISTICS_ENABLED,
            ENABLE_STATISTICS,
            PASS_BY_VALUE,
            DEFAULT_DISTINCT_NAME,
            DEFAULT_SECURITY_DOMAIN,
            DEFAULT_MISSING_METHOD_PERMISSIONS_DENY_ACCESS,
            DEFAULT_SFSB_PASSIVATION_DISABLED_CACHE,
            DISABLE_DEFAULT_EJB_PERMISSIONS,
            ENABLE_GRACEFUL_TXN_SHUTDOWN,
            LOG_EJB_EXCEPTIONS,
            ALLOW_EJB_NAME_REGEX,
            SERVER_INTERCEPTORS,
            CLIENT_INTERCEPTORS
    };

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerReadOnlyAttribute(DEFAULT_CLUSTERED_SFSB_CACHE, new SimpleAliasReadAttributeHandler(DEFAULT_SFSB_CACHE));
        resourceRegistration.registerReadWriteAttribute(DEFAULT_SFSB_CACHE, null, EJB3SubsystemDefaultCacheWriteHandler.SFSB_CACHE);
        resourceRegistration.registerReadWriteAttribute(DEFAULT_SFSB_PASSIVATION_DISABLED_CACHE, null, EJB3SubsystemDefaultCacheWriteHandler.SFSB_PASSIVATION_DISABLED_CACHE);
        resourceRegistration.registerReadWriteAttribute(DEFAULT_SLSB_INSTANCE_POOL, null, EJB3SubsystemDefaultPoolWriteHandler.SLSB_POOL);
        resourceRegistration.registerReadWriteAttribute(DEFAULT_MDB_INSTANCE_POOL, null, EJB3SubsystemDefaultPoolWriteHandler.MDB_POOL);
        resourceRegistration.registerReadWriteAttribute(DEFAULT_ENTITY_BEAN_INSTANCE_POOL, null, EJB3SubsystemDefaultPoolWriteHandler.ENTITY_BEAN_POOL);
        resourceRegistration.registerReadWriteAttribute(DEFAULT_ENTITY_BEAN_OPTIMISTIC_LOCKING, null, EJB3SubsystemDefaultEntityBeanOptimisticLockingWriteHandler.INSTANCE);
        resourceRegistration.registerReadWriteAttribute(DEFAULT_RESOURCE_ADAPTER_NAME, null, DefaultResourceAdapterWriteHandler.INSTANCE);
        resourceRegistration.registerReadWriteAttribute(DEFAULT_SINGLETON_BEAN_ACCESS_TIMEOUT, null, DefaultSingletonBeanAccessTimeoutWriteHandler.INSTANCE);
        resourceRegistration.registerReadWriteAttribute(DEFAULT_STATEFUL_BEAN_ACCESS_TIMEOUT, null, DefaultStatefulBeanAccessTimeoutWriteHandler.INSTANCE);
        resourceRegistration.registerReadWriteAttribute(DEFAULT_STATEFUL_BEAN_SESSION_TIMEOUT, null, DefaultStatefulBeanSessionTimeoutWriteHandler.INSTANCE);
        resourceRegistration.registerReadWriteAttribute(ENABLE_STATISTICS, (context, operation) -> {
            ModelNode aliasOp = operation.clone();
            aliasOp.get("name").set(EJB3SubsystemModel.STATISTICS_ENABLED);
            context.addStep(aliasOp, ReadAttributeHandler.INSTANCE, OperationContext.Stage.MODEL, true);
        }, (context, operation) -> {
            ModelNode aliasOp = operation.clone();
            aliasOp.get("name").set(EJB3SubsystemModel.STATISTICS_ENABLED);
            context.addStep(aliasOp, WriteAttributeHandler.INSTANCE, OperationContext.Stage.MODEL, true);
        });
        resourceRegistration.registerReadWriteAttribute(STATISTICS_ENABLED, null, StatisticsEnabledWriteHandler.INSTANCE);
        resourceRegistration.registerReadWriteAttribute(PASS_BY_VALUE, null, EJBRemoteInvocationPassByValueWriteHandler.INSTANCE);
        resourceRegistration.registerReadWriteAttribute(DEFAULT_DISTINCT_NAME, null, EJBDefaultDistinctNameWriteHandler.INSTANCE);
        resourceRegistration.registerReadWriteAttribute(LOG_EJB_EXCEPTIONS, null, ExceptionLoggingWriteHandler.INSTANCE);
        resourceRegistration.registerReadWriteAttribute(ALLOW_EJB_NAME_REGEX, null, EJBNameRegexWriteHandler.INSTANCE);

        final EJBDefaultSecurityDomainWriteHandler defaultSecurityDomainWriteHandler = new EJBDefaultSecurityDomainWriteHandler(DEFAULT_SECURITY_DOMAIN, this.defaultSecurityDomainName);
        resourceRegistration.registerReadWriteAttribute(DEFAULT_SECURITY_DOMAIN, null, defaultSecurityDomainWriteHandler);

        final EJBDefaultMissingMethodPermissionsWriteHandler defaultMissingMethodPermissionsWriteHandler = new EJBDefaultMissingMethodPermissionsWriteHandler(DEFAULT_MISSING_METHOD_PERMISSIONS_DENY_ACCESS, this.denyAccessByDefault);
        resourceRegistration.registerReadWriteAttribute(DEFAULT_MISSING_METHOD_PERMISSIONS_DENY_ACCESS, null, defaultMissingMethodPermissionsWriteHandler);

        resourceRegistration.registerReadWriteAttribute(DISABLE_DEFAULT_EJB_PERMISSIONS, null, new AbstractWriteAttributeHandler<Void>() {
            @Override
            protected boolean applyUpdateToRuntime(final OperationContext context, final ModelNode operation, final String attributeName, final ModelNode resolvedValue, final ModelNode currentValue, final HandbackHolder<Void> handbackHolder) throws OperationFailedException {
                if (resolvedValue.asBoolean()) {
                    throw EjbLogger.ROOT_LOGGER.disableDefaultEjbPermissionsCannotBeTrue();
                }
                return false;
            }

            @Override
            protected void revertUpdateToRuntime(final OperationContext context, final ModelNode operation, final String attributeName, final ModelNode valueToRestore, final ModelNode valueToRevert, final Void handback) throws OperationFailedException {
            }
        });
        resourceRegistration.registerReadWriteAttribute(ENABLE_GRACEFUL_TXN_SHUTDOWN, null, EnableGracefulTxnShutdownWriteHandler.INSTANCE);
        resourceRegistration.registerReadWriteAttribute(SERVER_INTERCEPTORS, null,  ReloadRequiredWriteAttributeHandler.INSTANCE);
        resourceRegistration.registerReadWriteAttribute(CLIENT_INTERCEPTORS, null,  ReloadRequiredWriteAttributeHandler.INSTANCE);
    }

    @Override
    public void registerOperations(ManagementResourceRegistration subsystemRegistration) {
        super.registerOperations(subsystemRegistration);
        subsystemRegistration.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION, GenericSubsystemDescribeHandler.INSTANCE);
    }

    /**
     * Overrides the default impl to use a special definition of the add op that includes additional parameter
     * {@link #DEFAULT_CLUSTERED_SFSB_CACHE}
     * {@inheritDoc}
     */
    @Override
    protected void registerAddOperation(ManagementResourceRegistration registration, OperationStepHandler handler, OperationEntry.Flag... flags) {
        OperationDefinition od = new SimpleOperationDefinitionBuilder(ADD, getResourceDescriptionResolver())
                .setParameters(ATTRIBUTES)
                .addParameter(DEFAULT_CLUSTERED_SFSB_CACHE)
                .withFlags(flags)
                .build();
        registration.registerOperationHandler(od, handler);
    }

    @Override
    public void registerChildren(ManagementResourceRegistration subsystemRegistration) {

        // subsystem=ejb3/service=remote
        subsystemRegistration.registerSubModel(new EJB3RemoteResourceDefinition());

        // subsystem=ejb3/service=async
        subsystemRegistration.registerSubModel(new EJB3AsyncResourceDefinition());

        // subsystem=ejb3/strict-max-bean-instance-pool=*
        subsystemRegistration.registerSubModel(new StrictMaxPoolResourceDefinition());

        // subsystem=ejb3/{cache=*, simple-cache=*, distributable-cache=*}
        subsystemRegistration.registerSubModel(new LegacyCacheFactoryResourceDefinition());
        new SimpleStatefulSessionBeanCacheProviderResourceDefinition().register(subsystemRegistration);
        new DistributableStatefulSessionBeanCacheProviderResourceDefinition().register(subsystemRegistration);

        subsystemRegistration.registerSubModel(new PassivationStoreResourceDefinition());
        subsystemRegistration.registerSubModel(new FilePassivationStoreResourceDefinition());
        subsystemRegistration.registerSubModel(new ClusterPassivationStoreResourceDefinition());

        // subsystem=ejb3/service=timerservice
        subsystemRegistration.registerSubModel(new TimerServiceResourceDefinition(pathManager));

        // subsystem=ejb3/thread-pool=*
        subsystemRegistration.registerSubModel(EnhancedQueueExecutorResourceDefinition.create(
                PathElement.pathElement(EJB3SubsystemModel.THREAD_POOL),
                new EJB3ThreadFactoryResolver(),
                EJB3SubsystemModel.BASE_THREAD_POOL_SERVICE_NAME,
                registerRuntimeOnly,
                RuntimeCapability.Builder.of(EXECUTOR_SERVICE_DESCRIPTOR).build(),
                false));

        // subsystem=ejb3/service=iiop
        subsystemRegistration.registerSubModel(new EJB3IIOPResourceDefinition());

        subsystemRegistration.registerSubModel(new RemotingProfileResourceDefinition());

        // subsystem=ejb3/mdb-delivery-group=*
        subsystemRegistration.registerSubModel(new MdbDeliveryGroupResourceDefinition());

        // subsystem=ejb3/application-security-domain=*
        subsystemRegistration.registerSubModel(new ApplicationSecurityDomainDefinition(this.knownApplicationSecurityDomains));

        subsystemRegistration.registerSubModel(new IdentityResourceDefinition(this.outflowSecurityDomains));
    }

    private static class EJB3ThreadFactoryResolver extends ThreadFactoryResolver.SimpleResolver {

        private EJB3ThreadFactoryResolver() {
            super(ThreadsServices.FACTORY);
        }

        @Override
        protected String getThreadGroupName(String threadPoolName) {
            return "EJB " + threadPoolName;
        }
    }

}
