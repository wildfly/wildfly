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

import javax.transaction.TransactionSynchronizationRegistry;

import org.infinispan.transaction.LockingMode;
import org.jboss.as.clustering.controller.AttributeTranslation;
import org.jboss.as.clustering.controller.BinaryCapabilityNameResolver;
import org.jboss.as.clustering.controller.BinaryRequirementCapability;
import org.jboss.as.clustering.controller.Capability;
import org.jboss.as.clustering.controller.CommonRequirement;
import org.jboss.as.clustering.controller.ManagementResourceRegistration;
import org.jboss.as.clustering.controller.ReadAttributeTranslationHandler;
import org.jboss.as.clustering.controller.Registration;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.controller.SimpleAliasEntry;
import org.jboss.as.clustering.controller.SimpleResourceRegistration;
import org.jboss.as.clustering.controller.SimpleResourceServiceHandler;
import org.jboss.as.clustering.controller.validation.EnumValidator;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.tm.XAResourceRecoveryRegistry;
import org.wildfly.clustering.infinispan.spi.InfinispanCacheRequirement;
import org.wildfly.clustering.service.Requirement;

/**
 * Resource description for the addressable resource and its alias
 *
 * /subsystem=infinispan/cache-container=X/cache=Y/component=transaction
 * /subsystem=infinispan/cache-container=X/cache=Y/transaction=TRANSACTION
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class TransactionResourceDefinition extends ComponentResourceDefinition {

    static final PathElement PATH = pathElement("transaction");
    static final PathElement LEGACY_PATH = PathElement.pathElement(PATH.getValue(), "TRANSACTION");

    enum Attribute implements org.jboss.as.clustering.controller.Attribute, UnaryOperator<SimpleAttributeDefinitionBuilder> {
        LOCKING("locking", ModelType.STRING, new ModelNode(LockingMode.PESSIMISTIC.name())) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setValidator(new EnumValidator<>(LockingMode.class));
            }
        },
        MODE("mode", ModelType.STRING, new ModelNode(TransactionMode.NONE.name())) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setValidator(new EnumValidator<>(TransactionMode.class));
            }
        },
        STOP_TIMEOUT("stop-timeout", ModelType.LONG, new ModelNode(TimeUnit.SECONDS.toMillis(10))) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setMeasurementUnit(MeasurementUnit.MILLISECONDS);
            }
        },
        COMPLETE_TIMEOUT("complete-timeout", ModelType.LONG, new ModelNode(TimeUnit.SECONDS.toMillis(60))) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setMeasurementUnit(MeasurementUnit.MILLISECONDS);
            }
        }
        ;
        private final SimpleAttributeDefinition definition;

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
    }

    enum TransactionRequirement implements Requirement {
        TRANSACTION_SYNCHRONIZATION_REGISTRY("org.wildfly.transactions.transaction-synchronization-registry", TransactionSynchronizationRegistry.class),
        XA_RESOURCE_RECOVERY_REGISTRY("org.wildfly.transactions.xa-resource-recovery-registry", XAResourceRecoveryRegistry.class);

        private final String name;
        private final Class<?> type;

        TransactionRequirement(String name, Class<?> type) {
            this.name = name;
            this.type = type;
        }

        @Override
        public String getName() {
            return this.name;
        }

        @Override
        public Class<?> getType() {
            return this.type;
        }
    }

    enum DeprecatedMetric implements AttributeTranslation, UnaryOperator<PathAddress>, Registration<ManagementResourceRegistration> {
        COMMITS(TransactionMetric.COMMITS),
        PREPARES(TransactionMetric.PREPARES),
        ROLLBACKS(TransactionMetric.ROLLBACKS),
        ;
        private final AttributeDefinition definition;
        private final org.jboss.as.clustering.controller.Attribute targetAttribute;

        DeprecatedMetric(TransactionMetric metric) {
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
            return cacheAddress.getParent().append(CacheRuntimeResourceDefinition.pathElement(cacheAddress.getLastElement().getValue()), TransactionRuntimeResourceDefinition.PATH);
        }
    }

    static void buildTransformation(ModelVersion version, ResourceTransformationDescriptionBuilder parent) {
        ResourceTransformationDescriptionBuilder builder = parent.addChildResource(PATH);

        if (InfinispanModel.VERSION_15_0_0.requiresTransformation(version)) {
            builder.getAttributeBuilder()
                   .setDiscard(DiscardAttributeChecker.DEFAULT_VALUE, Attribute.COMPLETE_TIMEOUT.getName())
                   .addRejectCheck(RejectAttributeChecker.DEFINED, Attribute.COMPLETE_TIMEOUT.getDefinition());
        }
    }

    static PathAddress cacheAddress(PathAddress transactionAddress) {
        return transactionAddress.subAddress(0, transactionAddress.size() - 1);
    }

    TransactionResourceDefinition() {
        super(PATH);
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent) {
        ManagementResourceRegistration registration = parent.registerSubModel(this);
        parent.registerAlias(LEGACY_PATH, new SimpleAliasEntry(registration));

        Capability dependentCapability = new BinaryRequirementCapability(InfinispanCacheRequirement.CACHE, BinaryCapabilityNameResolver.GRANDPARENT_PARENT);
        ResourceDescriptor descriptor = new ResourceDescriptor(this.getResourceDescriptionResolver())
                .addAttributes(Attribute.class)
                // Add a requirement on the tm capability to the parent cache capability
                .addResourceCapabilityReference(new TransactionResourceCapabilityReference(dependentCapability, CommonRequirement.LOCAL_TRANSACTION_PROVIDER, Attribute.MODE, EnumSet.of(TransactionMode.NONE, TransactionMode.BATCH)))
                // Add a requirement on the XAResourceRecoveryRegistry capability to the parent cache capability
                .addResourceCapabilityReference(new TransactionResourceCapabilityReference(dependentCapability, TransactionRequirement.TRANSACTION_SYNCHRONIZATION_REGISTRY, Attribute.MODE, EnumSet.complementOf(EnumSet.of(TransactionMode.NON_XA))))
                // Add a requirement on the XAResourceRecoveryRegistry capability to the parent cache capability
                .addResourceCapabilityReference(new TransactionResourceCapabilityReference(dependentCapability, TransactionRequirement.XA_RESOURCE_RECOVERY_REGISTRY, Attribute.MODE, EnumSet.complementOf(EnumSet.of(TransactionMode.FULL_XA))));
        ResourceServiceHandler handler = new SimpleResourceServiceHandler(TransactionServiceConfigurator::new);
        new SimpleResourceRegistration(descriptor, handler).register(registration);

        if (registration.isRuntimeOnlyRegistrationValid()) {
            for (DeprecatedMetric metric : EnumSet.allOf(DeprecatedMetric.class)) {
                metric.register(registration);
            }
        }

        return registration;
    }
}
