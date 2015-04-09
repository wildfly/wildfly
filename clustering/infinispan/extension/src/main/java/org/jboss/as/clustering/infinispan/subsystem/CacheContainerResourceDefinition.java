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

import org.jboss.as.clustering.controller.AddStepHandler;
import org.jboss.as.clustering.controller.AttributeParsers;
import org.jboss.as.clustering.controller.MetricHandler;
import org.jboss.as.clustering.controller.Operations;
import org.jboss.as.clustering.controller.Registration;
import org.jboss.as.clustering.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.clustering.controller.RemoveStepHandler;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
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
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.operations.global.ListOperations;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.operations.validation.ParameterValidator;
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
public class CacheContainerResourceDefinition extends SimpleResourceDefinition implements Registration {

    static final PathElement WILDCARD_PATH = pathElement(PathElement.WILDCARD_VALUE);

    static PathElement pathElement(String containerName) {
        return PathElement.pathElement("cache-container", containerName);
    }

    static final AttributeDefinition ALIAS = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.NAME, ModelType.STRING)
            .setAllowExpression(false)
            .build();

    static final OperationDefinition ALIAS_ADD = new SimpleOperationDefinitionBuilder("add-alias", new InfinispanResourceDescriptionResolver(WILDCARD_PATH))
            .setParameters(ALIAS)
            .setDeprecated(InfinispanModel.VERSION_3_0_0.getVersion())
            .build();

    static final OperationDefinition ALIAS_REMOVE = new SimpleOperationDefinitionBuilder("remove-alias", new InfinispanResourceDescriptionResolver(WILDCARD_PATH))
            .setParameters(ALIAS)
            .setDeprecated(InfinispanModel.VERSION_3_0_0.getVersion())
            .build();

    enum Attribute implements org.jboss.as.clustering.controller.Attribute {
        ALIASES("aliases"),
        MODULE("module", ModelType.STRING, new ModelNode("org.jboss.as.clustering.infinispan"), new ModuleIdentifierValidator(true, true)),
        DEFAULT_CACHE("default-cache", ModelType.STRING, null, null),
        @Deprecated EVICTION_EXECUTOR("eviction-executor", ModelType.STRING, null, null, InfinispanModel.VERSION_3_0_0),
        JNDI_NAME("jndi-name", ModelType.STRING, null, null),
        @Deprecated LISTENER_EXECUTOR("listener-executor", ModelType.STRING, null, null, InfinispanModel.VERSION_3_0_0),
        @Deprecated REPLICATION_QUEUE_EXECUTOR("replication-queue-executor", ModelType.STRING, null, null, InfinispanModel.VERSION_3_0_0),
        @Deprecated START("start", ModelType.STRING, new ModelNode(StartMode.LAZY.name()), new EnumValidator<>(StartMode.class, true, true), InfinispanModel.VERSION_3_0_0),
        STATISTICS_ENABLED("statistics-enabled", ModelType.BOOLEAN, new ModelNode(false), null),
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelType type, ModelNode defaultValue, ParameterValidator validator) {
            this.definition = createBuilder(name, type, defaultValue, validator).build();
        }

        Attribute(String name, ModelType type, ModelNode defaultValue, ParameterValidator validator, InfinispanModel deprecation) {
            this.definition = createBuilder(name, type, defaultValue, validator).setDeprecated(deprecation.getVersion()).build();
        }

        private static SimpleAttributeDefinitionBuilder createBuilder(String name, ModelType type, ModelNode defaultValue, ParameterValidator validator) {
            return new SimpleAttributeDefinitionBuilder(name, type)
                    .setAllowExpression(true)
                    .setAllowNull(true)
                    .setDefaultValue(defaultValue)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .setValidator(validator)
            ;
        }

        Attribute(String name) {
            this.definition = new StringListAttributeDefinition.Builder(name)
                    .setAllowNull(true)
                    .setAttributeParser(AttributeParsers.COLLECTION)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    static void buildTransformation(ModelVersion version, ResourceTransformationDescriptionBuilder parent) {
        ResourceTransformationDescriptionBuilder builder = parent.addChildResource(WILDCARD_PATH);

        if (InfinispanModel.VERSION_4_0_0.requiresTransformation(version)) {
            builder.discardChildResource(NoTransportResourceDefinition.PATH);
        } else {
            NoTransportResourceDefinition.buildTransformation(version, builder);
        }

        if (InfinispanModel.VERSION_3_0_0.requiresTransformation(version)) {
            OperationTransformer addAliasTransformer = new OperationTransformer() {
                @Override
                public ModelNode transformOperation(ModelNode operation) {
                    String attributeName = Operations.getAttributeName(operation);
                    if (Attribute.ALIASES.getDefinition().getName().equals(attributeName)) {
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
                    if (Attribute.ALIASES.getDefinition().getName().equals(attributeName)) {
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
                    .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(false, false, new ModelNode(true)), Attribute.STATISTICS_ENABLED.getDefinition())
                    .addRejectCheck(RejectAttributeChecker.UNDEFINED, Attribute.STATISTICS_ENABLED.getDefinition())
                    .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, Attribute.STATISTICS_ENABLED.getDefinition())
                    .addRejectCheck(new RejectAttributeChecker.SimpleRejectAttributeChecker(new ModelNode(false)), Attribute.STATISTICS_ENABLED.getDefinition());
        }

        JGroupsTransportResourceDefinition.buildTransformation(version, builder);

        DistributedCacheResourceDefinition.buildTransformation(version, builder);
        ReplicatedCacheResourceDefinition.buildTransformation(version, builder);
        InvalidationCacheResourceDefinition.buildTransformation(version, builder);
        LocalCacheResourceDefinition.buildTransformation(version, builder);
    }

    private final PathManager pathManager;
    private final boolean allowRuntimeOnlyRegistration;

    CacheContainerResourceDefinition(PathManager pathManager, boolean allowRuntimeOnlyRegistration) {
        super(WILDCARD_PATH, new InfinispanResourceDescriptionResolver(WILDCARD_PATH));
        this.pathManager = pathManager;
        this.allowRuntimeOnlyRegistration = allowRuntimeOnlyRegistration;
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration registration) {
        new ReloadRequiredWriteAttributeHandler(Attribute.class).register(registration);

        if (this.allowRuntimeOnlyRegistration) {
            new MetricHandler<>(new CacheContainerMetricExecutor(), CacheContainerMetric.class).register(registration);
        }
    }

    @Override
    public void registerOperations(ManagementResourceRegistration registration) {
        ResourceServiceHandler handler = new CacheContainerServiceHandler();
        new AddStepHandler(this.getResourceDescriptionResolver(), handler).addAttributes(Attribute.class).register(registration);
        new RemoveStepHandler(this.getResourceDescriptionResolver(), handler).register(registration);

        // Translate legacy add-alias operation to list-add operation
        OperationStepHandler addAliasHandler = new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode legacyOperation) {
                String value = legacyOperation.get(ALIAS.getName()).asString();
                ModelNode operation = Operations.createListAddOperation(context.getCurrentAddress(), Attribute.ALIASES, value);
                context.addStep(operation, ListOperations.LIST_ADD_HANDLER, context.getCurrentStage());
            }
        };
        registration.registerOperationHandler(ALIAS_ADD, addAliasHandler);

        // Translate legacy remove-alias operation to list-remove operation
        OperationStepHandler removeAliasHandler = new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode legacyOperation) throws OperationFailedException {
                String value = legacyOperation.get(ALIAS.getName()).asString();
                ModelNode operation = Operations.createListRemoveOperation(context.getCurrentAddress(), Attribute.ALIASES, value);
                context.addStep(operation, ListOperations.LIST_REMOVE_HANDLER, context.getCurrentStage());
            }
        };
        registration.registerOperationHandler(ALIAS_REMOVE, removeAliasHandler);
    }

    @Override
    public void registerChildren(ManagementResourceRegistration registration) {
        new JGroupsTransportResourceDefinition().register(registration);
        new NoTransportResourceDefinition().register(registration);

        new LocalCacheResourceDefinition(this.pathManager, this.allowRuntimeOnlyRegistration).register(registration);
        new InvalidationCacheResourceDefinition(this.pathManager, this.allowRuntimeOnlyRegistration).register(registration);
        new ReplicatedCacheResourceDefinition(this.pathManager, this.allowRuntimeOnlyRegistration).register(registration);
        new DistributedCacheResourceDefinition(this.pathManager, this.allowRuntimeOnlyRegistration).register(registration);
    }

    @Override
    public void register(ManagementResourceRegistration registration) {
        registration.registerSubModel(this);
    }
}
