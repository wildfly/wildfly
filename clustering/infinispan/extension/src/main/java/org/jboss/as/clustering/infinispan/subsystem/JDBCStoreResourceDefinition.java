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

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import org.infinispan.configuration.cache.PersistenceConfiguration;
import org.infinispan.persistence.jdbc.DatabaseType;
import org.jboss.as.clustering.controller.AttributeValueTranslator;
import org.jboss.as.clustering.controller.CapabilityReference;
import org.jboss.as.clustering.controller.CommonUnaryRequirement;
import org.jboss.as.clustering.controller.ManagementResourceRegistration;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceBuilderFactory;
import org.jboss.as.clustering.controller.transform.SimpleAttributeConverter;
import org.jboss.as.clustering.controller.transform.SimpleAttributeConverter.Converter;
import org.jboss.as.clustering.controller.validation.EnumValidator;
import org.jboss.as.clustering.infinispan.InfinispanLogger;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;

/**
 * Base class for store resources which require common store attributes and JDBC store attributes
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public abstract class JDBCStoreResourceDefinition extends StoreResourceDefinition {

    static final PathElement PATH = pathElement("jdbc");

    enum Attribute implements org.jboss.as.clustering.controller.Attribute {
        DATA_SOURCE("data-source", ModelType.STRING, builder -> builder.setRequired(true).setCapabilityReference(new CapabilityReference(Capability.PERSISTENCE, CommonUnaryRequirement.DATA_SOURCE)).setAlternatives(DeprecatedAttribute.DATASOURCE.getName())),
        DIALECT("dialect", ModelType.STRING, builder -> builder.setAllowExpression(true).setRequired(false).setValidator(new EnumValidator<>(DatabaseType.class))),
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelType type, UnaryOperator<SimpleAttributeDefinitionBuilder> configurator) {
            this.definition = configurator.apply(new SimpleAttributeDefinitionBuilder(name, type).setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)).build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    enum DeprecatedAttribute implements org.jboss.as.clustering.controller.Attribute {
        DATASOURCE("datasource", ModelType.STRING, builder -> builder.setAllowExpression(true).setRequired(false), InfinispanModel.VERSION_4_0_0), // Defines data source as JNDI name
        ;
        private final AttributeDefinition definition;

        DeprecatedAttribute(String name, ModelType type, UnaryOperator<SimpleAttributeDefinitionBuilder> configurator, InfinispanModel deprecation) {
            this.definition = configurator.apply(new SimpleAttributeDefinitionBuilder(name, type).setDeprecated(deprecation.getVersion()).setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)).build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    static void buildTransformation(ModelVersion version, ResourceTransformationDescriptionBuilder builder, PathElement path) {

        if (InfinispanModel.VERSION_5_0_0.requiresTransformation(version)) {
            builder.getAttributeBuilder()
                    // WFLY-8985 value MARIA_DB and any expression which could potentially resolve to that value on a slave node must be rejected
                    .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, Attribute.DIALECT.getDefinition())
                    .addRejectCheck(new RejectAttributeChecker.SimpleRejectAttributeChecker(new ModelNode(DatabaseType.MARIA_DB.name())), Attribute.DIALECT.getDefinition());
        }

        if (InfinispanModel.VERSION_5_0_0.requiresTransformation(version) && !InfinispanModel.VERSION_4_0_0.requiresTransformation(version)) {
            // DATASOURCE attribute was only supported as an add operation parameter
            builder.getAttributeBuilder().setDiscard(DiscardAttributeChecker.ALWAYS, DeprecatedAttribute.DATASOURCE.getDefinition());
        }

        if (InfinispanModel.VERSION_4_0_0.requiresTransformation(version)) {
            // Converts pool name to its JNDI name
            Converter converter = new Converter() {
                @Override
                public void convert(PathAddress address, String name, ModelNode value, ModelNode model, TransformationContext context) {
                    if (value.isDefined()) {
                        PathAddress rootAddress = address.subAddress(0, address.size() - 4);
                        PathAddress subsystemAddress = rootAddress.append(PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, "datasources"));
                        Resource subsystem = context.readResourceFromRoot(subsystemAddress);
                        String poolName = value.asString();
                        for (String type : Arrays.asList("data-source", "xa-data-source")) {
                            if (subsystem.hasChildren(type)) {
                                for (Resource.ResourceEntry entry : subsystem.getChildren(type)) {
                                    if (entry.getName().equals(poolName)) {
                                        value.set(entry.getModel().get("jndi-name"));
                                        return;
                                    }
                                }
                            }
                        }
                    }
                }
            };
            builder.getAttributeBuilder()
                    .addRename(Attribute.DATA_SOURCE.getName(), DeprecatedAttribute.DATASOURCE.getName())
                    .setValueConverter(new SimpleAttributeConverter(converter), Attribute.DATA_SOURCE.getDefinition())
            ;
        }

        StoreResourceDefinition.buildTransformation(version, builder, path);

        if (InfinispanModel.VERSION_2_0_0.requiresTransformation(version)) {
            builder.getAttributeBuilder()
                    .setDiscard(DiscardAttributeChecker.UNDEFINED, Attribute.DIALECT.getDefinition())
                    .addRejectCheck(RejectAttributeChecker.DEFINED, Attribute.DIALECT.getDefinition())
                    .end();
        }
    }

    static class TableAttributeTransformation implements UnaryOperator<OperationStepHandler> {
        private final org.jboss.as.clustering.controller.Attribute attribute;
        private final PathElement path;

        TableAttributeTransformation(org.jboss.as.clustering.controller.Attribute attribute, PathElement path) {
            this.attribute = attribute;
            this.path = path;
        }

        @Override
        public OperationStepHandler apply(OperationStepHandler handler) {
            return (context, operation) -> {
                if (operation.hasDefined(this.attribute.getName())) {
                    // Translate deprecated table attribute into separate add table operation
                    ModelNode addTableOperation = Util.createAddOperation(context.getCurrentAddress().append(this.path));
                    ModelNode parameters = operation.get(this.attribute.getName());
                    for (Property parameter : parameters.asPropertyList()) {
                        addTableOperation.get(parameter.getName()).set(parameter.getValue());
                    }
                    context.addStep(addTableOperation, context.getResourceRegistration().getOperationHandler(PathAddress.pathAddress(this.path), ModelDescriptionConstants.ADD), context.getCurrentStage());
                }
                handler.execute(context, operation);
            };
        }

        public UnaryOperator<OperationStepHandler> andThen(UnaryOperator<OperationStepHandler> after) {
            return handler -> after.apply(this.apply(handler));
        }
    }

    static final AttributeValueTranslator POOL_NAME_TO_JNDI_NAME_TRANSLATOR = new AttributeValueTranslator() {
        @Override
        public ModelNode translate(OperationContext context, ModelNode value) throws OperationFailedException {
            String poolName = value.asString();
            PathAddress address = context.getCurrentAddress();
            PathAddress rootAddress = address.subAddress(0, address.size() - 4);
            PathAddress subsystemAddress = rootAddress.append(PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, "datasources"));
            Resource subsystem = context.readResourceFromRoot(subsystemAddress);
            for (String type : Arrays.asList("data-source", "xa-data-source")) {
                Resource resource = subsystem.getChild(PathElement.pathElement(type, poolName));
                if (resource != null) {
                    return resource.getModel().get("jndi-name");
                }
            }
            throw InfinispanLogger.ROOT_LOGGER.dataSourceNotFound(poolName);
        }
    };

    static final AttributeValueTranslator JNDI_NAME_TO_POOL_NAME_TRANSLATOR = new AttributeValueTranslator() {
        @Override
        public ModelNode translate(OperationContext context, ModelNode value) throws OperationFailedException {
            String jndiName = value.asString();
            PathAddress address = context.getCurrentAddress();
            PathAddress rootAddress = address.subAddress(0, address.size() - 4);
            PathAddress subsystemAddress = rootAddress.append(PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, "datasources"));
            Resource subsystem = context.readResourceFromRoot(subsystemAddress);
            for (String type : Arrays.asList("data-source", "xa-data-source")) {
                if (subsystem.hasChildren(type)) {
                    for (Resource.ResourceEntry entry : subsystem.getChildren(type)) {
                        ModelNode model = entry.getModel();
                        if (model.get("jndi-name").asString().equals(jndiName)) {
                            return new ModelNode(entry.getName());
                        }
                    }
                }
            }
            throw InfinispanLogger.ROOT_LOGGER.dataSourceJndiNameNotFound(jndiName);
        }
    };

    JDBCStoreResourceDefinition(PathElement path, PathElement legacyPath, InfinispanResourceDescriptionResolver resolver, Consumer<ResourceDescriptor> configurator, ResourceServiceBuilderFactory<PersistenceConfiguration> builderFactory, Consumer<ManagementResourceRegistration> registrationConfigurator) {
        super(path, legacyPath, resolver, configurator.andThen(descriptor -> descriptor
                .addAttributes(Attribute.class)
                // Translate deprecated DATASOURCE attribute to DATA_SOURCE attribute
                .addAttributeTranslation(DeprecatedAttribute.DATASOURCE, Attribute.DATA_SOURCE, POOL_NAME_TO_JNDI_NAME_TRANSLATOR, JNDI_NAME_TO_POOL_NAME_TRANSLATOR)
            ), builderFactory, registrationConfigurator);
    }
}
