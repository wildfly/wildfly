/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
package org.jboss.as.clustering.infinispan.subsystem;

import org.jboss.as.clustering.controller.MetricHandler;
import org.jboss.as.clustering.controller.Operations;
import org.jboss.as.clustering.controller.transform.OperationTransformer;
import org.jboss.as.clustering.controller.transform.SimpleOperationTransformer;
import org.jboss.as.clustering.controller.validation.ModuleIdentifierValidator;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.operations.global.ListOperations;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.services.path.PathManager;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Resource description for the addressable resource /subsystem=infinispan/cache-container=X
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 * @author Paul Ferraro
 */
public class CacheContainerResourceDefinition extends SimpleResourceDefinition {

    static final PathElement WILDCARD_PATH = pathElement(PathElement.WILDCARD_VALUE);

    static PathElement pathElement(String containerName) {
        return PathElement.pathElement(ModelKeys.CACHE_CONTAINER, containerName);
    }

    static final StringListAttributeDefinition ALIASES = new StringListAttributeDefinition.Builder(ModelKeys.ALIASES)
            .setAllowNull(true)
            .build();

    static final SimpleAttributeDefinition MODULE = new SimpleAttributeDefinitionBuilder(ModelKeys.MODULE, ModelType.STRING, true)
            .setXmlName(Attribute.MODULE.getLocalName())
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setValidator(new ModuleIdentifierValidator(true))
            .setDefaultValue(new ModelNode().set("org.jboss.as.clustering.infinispan"))
            .build();

    // make default-cache non required (AS7-3488)
    static final SimpleAttributeDefinition DEFAULT_CACHE = new SimpleAttributeDefinitionBuilder(ModelKeys.DEFAULT_CACHE, ModelType.STRING, true)
            .setXmlName(Attribute.DEFAULT_CACHE.getLocalName())
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    static final SimpleAttributeDefinition EVICTION_EXECUTOR = new SimpleAttributeDefinitionBuilder(ModelKeys.EVICTION_EXECUTOR, ModelType.STRING, true)
            .setXmlName(Attribute.EVICTION_EXECUTOR.getLocalName())
            .setAllowExpression(false)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    static final SimpleAttributeDefinition JNDI_NAME = new SimpleAttributeDefinitionBuilder(ModelKeys.JNDI_NAME, ModelType.STRING, true)
            .setXmlName(Attribute.JNDI_NAME.getLocalName())
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    static final SimpleAttributeDefinition LISTENER_EXECUTOR = new SimpleAttributeDefinitionBuilder(ModelKeys.LISTENER_EXECUTOR, ModelType.STRING, true)
            .setXmlName(Attribute.LISTENER_EXECUTOR.getLocalName())
            .setAllowExpression(false)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    static final SimpleAttributeDefinition REPLICATION_QUEUE_EXECUTOR = new SimpleAttributeDefinitionBuilder(ModelKeys.REPLICATION_QUEUE_EXECUTOR, ModelType.STRING, true)
            .setXmlName(Attribute.REPLICATION_QUEUE_EXECUTOR.getLocalName())
            .setAllowExpression(false)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    static final SimpleAttributeDefinition START = new SimpleAttributeDefinitionBuilder(ModelKeys.START, ModelType.STRING, true)
            .setXmlName(Attribute.START.getLocalName())
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setValidator(new EnumValidator<>(StartMode.class, true, false))
            .setDefaultValue(new ModelNode().set(StartMode.LAZY.name()))
            .setDeprecated(InfinispanModel.VERSION_3_0_0.getVersion())
            .build();

    static final SimpleAttributeDefinition STATISTICS_ENABLED = new SimpleAttributeDefinitionBuilder(ModelKeys.STATISTICS_ENABLED, ModelType.BOOLEAN, true)
            .setXmlName(Attribute.STATISTICS_ENABLED.getLocalName())
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .setDefaultValue(new ModelNode().set(false))
            .build();

    static final AttributeDefinition[] ATTRIBUTES = new AttributeDefinition[] {
            DEFAULT_CACHE, ALIASES, JNDI_NAME, START, LISTENER_EXECUTOR, EVICTION_EXECUTOR, REPLICATION_QUEUE_EXECUTOR, MODULE, STATISTICS_ENABLED
    };


    static final AttributeDefinition ALIAS = new SimpleAttributeDefinitionBuilder(ModelKeys.NAME, ModelType.STRING)
            .setAllowExpression(false)
            .build();

    static final OperationDefinition ALIAS_ADD = new SimpleOperationDefinitionBuilder("add-alias", new InfinispanResourceDescriptionResolver(ModelKeys.CACHE_CONTAINER))
            .setParameters(ALIAS)
            .setDeprecated(InfinispanModel.VERSION_3_0_0.getVersion())
            .build();

    static final OperationDefinition ALIAS_REMOVE = new SimpleOperationDefinitionBuilder("remove-alias", new InfinispanResourceDescriptionResolver(ModelKeys.CACHE_CONTAINER))
            .setParameters(ALIAS)
            .setDeprecated(InfinispanModel.VERSION_3_0_0.getVersion())
            .build();

    static void buildTransformation(ModelVersion version, ResourceTransformationDescriptionBuilder parent) {
        ResourceTransformationDescriptionBuilder builder = parent.addChildResource(WILDCARD_PATH);

        if (InfinispanModel.VERSION_3_0_0.requiresTransformation(version)) {
            OperationTransformer addAliasTransformer = new OperationTransformer() {
                @Override
                public ModelNode transformOperation(ModelNode operation) {
                    String attributeName = Operations.getAttributeName(operation);
                    if (ALIASES.getName().equals(attributeName)) {
                        ModelNode value = Operations.getAttributeValue(operation);
                        PathAddress address = Operations.getPathAddress(operation);
                        ModelNode transformedOperation = Util.createOperation(ALIAS_ADD, address);
                        transformedOperation.get(ALIAS.getName()).set(value);
                        return transformedOperation;
                    }
                    return operation;
                }
            };
            builder.addRawOperationTransformationOverride(ListOperations.LIST_ADD_DEFINITION.getName(), new SimpleOperationTransformer(addAliasTransformer));

            OperationTransformer removeAliasTransformer = new OperationTransformer() {
                @Override
                public ModelNode transformOperation(ModelNode operation) {
                    String attributeName = Operations.getAttributeName(operation);
                    if (ALIASES.getName().equals(attributeName)) {
                        ModelNode value = Operations.getAttributeValue(operation);
                        PathAddress address = Operations.getPathAddress(operation);
                        ModelNode transformedOperation = Util.createOperation(ALIAS_REMOVE, address);
                        transformedOperation.get(ALIAS.getName()).set(value);
                        return transformedOperation;
                    }
                    return operation;
                }
            };
            builder.addRawOperationTransformationOverride(ListOperations.LIST_REMOVE_DEFINITION.getName(), new SimpleOperationTransformer(removeAliasTransformer));
        }

        if (InfinispanModel.VERSION_1_5_0.requiresTransformation(version)) {
            builder.getAttributeBuilder()
                    // discard statistics if set to true, reject otherwise
                    .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(false, false, new ModelNode(true)), STATISTICS_ENABLED)
                    .addRejectCheck(RejectAttributeChecker.UNDEFINED, STATISTICS_ENABLED)
                    .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, STATISTICS_ENABLED)
                    .addRejectCheck(new RejectAttributeChecker.SimpleRejectAttributeChecker(new ModelNode(false)), STATISTICS_ENABLED);
        }

        TransportResourceDefinition.buildTransformation(version, builder);

        DistributedCacheResourceDefinition.buildTransformation(version, builder);
        ReplicatedCacheResourceDefinition.buildTransformation(version, builder);
        InvalidationCacheResourceDefinition.buildTransformation(version, builder);
        LocalCacheResourceDefinition.buildTransformation(version, builder);
    }

    private final PathManager pathManager;
    private final boolean allowRuntimeOnlyRegistration;

    CacheContainerResourceDefinition(PathManager pathManager, boolean allowRuntimeOnlyRegistration) {
        super(WILDCARD_PATH, new InfinispanResourceDescriptionResolver(ModelKeys.CACHE_CONTAINER), new CacheContainerAddHandler(), new CacheContainerRemoveHandler());
        this.pathManager = pathManager;
        this.allowRuntimeOnlyRegistration = allowRuntimeOnlyRegistration;
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration registration) {
        OperationStepHandler writeHandler = new ReloadRequiredWriteAttributeHandler(ATTRIBUTES);
        for (AttributeDefinition attr : ATTRIBUTES) {
            registration.registerReadWriteAttribute(attr, null, writeHandler);
        }

        if (this.allowRuntimeOnlyRegistration) {
            new MetricHandler<>(new CacheContainerMetricExecutor(), CacheContainerMetric.class).register(registration);
        }
    }

    @Override
    public void registerOperations(ManagementResourceRegistration registration) {
        super.registerOperations(registration);

        // Translate legacy add-alias operation to list-add operation
        OperationStepHandler addAliasHandler = new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode legacyOperation) {
                String value = legacyOperation.get(ALIAS.getName()).asString();
                ModelNode operation = Operations.createListAddOperation(context.getCurrentAddress(), ALIASES.getName(), value);
                context.addStep(operation, ListOperations.LIST_ADD_HANDLER, context.getCurrentStage());
            }
        };
        registration.registerOperationHandler(ALIAS_ADD, addAliasHandler);

        // Translate legacy remove-alias operation to list-remove operation
        OperationStepHandler removeAliasHandler = new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode legacyOperation) throws OperationFailedException {
                String value = legacyOperation.get(ALIAS.getName()).asString();
                ModelNode operation = Operations.createListRemoveOperation(context.getCurrentAddress(), ALIASES.getName(), value);
                context.addStep(operation, ListOperations.LIST_REMOVE_HANDLER, context.getCurrentStage());
            }
        };
        registration.registerOperationHandler(ALIAS_REMOVE, removeAliasHandler);
    }

    @Override
    public void registerChildren(ManagementResourceRegistration registration) {
        // child resources
        registration.registerSubModel(new TransportResourceDefinition());
        registration.registerSubModel(new LocalCacheResourceDefinition(this.pathManager, this.allowRuntimeOnlyRegistration));
        registration.registerSubModel(new InvalidationCacheResourceDefinition(this.pathManager, this.allowRuntimeOnlyRegistration));
        registration.registerSubModel(new ReplicatedCacheResourceDefinition(this.pathManager, this.allowRuntimeOnlyRegistration));
        registration.registerSubModel(new DistributedCacheResourceDefinition(this.pathManager, this.allowRuntimeOnlyRegistration));
    }
}
