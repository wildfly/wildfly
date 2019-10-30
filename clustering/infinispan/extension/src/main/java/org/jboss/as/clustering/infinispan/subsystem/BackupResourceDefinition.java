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

import java.util.function.UnaryOperator;

import org.infinispan.configuration.cache.BackupConfiguration.BackupStrategy;
import org.infinispan.configuration.cache.BackupFailurePolicy;
import org.jboss.as.clustering.controller.ChildResourceDefinition;
import org.jboss.as.clustering.controller.ManagementResourceRegistration;
import org.jboss.as.clustering.controller.OperationHandler;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceConfiguratorFactory;
import org.jboss.as.clustering.controller.RestartParentResourceRegistration;
import org.jboss.as.clustering.controller.validation.EnumValidator;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Definition of a backup site resource.
 *
 * @author Paul Ferraro
 */
public class BackupResourceDefinition extends ChildResourceDefinition<ManagementResourceRegistration> {

    static final PathElement WILDCARD_PATH = pathElement(PathElement.WILDCARD_VALUE);

    static PathElement pathElement(String name) {
        return PathElement.pathElement("backup", name);
    }

    enum Attribute implements org.jboss.as.clustering.controller.Attribute, UnaryOperator<SimpleAttributeDefinitionBuilder> {
        ENABLED("enabled", ModelType.BOOLEAN, new ModelNode(true)),
        FAILURE_POLICY("failure-policy", ModelType.STRING, new ModelNode(BackupFailurePolicy.WARN.name())) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setValidator(new EnumValidator<>(BackupFailurePolicy.class));
            }
        },
        STRATEGY("strategy", ModelType.STRING, new ModelNode(BackupStrategy.ASYNC.name())) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setValidator(new EnumValidator<>(BackupStrategy.class));
            }
        },
        TIMEOUT("timeout", ModelType.LONG, new ModelNode(10000L)),
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelType type, ModelNode defaultValue) {
            this.definition = this.apply(createBuilder(name, type, defaultValue)).build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }

        @Override
        public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
            return builder;
        }
    }

    enum TakeOfflineAttribute implements org.jboss.as.clustering.controller.Attribute {
        AFTER_FAILURES("after-failures", ModelType.INT, new ModelNode(1)),
        MIN_WAIT("min-wait", ModelType.LONG, new ModelNode(0L)),
        ;
        private final AttributeDefinition definition;

        TakeOfflineAttribute(String name, ModelType type, ModelNode defaultValue) {
            this.definition = createBuilder(name, type, defaultValue).build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    static SimpleAttributeDefinitionBuilder createBuilder(String name, ModelType type, ModelNode defaultValue) {
        return new SimpleAttributeDefinitionBuilder(name, type)
                .setAllowExpression(true)
                .setRequired(false)
                .setDefaultValue(defaultValue)
                .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                .setMeasurementUnit((type == ModelType.LONG) ? MeasurementUnit.MILLISECONDS : null)
                ;
    }

    static void buildTransformation(ModelVersion version, ResourceTransformationDescriptionBuilder parent) {
        // Nothing to transform
    }

    private final ResourceServiceConfiguratorFactory parentServiceConfiguratorFactory;

    BackupResourceDefinition(ResourceServiceConfiguratorFactory parentServiceConfiguratorFactory) {
        super(WILDCARD_PATH, InfinispanExtension.SUBSYSTEM_RESOLVER.createChildResolver(WILDCARD_PATH));
        this.parentServiceConfiguratorFactory = parentServiceConfiguratorFactory;
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent) {
        ManagementResourceRegistration registration = parent.registerSubModel(this);

        ResourceDescriptor descriptor = new ResourceDescriptor(this.getResourceDescriptionResolver())
                .addAttributes(Attribute.class)
                .addAttributes(TakeOfflineAttribute.class)
                ;
        new RestartParentResourceRegistration(this.parentServiceConfiguratorFactory, descriptor).register(registration);

        if (registration.isRuntimeOnlyRegistrationValid()) {
            new OperationHandler<>(new BackupOperationExecutor(), BackupOperation.class).register(registration);
        }

        return registration;
    }
}
