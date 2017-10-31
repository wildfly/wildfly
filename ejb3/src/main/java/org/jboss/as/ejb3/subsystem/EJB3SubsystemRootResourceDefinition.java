/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
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
import org.jboss.as.ejb3.deployment.processors.EJBDefaultSecurityDomainProcessor;
import org.jboss.as.ejb3.deployment.processors.merging.MissingMethodPermissionsDenyAccessMergingProcessor;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.threads.ThreadFactoryResolver;
import org.jboss.as.threads.ThreadsServices;
import org.jboss.as.threads.UnboundedQueueThreadPoolResourceDefinition;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * {@link org.jboss.as.controller.ResourceDefinition} for the EJB3 subsystem's root management resource.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class EJB3SubsystemRootResourceDefinition extends SimpleResourceDefinition {
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
    static final SimpleAttributeDefinition DEFAULT_ENTITY_BEAN_INSTANCE_POOL =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.DEFAULT_ENTITY_BEAN_INSTANCE_POOL, ModelType.STRING, true)
                    .setAllowExpression(true).build();
    static final SimpleAttributeDefinition DEFAULT_ENTITY_BEAN_OPTIMISTIC_LOCKING =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.DEFAULT_ENTITY_BEAN_OPTIMISTIC_LOCKING, ModelType.BOOLEAN, true)
                    .setAllowExpression(true).build();

    static final SimpleAttributeDefinition DEFAULT_STATEFUL_BEAN_ACCESS_TIMEOUT =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.DEFAULT_STATEFUL_BEAN_ACCESS_TIMEOUT, ModelType.LONG, true)
                    .setXmlName(EJB3SubsystemXMLAttribute.DEFAULT_ACCESS_TIMEOUT.getLocalName())
                    .setDefaultValue(new ModelNode().set(5000)) // TODO: this should come from component description
                    .setAllowExpression(true) // we allow expression for setting a timeout value
                    .setValidator(new LongRangeValidator(1, Integer.MAX_VALUE, true, true))
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
                    .setDefaultValue(new ModelNode(false))
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
                    .setDefaultValue(new ModelNode(true))
                    .build();


    public static final SimpleAttributeDefinition DEFAULT_MISSING_METHOD_PERMISSIONS_DENY_ACCESS =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.DEFAULT_MISSING_METHOD_PERMISSIONS_DENY_ACCESS, ModelType.BOOLEAN, true)
                    .setAllowExpression(true)
                    .setDefaultValue(new ModelNode(false))
                    .build();

    public static final SimpleAttributeDefinition DISABLE_DEFAULT_EJB_PERMISSIONS =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.DISABLE_DEFAULT_EJB_PERMISSIONS, ModelType.BOOLEAN, true)
                    .setDeprecated(ModelVersion.create(3, 0, 0))
                    .setAllowExpression(true)
                    .setDefaultValue(new ModelNode(true))
                    .build();

    public static final SimpleAttributeDefinition ENABLE_GRACEFUL_TXN_SHUTDOWN =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.ENABLE_GRACEFUL_TXN_SHUTDOWN, ModelType.BOOLEAN, true)
                    .setAllowExpression(true)
                    .setDefaultValue(new ModelNode(false))
                    .build();

    public static final SimpleAttributeDefinition LOG_EJB_EXCEPTIONS =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.LOG_SYSTEM_EXCEPTIONS, ModelType.BOOLEAN, true)
                    .setAllowExpression(true)
                    .setDefaultValue(new ModelNode(true))
                    .build();

    public static final SimpleAttributeDefinition ALLOW_EJB_NAME_REGEX =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.ALLOW_EJB_NAME_REGEX, ModelType.BOOLEAN, true)
            .setDefaultValue(new ModelNode(false))
            .setAllowExpression(true)
            .build();

    public static final RuntimeCapability<Void> CLUSTERED_SINGLETON_CAPABILITY =  RuntimeCapability.Builder.of(
            "org.wildfly.ejb3.clustered.singleton", Void.class).build();

    public static final RuntimeCapability<Void> EJB_CAPABILITY =  RuntimeCapability.Builder.of(
            "org.wildfly.ejb3", Void.class).build();

    //We don't want to actually expose the service, we just want to use optional deps
    public static final RuntimeCapability<Void>  EJB_CLIENT_CONFIGURATOR = RuntimeCapability.Builder.of("org.wildfly.ejb3.remote.client-configurator", Void.class)
            .build();

    private static final ApplicationSecurityDomainDefinition APPLICATION_SECURITY_DOMAIN = ApplicationSecurityDomainDefinition.INSTANCE;
    private static final IdentityResourceDefinition IDENTITY = IdentityResourceDefinition.INSTANCE;
    private static final EJBDefaultSecurityDomainProcessor defaultSecurityDomainDeploymentProcessor = new EJBDefaultSecurityDomainProcessor(null,
            APPLICATION_SECURITY_DOMAIN.getKnownSecurityDomainFunction(), IDENTITY.getOutflowSecurityDomainsConfiguredSupplier());
    private static final MissingMethodPermissionsDenyAccessMergingProcessor missingMethodPermissionsDenyAccessMergingProcessor = new MissingMethodPermissionsDenyAccessMergingProcessor();


    private final boolean registerRuntimeOnly;
    private final PathManager pathManager;



    EJB3SubsystemRootResourceDefinition(boolean registerRuntimeOnly, PathManager pathManager) {
        super(PathElement.pathElement(SUBSYSTEM, EJB3Extension.SUBSYSTEM_NAME),
                EJB3Extension.getResourceDescriptionResolver(EJB3Extension.SUBSYSTEM_NAME),
                new EJB3SubsystemAdd(defaultSecurityDomainDeploymentProcessor, missingMethodPermissionsDenyAccessMergingProcessor), EJB3SubsystemRemove.INSTANCE,
                OperationEntry.Flag.RESTART_ALL_SERVICES, OperationEntry.Flag.RESTART_ALL_SERVICES);
        this.registerRuntimeOnly = registerRuntimeOnly;
        this.pathManager = pathManager;
    }

    static final SimpleAttributeDefinition[] ATTRIBUTES = {
            DEFAULT_ENTITY_BEAN_INSTANCE_POOL,
            DEFAULT_ENTITY_BEAN_OPTIMISTIC_LOCKING,
            DEFAULT_MDB_INSTANCE_POOL,
            DEFAULT_RESOURCE_ADAPTER_NAME,
            DEFAULT_SFSB_CACHE,
            DEFAULT_SINGLETON_BEAN_ACCESS_TIMEOUT,
            DEFAULT_SLSB_INSTANCE_POOL,
            DEFAULT_STATEFUL_BEAN_ACCESS_TIMEOUT,
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
            ALLOW_EJB_NAME_REGEX
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

        final EJBDefaultSecurityDomainWriteHandler defaultSecurityDomainWriteHandler = new EJBDefaultSecurityDomainWriteHandler(DEFAULT_SECURITY_DOMAIN, defaultSecurityDomainDeploymentProcessor);
        resourceRegistration.registerReadWriteAttribute(DEFAULT_SECURITY_DOMAIN, null, defaultSecurityDomainWriteHandler);

        final EJBDefaultMissingMethodPermissionsWriteHandler defaultMissingMethodPermissionsWriteHandler = new EJBDefaultMissingMethodPermissionsWriteHandler(DEFAULT_MISSING_METHOD_PERMISSIONS_DENY_ACCESS, missingMethodPermissionsDenyAccessMergingProcessor);
        resourceRegistration.registerReadWriteAttribute(DEFAULT_MISSING_METHOD_PERMISSIONS_DENY_ACCESS, null, defaultMissingMethodPermissionsWriteHandler);

        resourceRegistration.registerReadWriteAttribute(DISABLE_DEFAULT_EJB_PERMISSIONS, null, new AbstractWriteAttributeHandler<Void>() {
            protected boolean applyUpdateToRuntime(final OperationContext context, final ModelNode operation, final String attributeName, final ModelNode resolvedValue, final ModelNode currentValue, final HandbackHolder<Void> handbackHolder) throws OperationFailedException {
                if (resolvedValue.asBoolean()) {
                    throw EjbLogger.ROOT_LOGGER.disableDefaultEjbPermissionsCannotBeTrue();
                }
                return false;
            }

            protected void revertUpdateToRuntime(final OperationContext context, final ModelNode operation, final String attributeName, final ModelNode valueToRestore, final ModelNode valueToRevert, final Void handback) throws OperationFailedException {
            }
        });
        resourceRegistration.registerReadWriteAttribute(ENABLE_GRACEFUL_TXN_SHUTDOWN, null, EnableGracefulTxnShutdownWriteHandler.INSTANCE);
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
    protected void registerAddOperation(ManagementResourceRegistration registration, AbstractAddStepHandler handler, OperationEntry.Flag... flags) {
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
        subsystemRegistration.registerSubModel(EJB3RemoteResourceDefinition.INSTANCE);

        // subsystem=ejb3/service=async
        subsystemRegistration.registerSubModel(EJB3AsyncResourceDefinition.INSTANCE);

        // subsystem=ejb3/strict-max-bean-instance-pool=*
        subsystemRegistration.registerSubModel(StrictMaxPoolResourceDefinition.INSTANCE);

        subsystemRegistration.registerSubModel(CacheFactoryResourceDefinition.INSTANCE);
        subsystemRegistration.registerSubModel(PassivationStoreResourceDefinition.INSTANCE);
        subsystemRegistration.registerSubModel(FilePassivationStoreResourceDefinition.INSTANCE);
        subsystemRegistration.registerSubModel(ClusterPassivationStoreResourceDefinition.INSTANCE);

        // subsystem=ejb3/service=timerservice
        subsystemRegistration.registerSubModel(new TimerServiceResourceDefinition(pathManager));

        // subsystem=ejb3/thread-pool=*
        subsystemRegistration.registerSubModel(UnboundedQueueThreadPoolResourceDefinition.create(EJB3SubsystemModel.THREAD_POOL,
                new EJB3ThreadFactoryResolver(), EJB3SubsystemModel.BASE_THREAD_POOL_SERVICE_NAME, registerRuntimeOnly));

        // subsystem=ejb3/service=iiop
        subsystemRegistration.registerSubModel(EJB3IIOPResourceDefinition.INSTANCE);

        subsystemRegistration.registerSubModel(RemotingProfileResourceDefinition.INSTANCE);

        // subsystem=ejb3/mdb-delivery-group=*
        subsystemRegistration.registerSubModel(MdbDeliveryGroupResourceDefinition.INSTANCE);

        // subsystem=ejb3/application-security-domain=*
        subsystemRegistration.registerSubModel(APPLICATION_SECURITY_DOMAIN);

        subsystemRegistration.registerSubModel(IDENTITY);
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
