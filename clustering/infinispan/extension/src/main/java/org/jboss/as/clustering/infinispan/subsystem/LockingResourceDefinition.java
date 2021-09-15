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

import java.util.EnumSet;
import java.util.concurrent.TimeUnit;
import java.util.function.UnaryOperator;

import org.infinispan.util.concurrent.IsolationLevel;
import org.jboss.as.clustering.controller.AttributeTranslation;
import org.jboss.as.clustering.controller.ManagementResourceRegistration;
import org.jboss.as.clustering.controller.ReadAttributeTranslationHandler;
import org.jboss.as.clustering.controller.Registration;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.SimpleResourceRegistration;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.controller.SimpleAliasEntry;
import org.jboss.as.clustering.controller.SimpleResourceServiceHandler;
import org.jboss.as.clustering.controller.validation.EnumValidator;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Resource description for the addressable resource /subsystem=infinispan/cache-container=X/cache=Y/locking=LOCKING
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class LockingResourceDefinition extends ComponentResourceDefinition {

    static final PathElement PATH = pathElement("locking");
    static final PathElement LEGACY_PATH = PathElement.pathElement(PATH.getValue(), "LOCKING");

    enum Attribute implements org.jboss.as.clustering.controller.Attribute, UnaryOperator<SimpleAttributeDefinitionBuilder> {
        ACQUIRE_TIMEOUT("acquire-timeout", ModelType.LONG, new ModelNode(TimeUnit.SECONDS.toMillis(15))) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setMeasurementUnit(MeasurementUnit.MILLISECONDS);
            }
        },
        CONCURRENCY("concurrency-level", ModelType.INT, new ModelNode(1000)),
        ISOLATION("isolation", ModelType.STRING, new ModelNode(IsolationLevel.READ_COMMITTED.name())) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setValidator(new EnumValidator<>(IsolationLevel.class));
            }
        },
        STRIPING("striping", ModelType.BOOLEAN, ModelNode.FALSE),
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelType type, ModelNode defaultValue) {
            this.definition = this.apply(new SimpleAttributeDefinitionBuilder(name, type)
                    .setAllowExpression(true)
                    .setRequired(false)
                    .setDefaultValue(defaultValue)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    ).build();
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

    enum DeprecatedMetric implements AttributeTranslation, UnaryOperator<PathAddress>, Registration<ManagementResourceRegistration> {
        CURRENT_CONCURRENCY_LEVEL(LockingMetric.CURRENT_CONCURRENCY_LEVEL),
        NUMBER_OF_LOCKS_AVAILABLE(LockingMetric.NUMBER_OF_LOCKS_AVAILABLE),
        NUMBER_OF_LOCKS_HELD(LockingMetric.NUMBER_OF_LOCKS_HELD),
        ;
        private final AttributeDefinition definition;
        private final org.jboss.as.clustering.controller.Attribute targetAttribute;

        DeprecatedMetric(LockingMetric metric) {
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
            return cacheAddress.getParent().append(CacheRuntimeResourceDefinition.pathElement(cacheAddress.getLastElement().getValue()), LockingRuntimeResourceDefinition.PATH);
        }
    }

    LockingResourceDefinition() {
        super(PATH);
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent) {
        ManagementResourceRegistration registration = parent.registerSubModel(this);
        parent.registerAlias(LEGACY_PATH, new SimpleAliasEntry(registration));

        ResourceDescriptor descriptor = new ResourceDescriptor(this.getResourceDescriptionResolver()).addAttributes(Attribute.class);
        ResourceServiceHandler handler = new SimpleResourceServiceHandler(LockingServiceConfigurator::new);
        new SimpleResourceRegistration(descriptor, handler).register(registration);

        if (registration.isRuntimeOnlyRegistrationValid()) {
            for (DeprecatedMetric metric : EnumSet.allOf(DeprecatedMetric.class)) {
                metric.register(registration);
            }
        }

        return registration;
    }
}
