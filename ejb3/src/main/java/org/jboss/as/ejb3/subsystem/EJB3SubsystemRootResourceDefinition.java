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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.UNDEFINE_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.operations.validation.LongRangeValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.controller.services.path.PathManager;
import org.jboss.as.controller.transform.DiscardUndefinedAttributesTransformer;
import org.jboss.as.controller.transform.RejectExpressionValuesTransformer;
import org.jboss.as.controller.transform.TransformersSubRegistration;
import org.jboss.as.controller.transform.chained.ChainedOperationTransformer;
import org.jboss.as.controller.transform.chained.ChainedResourceTransformer;
import org.jboss.as.ejb3.deployment.processors.EJBDefaultSecurityDomainProcessor;
import org.jboss.as.threads.ThreadFactoryResolver;
import org.jboss.as.threads.ThreadsServices;
import org.jboss.as.threads.UnboundedQueueThreadPoolResourceDefinition;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * {@link ResourceDefinition} for the EJB3 subsystem's root management resource.
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
                    .setDefaultValue(new ModelNode().set("hornetq-ra"))
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
    static final SimpleAttributeDefinition DEFAULT_CLUSTERED_SFSB_CACHE =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.DEFAULT_CLUSTERED_SFSB_CACHE, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setAllowNull(true)
                    .build();

    static final SimpleAttributeDefinition ENABLE_STATISTICS =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.ENABLE_STATISTICS, ModelType.BOOLEAN, true)
                    .setAllowExpression(true)
                    .build();

    static final SimpleAttributeDefinition DEFAULT_DISTINCT_NAME =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.DEFAULT_DISTINCT_NAME, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setValidator(new StringLengthValidator(0, true))
                    .build();

    public static final SimpleAttributeDefinition DEFAULT_SECURITY_DOMAIN =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.DEFAULT_SECURITY_DOMAIN, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .build();

    public static final SimpleAttributeDefinition PASS_BY_VALUE =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.IN_VM_REMOTE_INTERFACE_INVOCATION_PASS_BY_VALUE, ModelType.BOOLEAN, true)
                    .setAllowExpression(true)
                    .setDefaultValue(new ModelNode().set("true"))
                    .build();


    private static final EJBDefaultSecurityDomainProcessor defaultSecurityDomainDeploymentProcessor = new EJBDefaultSecurityDomainProcessor(null);

    private final boolean registerRuntimeOnly;
    private final PathManager pathManager;

    EJB3SubsystemRootResourceDefinition(boolean registerRuntimeOnly, PathManager pathManager) {
        super(PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, EJB3Extension.SUBSYSTEM_NAME),
                EJB3Extension.getResourceDescriptionResolver(EJB3Extension.SUBSYSTEM_NAME),
                new EJB3SubsystemAdd(defaultSecurityDomainDeploymentProcessor), EJB3SubsystemRemove.INSTANCE,
                OperationEntry.Flag.RESTART_ALL_SERVICES, OperationEntry.Flag.RESTART_ALL_SERVICES);
        this.registerRuntimeOnly = registerRuntimeOnly;
        this.pathManager = pathManager;
    }

    static final SimpleAttributeDefinition[] ATTRIBUTES = {
            DEFAULT_CLUSTERED_SFSB_CACHE,
            DEFAULT_ENTITY_BEAN_INSTANCE_POOL,
            DEFAULT_ENTITY_BEAN_OPTIMISTIC_LOCKING,
            DEFAULT_MDB_INSTANCE_POOL,
            DEFAULT_RESOURCE_ADAPTER_NAME,
            DEFAULT_SFSB_CACHE,
            DEFAULT_SINGLETON_BEAN_ACCESS_TIMEOUT,
            DEFAULT_SLSB_INSTANCE_POOL,
            DEFAULT_STATEFUL_BEAN_ACCESS_TIMEOUT,
            ENABLE_STATISTICS,
            PASS_BY_VALUE,
            DEFAULT_DISTINCT_NAME,
            DEFAULT_SECURITY_DOMAIN
    };

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerReadWriteAttribute(DEFAULT_SFSB_CACHE, null, EJB3SubsystemDefaultCacheWriteHandler.SFSB_CACHE);
        resourceRegistration.registerReadWriteAttribute(DEFAULT_CLUSTERED_SFSB_CACHE, null, EJB3SubsystemDefaultCacheWriteHandler.CLUSTERED_SFSB_CACHE);
        resourceRegistration.registerReadWriteAttribute(DEFAULT_SLSB_INSTANCE_POOL, null, EJB3SubsystemDefaultPoolWriteHandler.SLSB_POOL);
        resourceRegistration.registerReadWriteAttribute(DEFAULT_MDB_INSTANCE_POOL, null, EJB3SubsystemDefaultPoolWriteHandler.MDB_POOL);
        resourceRegistration.registerReadWriteAttribute(DEFAULT_ENTITY_BEAN_INSTANCE_POOL, null, EJB3SubsystemDefaultPoolWriteHandler.ENTITY_BEAN_POOL);
        resourceRegistration.registerReadWriteAttribute(DEFAULT_ENTITY_BEAN_OPTIMISTIC_LOCKING, null, EJB3SubsystemDefaultEntityBeanOptimisticLockingWriteHandler.INSTANCE);
        resourceRegistration.registerReadWriteAttribute(DEFAULT_RESOURCE_ADAPTER_NAME, null, DefaultResourceAdapterWriteHandler.INSTANCE);
        resourceRegistration.registerReadWriteAttribute(DEFAULT_SINGLETON_BEAN_ACCESS_TIMEOUT, null, DefaultSingletonBeanAccessTimeoutWriteHandler.INSTANCE);
        resourceRegistration.registerReadWriteAttribute(DEFAULT_STATEFUL_BEAN_ACCESS_TIMEOUT, null, DefaultStatefulBeanAccessTimeoutWriteHandler.INSTANCE);
        resourceRegistration.registerReadWriteAttribute(ENABLE_STATISTICS, null, EnableStatisticsWriteHandler.INSTANCE);
        resourceRegistration.registerReadWriteAttribute(PASS_BY_VALUE, null, EJBRemoteInvocationPassByValueWriteHandler.INSTANCE);
        resourceRegistration.registerReadWriteAttribute(DEFAULT_DISTINCT_NAME, null, EJBDefaultDistinctNameWriteHandler.INSTANCE);

        final EJBDefaultSecurityDomainWriteHandler defaultSecurityDomainWriteHandler = new EJBDefaultSecurityDomainWriteHandler(DEFAULT_SECURITY_DOMAIN, defaultSecurityDomainDeploymentProcessor);
        resourceRegistration.registerReadWriteAttribute(DEFAULT_SECURITY_DOMAIN, null, defaultSecurityDomainWriteHandler);
    }

    @Override
    public void registerOperations(ManagementResourceRegistration subsystemRegistration) {
        super.registerOperations(subsystemRegistration);
        subsystemRegistration.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION, GenericSubsystemDescribeHandler.INSTANCE);
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
        subsystemRegistration.registerSubModel(FilePassivationStoreResourceDefinition.INSTANCE);
        subsystemRegistration.registerSubModel(ClusterPassivationStoreResourceDefinition.INSTANCE);

        // subsystem=ejb3/service=timerservice
        subsystemRegistration.registerSubModel(new TimerServiceResourceDefinition(pathManager));

        // subsystem=ejb3/thread-pool=*
        subsystemRegistration.registerSubModel(UnboundedQueueThreadPoolResourceDefinition.create(EJB3SubsystemModel.THREAD_POOL,
                new EJB3ThreadFactoryResolver(), EJB3SubsystemModel.BASE_THREAD_POOL_SERVICE_NAME, registerRuntimeOnly));

        // subsystem=ejb3/service=iiop
        subsystemRegistration.registerSubModel(EJB3IIOPResourceDefinition.INSTANCE);
    }

    static void registerTransformers(SubsystemRegistration subsystemRegistration) {
        registerTransformers_1_1_0(subsystemRegistration);
    }

    private static void registerTransformers_1_1_0(SubsystemRegistration subsystemRegistration) {

        ModelVersion subsystem110 = ModelVersion.create(1, 1);

        RejectExpressionValuesTransformer rejectTransformer = new RejectExpressionValuesTransformer(EJB3SubsystemRootResourceDefinition.ENABLE_STATISTICS);
        DiscardUndefinedAttributesTransformer discardTransformer = new DiscardUndefinedAttributesTransformer(EJB3SubsystemRootResourceDefinition.DEFAULT_SECURITY_DOMAIN);
        ChainedResourceTransformer ctr = new ChainedResourceTransformer(rejectTransformer.getChainedTransformer(), discardTransformer);
        final TransformersSubRegistration transformers110 = subsystemRegistration.registerModelTransformers(subsystem110, ctr);
        transformers110.registerOperationTransformer(ADD, new ChainedOperationTransformer(rejectTransformer, discardTransformer));
        transformers110.registerOperationTransformer(WRITE_ATTRIBUTE_OPERATION,
                new ChainedOperationTransformer(rejectTransformer.getWriteAttributeTransformer(), discardTransformer.getWriteAttributeTransformer()));
        transformers110.registerOperationTransformer(UNDEFINE_ATTRIBUTE_OPERATION, discardTransformer);

        UnboundedQueueThreadPoolResourceDefinition.registerTransformers1_0(transformers110, EJB3SubsystemModel.THREAD_POOL);

        StrictMaxPoolResourceDefinition.registerTransformers_1_1_0(transformers110);
        FilePassivationStoreResourceDefinition.registerTransformers_1_1_0(transformers110);
        ClusterPassivationStoreResourceDefinition.registerTransformers_1_1_0(transformers110);
        TimerServiceResourceDefinition.registerTransformers_1_1_0(transformers110);
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
