/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

import java.util.EnumSet;
import java.util.function.UnaryOperator;

import org.jboss.as.clustering.controller.AttributeTranslation;
import org.jboss.as.clustering.controller.ManagementResourceRegistration;
import org.jboss.as.clustering.controller.Operations;
import org.jboss.as.clustering.controller.ReadAttributeTranslationHandler;
import org.jboss.as.clustering.controller.Registration;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.controller.SimpleResourceRegistration;
import org.jboss.as.clustering.controller.SimpleResourceServiceHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.logging.ControllerLogger;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Resource definition of the partition handling component of a cache.
 * @author Paul Ferraro
 */
public class PartitionHandlingResourceDefinition extends ComponentResourceDefinition {

    static final PathElement PATH = pathElement("partition-handling");

    enum Attribute implements org.jboss.as.clustering.controller.Attribute {
        ENABLED("enabled", ModelType.BOOLEAN, ModelNode.FALSE),
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelType type, ModelNode defaultValue) {
            this.definition = new SimpleAttributeDefinitionBuilder(name, type)
                    .setAllowExpression(true)
                    .setRequired(false)
                    .setDefaultValue(defaultValue)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    enum DeprecatedMetric implements AttributeTranslation, UnaryOperator<PathAddress>, Registration<ManagementResourceRegistration> {
        AVAILABILITY(PartitionHandlingMetric.AVAILABILITY),
        ;
        private final AttributeDefinition definition;
        private final org.jboss.as.clustering.controller.Attribute targetAttribute;

        DeprecatedMetric(PartitionHandlingMetric metric) {
            this.targetAttribute = metric;
            this.definition = new SimpleAttributeDefinitionBuilder(metric.getName(), metric.getDefinition().getType())
                    .setDeprecated(InfinispanModel.VERSION_11_0_0.getVersion())
                    .setStorageRuntime()
                    .build();
        }

        @Override
        public void register(ManagementResourceRegistration registration) {
            registration.registerReadOnlyAttribute(this.definition, new ReadAttributeTranslationHandler(this));
        }

        @Override
        public org.jboss.as.clustering.controller.Attribute getTargetAttribute() {
            return this.targetAttribute;
        }

        @Override
        public UnaryOperator<PathAddress> getPathAddressTransformation() {
            return this;
        }

        @Override
        public PathAddress apply(PathAddress address) {
            PathAddress cacheAddress = address.getParent();
            return cacheAddress.getParent().append(CacheRuntimeResourceDefinition.pathElement(cacheAddress.getLastElement().getValue()), PartitionHandlingRuntimeResourceDefinition.PATH);
        }
    }

    enum DeprecatedOperation implements OperationStepHandler, Registration<ManagementResourceRegistration> {
        FORCE_AVAILABLE(PartitionHandlingOperation.FORCE_AVAILABLE),
        ;
        private final OperationDefinition definition;

        DeprecatedOperation(PartitionHandlingOperation operation) {
            this.definition = new SimpleOperationDefinitionBuilder(operation.getName(), InfinispanExtension.SUBSYSTEM_RESOLVER.createChildResolver(PartitionHandlingResourceDefinition.PATH))
                    .setDeprecated(InfinispanModel.VERSION_11_0_0.getVersion())
                    .setRuntimeOnly()
                    .build();
        }

        @Override
        public void register(ManagementResourceRegistration registration) {
            registration.registerOperationHandler(this.definition, this);
        }

        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            PathAddress currentAddress = context.getCurrentAddress();
            PathAddress targetAddress = DeprecatedMetric.AVAILABILITY.getPathAddressTransformation().apply(context.getCurrentAddress());
            Operations.setPathAddress(operation, targetAddress);
            ImmutableManagementResourceRegistration registration = (currentAddress == targetAddress) ? context.getResourceRegistration() : context.getRootResourceRegistration().getSubModel(targetAddress);
            if (registration == null) {
                throw new OperationFailedException(ControllerLogger.MGMT_OP_LOGGER.noSuchResourceType(targetAddress));
            }
            OperationStepHandler handler = registration.getOperationHandler(PathAddress.EMPTY_ADDRESS, this.definition.getName());
            context.addStep(operation, handler, context.getCurrentStage());
        }
    }

    PartitionHandlingResourceDefinition() {
        super(PATH);
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent) {
        ManagementResourceRegistration registration = parent.registerSubModel(this);

        ResourceDescriptor descriptor = new ResourceDescriptor(this.getResourceDescriptionResolver()).addAttributes(Attribute.class);
        ResourceServiceHandler handler = new SimpleResourceServiceHandler(PartitionHandlingServiceConfigurator::new);
        new SimpleResourceRegistration(descriptor, handler).register(registration);

        if (registration.isRuntimeOnlyRegistrationValid()) {
            for (DeprecatedOperation operation : EnumSet.allOf(DeprecatedOperation.class)) {
                operation.register(registration);
            }
            for (DeprecatedMetric metric : EnumSet.allOf(DeprecatedMetric.class)) {
                metric.register(registration);
            }
        }

        return registration;
    }
}
