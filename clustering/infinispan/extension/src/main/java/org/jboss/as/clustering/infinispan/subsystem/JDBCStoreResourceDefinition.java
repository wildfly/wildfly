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

import org.infinispan.persistence.jdbc.DatabaseType;
import org.jboss.as.clustering.controller.CapabilityReference;
import org.jboss.as.clustering.controller.CommonUnaryRequirement;
import org.jboss.as.clustering.controller.transform.LegacyPropertyAddOperationTransformer;
import org.jboss.as.clustering.controller.transform.SimpleAttributeConverter;
import org.jboss.as.clustering.controller.transform.SimpleAttributeConverter.Converter;
import org.jboss.as.clustering.controller.transform.SimpleOperationTransformer;
import org.jboss.as.clustering.controller.validation.EnumValidatorBuilder;
import org.jboss.as.clustering.controller.validation.ParameterValidatorBuilder;
import org.jboss.as.clustering.infinispan.InfinispanLogger;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.CapabilityReferenceRecorder;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.logging.ControllerLogger;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Base class for store resources which require common store attributes and JDBC store attributes
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public abstract class JDBCStoreResourceDefinition extends StoreResourceDefinition {

    enum Capability implements org.jboss.as.clustering.controller.Capability {
        DATA_SOURCE("org.wildfly.clustering.infinispan.cache-container.cache.store.jdbc.data-source"),
        ;
        private final RuntimeCapability<Void> definition;

        Capability(String name) {
            this.definition = RuntimeCapability.Builder.of(name, true).build();
        }

        @Override
        public RuntimeCapability<Void> getDefinition() {
            return this.definition;
        }

        @Override
        public RuntimeCapability<Void> resolve(PathAddress address) {
            PathAddress cacheAddress = address.getParent();
            PathAddress containerAddress = cacheAddress.getParent();
            return this.definition.fromBaseCapability(containerAddress.getLastElement().getValue() + "." + cacheAddress.getLastElement().getValue());
        }
    }

    enum Attribute implements org.jboss.as.clustering.controller.Attribute {
        DATA_SOURCE("data-source", ModelType.STRING, new CapabilityReference(Capability.DATA_SOURCE, CommonUnaryRequirement.DATA_SOURCE)),
        DIALECT("dialect", ModelType.STRING, new EnumValidatorBuilder<>(DatabaseType.class)),
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelType type, CapabilityReferenceRecorder reference) {
            this.definition = createBuilder(name, type, true).setAllowExpression(false).setCapabilityReference(reference).build();
        }

        Attribute(String name, ModelType type, ParameterValidatorBuilder validator) {
            SimpleAttributeDefinitionBuilder builder = createBuilder(name, type, true).setAllowExpression(true);
            this.definition = builder.setValidator(validator.configure(builder).build()).build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    enum DeprecatedAttribute implements org.jboss.as.clustering.controller.Attribute {
        DATASOURCE("datasource", ModelType.STRING, InfinispanModel.VERSION_4_0_0), // Defines data source as JNDI name
        ;
        private final AttributeDefinition definition;

        DeprecatedAttribute(String name, ModelType type, InfinispanModel deprecation) {
            this.definition = createBuilder(name, type, true).setAllowExpression(true).setDeprecated(deprecation.getVersion()).build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    static SimpleAttributeDefinitionBuilder createBuilder(String name, ModelType type, boolean allowNull) {
        return new SimpleAttributeDefinitionBuilder(name, type)
                .setAllowNull(allowNull)
                .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
        ;
    }

    static void buildTransformation(ModelVersion version, ResourceTransformationDescriptionBuilder builder) {

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

        if (InfinispanModel.VERSION_3_0_0.requiresTransformation(version)) {
            builder.addOperationTransformationOverride(ModelDescriptionConstants.ADD)
                    .setCustomOperationTransformer(new SimpleOperationTransformer(new LegacyPropertyAddOperationTransformer())).inheritResourceAttributeDefinitions();
        }

        if (InfinispanModel.VERSION_2_0_0.requiresTransformation(version)) {
            builder.getAttributeBuilder()
                    .setDiscard(DiscardAttributeChecker.UNDEFINED, Attribute.DIALECT.getDefinition())
                    .addRejectCheck(RejectAttributeChecker.DEFINED, Attribute.DIALECT.getDefinition())
                    .end();
        }

        StoreResourceDefinition.buildTransformation(version, builder);
    }

    static void translateAddOperation(OperationContext context, ModelNode operation) throws OperationFailedException {
        if (!operation.hasDefined(JDBCStoreResourceDefinition.Attribute.DATA_SOURCE.getName())) {
            if (operation.hasDefined(JDBCStoreResourceDefinition.DeprecatedAttribute.DATASOURCE.getName())) {
                // Translate JNDI name into pool name
                String jndiName = JDBCStoreResourceDefinition.DeprecatedAttribute.DATASOURCE.resolveModelAttribute(context, operation).asString();
                String poolName = findPoolName(context, jndiName);
                operation.get(JDBCStoreResourceDefinition.Attribute.DATA_SOURCE.getName()).set(poolName);
            } else {
                throw ControllerLogger.MGMT_OP_LOGGER.validationFailedRequiredParameterNotPresent(JDBCStoreResourceDefinition.Attribute.DATA_SOURCE.getDefinition().getName(), operation.toString());
            }
        }
    }

    private static String findPoolName(OperationContext context, String jndiName) throws OperationFailedException {
        PathAddress address = context.getCurrentAddress();
        PathAddress rootAddress = address.subAddress(0, address.size() - 4);
        PathAddress subsystemAddress = rootAddress.append(PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, "datasources"));
        Resource subsystem = context.readResourceFromRoot(subsystemAddress);
        for (String type : Arrays.asList("data-source", "xa-data-source")) {
            if (subsystem.hasChildren(type)) {
                for (Resource.ResourceEntry entry : subsystem.getChildren(type)) {
                    ModelNode model = entry.getModel();
                    if (model.get("jndi-name").asString().equals(jndiName)) {
                        return entry.getName();
                    }
                }
            }
        }
        throw InfinispanLogger.ROOT_LOGGER.dataSourceJndiNameNotFound(jndiName);
    }

    JDBCStoreResourceDefinition(PathElement path, InfinispanResourceDescriptionResolver resolver, boolean allowRuntimeOnlyRegistration) {
        super(path, resolver, allowRuntimeOnlyRegistration);
    }
}
