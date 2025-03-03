/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import jakarta.transaction.TransactionSynchronizationRegistry;

import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.TransactionConfiguration;
import org.infinispan.configuration.cache.TransactionConfigurationBuilder;
import org.infinispan.transaction.LockingMode;
import org.infinispan.transaction.tm.EmbeddedTransactionManager;
import org.jboss.as.clustering.controller.ManagementResourceRegistration;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.controller.SimpleResourceRegistrar;
import org.jboss.as.clustering.infinispan.tx.InfinispanXAResourceRecovery;
import org.jboss.as.clustering.infinispan.tx.TransactionManagerProvider;
import org.jboss.as.clustering.infinispan.tx.TransactionSynchronizationRegistryProvider;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.capability.BinaryCapabilityNameResolver;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.tm.XAResourceRecovery;
import org.jboss.tm.XAResourceRecoveryRegistry;
import org.wildfly.clustering.infinispan.service.InfinispanServiceDescriptor;
import org.wildfly.service.descriptor.BinaryServiceDescriptor;
import org.wildfly.service.descriptor.NullaryServiceDescriptor;
import org.wildfly.subsystem.resource.capability.ResourceCapabilityReference;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.ServiceInstaller;
import org.wildfly.subsystem.service.capability.CapabilityServiceInstaller;
import org.wildfly.transaction.client.ContextTransactionManager;

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

    static final BinaryServiceDescriptor<TransactionConfiguration> SERVICE_DESCRIPTOR = serviceDescriptor(PATH, TransactionConfiguration.class);
    private static final RuntimeCapability<Void> CAPABILITY = RuntimeCapability.Builder.of(SERVICE_DESCRIPTOR).setDynamicNameMapper(BinaryCapabilityNameResolver.GRANDPARENT_PARENT).build();

    enum Attribute implements org.jboss.as.clustering.controller.Attribute, UnaryOperator<SimpleAttributeDefinitionBuilder> {
        LOCKING("locking", ModelType.STRING, new ModelNode(LockingMode.PESSIMISTIC.name())) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setValidator(EnumValidator.create(LockingMode.class));
            }
        },
        MODE("mode", ModelType.STRING, new ModelNode(TransactionMode.NONE.name())) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setValidator(EnumValidator.create(TransactionMode.class));
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

    static final NullaryServiceDescriptor<Void> LOCAL_TRANSACTION_PROVIDER = NullaryServiceDescriptor.of("org.wildfly.transactions.global-default-local-provider", Void.class);
    static final NullaryServiceDescriptor<TransactionSynchronizationRegistry> TRANSACTION_SYNCHRONIZATION_REGISTRY = NullaryServiceDescriptor.of("org.wildfly.transactions.transaction-synchronization-registry", TransactionSynchronizationRegistry.class);
    static final NullaryServiceDescriptor<XAResourceRecoveryRegistry> XA_RESOURCE_RECOVERY_REGISTRY = NullaryServiceDescriptor.of("org.wildfly.transactions.xa-resource-recovery-registry", XAResourceRecoveryRegistry.class);

    TransactionResourceDefinition() {
        super(PATH);
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent) {
        ManagementResourceRegistration registration = parent.registerSubModel(this);

        ResourceDescriptor descriptor = new ResourceDescriptor(this.getResourceDescriptionResolver())
                .addAttributes(Attribute.class)
                .addCapabilities(List.of(CAPABILITY))
                .addResourceCapabilityReference(ResourceCapabilityReference.builder(CAPABILITY, LOCAL_TRANSACTION_PROVIDER).when(Attribute.MODE.getDefinition(), new TransactionModeFilter(EnumSet.complementOf(EnumSet.of(TransactionMode.NONE, TransactionMode.BATCH)))).build())
                .addResourceCapabilityReference(ResourceCapabilityReference.builder(CAPABILITY, TRANSACTION_SYNCHRONIZATION_REGISTRY).when(Attribute.MODE.getDefinition(), new TransactionModeFilter(EnumSet.of(TransactionMode.NON_XA))).build())
                .addResourceCapabilityReference(ResourceCapabilityReference.builder(CAPABILITY, XA_RESOURCE_RECOVERY_REGISTRY).when(Attribute.MODE.getDefinition(), new TransactionModeFilter(EnumSet.of(TransactionMode.FULL_XA))).build())
                ;
        ResourceOperationRuntimeHandler handler = ResourceOperationRuntimeHandler.configureService(this);
        new SimpleResourceRegistrar(descriptor, ResourceServiceHandler.of(handler)).register(registration);

        return registration;
    }

    private static class TransactionModeFilter implements Predicate<ModelNode> {
        private final Set<TransactionMode> modes;

        TransactionModeFilter(Set<TransactionMode> modes) {
            this.modes = modes;
        }

        @Override
        public boolean test(ModelNode value) {
            try {
                TransactionMode mode = TransactionMode.valueOf(value.asString());
                return this.modes.contains(mode);
            } catch (IllegalArgumentException e) {
                // IAE would be due to an expression that can't be resolved right now (OperationContext.Stage.MODEL).
                // Very unlikely an expression is used and that it uses a resolution source not available in MODEL.
                // In any case we add the requirement. Downside is they are forced to configure the tx subsystem when
                // they otherwise wouldn't, but that "otherwise wouldn't" also is a less likely scenario.
                return true;
            }
        }
    }

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        TransactionMode mode = TransactionMode.valueOf(Attribute.MODE.resolveModelAttribute(context, model).asString());
        LockingMode locking = LockingMode.valueOf(Attribute.LOCKING.resolveModelAttribute(context, model).asString());
        long stopTimeout = Attribute.STOP_TIMEOUT.resolveModelAttribute(context, model).asLong();
        long transactionTimeout = Attribute.COMPLETE_TIMEOUT.resolveModelAttribute(context, model).asLong();

        ServiceDependency<Void> dependency = !EnumSet.of(TransactionMode.NONE, TransactionMode.BATCH).contains(mode) ? ServiceDependency.on(LOCAL_TRANSACTION_PROVIDER) : ServiceDependency.of(null);
        ServiceDependency<TransactionSynchronizationRegistry> tsr = mode == TransactionMode.NON_XA ? ServiceDependency.on(TRANSACTION_SYNCHRONIZATION_REGISTRY) : ServiceDependency.of(null);

        Supplier<TransactionConfiguration> configurationFactory = new Supplier<>() {
            @Override
            public TransactionConfiguration get() {
                TransactionConfigurationBuilder builder = new ConfigurationBuilder().transaction()
                        .lockingMode(locking)
                        .cacheStopTimeout(stopTimeout)
                        .completedTxTimeout(transactionTimeout)
                        .transactionMode((mode == TransactionMode.NONE) ? org.infinispan.transaction.TransactionMode.NON_TRANSACTIONAL : org.infinispan.transaction.TransactionMode.TRANSACTIONAL)
                        .useSynchronization(mode == TransactionMode.NON_XA)
                        .recovery().enabled(mode == TransactionMode.FULL_XA).transaction()
                        ;

                switch (mode) {
                    case NONE: {
                        break;
                    }
                    case BATCH: {
                        builder.transactionManagerLookup(new TransactionManagerProvider(EmbeddedTransactionManager.getInstance()));
                        break;
                    }
                    case NON_XA: {
                        builder.transactionSynchronizationRegistryLookup(new TransactionSynchronizationRegistryProvider(tsr.get()));
                    }
                    //$FALL-THROUGH$
                    default: {
                        builder.transactionManagerLookup(new TransactionManagerProvider(ContextTransactionManager.getInstance()));
                    }
                }
                return builder.create();
            }
        };
        List<ResourceServiceInstaller> installers = new ArrayList<>(2);
        installers.add(CapabilityServiceInstaller.builder(CAPABILITY, configurationFactory)
                .requires(List.of(dependency, tsr))
                .build());

        if (mode == TransactionMode.FULL_XA) {
            PathAddress cacheAddress = context.getCurrentAddress().getParent();
            String containerName = cacheAddress.getParent().getLastElement().getValue();
            String cacheName = cacheAddress.getLastElement().getValue();
            ServiceDependency<XAResourceRecoveryRegistry> recoveryRegistry = ServiceDependency.on(TransactionResourceDefinition.XA_RESOURCE_RECOVERY_REGISTRY);
            ServiceDependency<XAResourceRecovery> recovery = ServiceDependency.on(InfinispanServiceDescriptor.CACHE, containerName, cacheName).map(InfinispanXAResourceRecovery::new);
            Consumer<XAResourceRecovery> start = new Consumer<>() {
                @Override
                public void accept(XAResourceRecovery recovery) {
                    recoveryRegistry.get().addXAResourceRecovery(recovery);
                }
            };
            Consumer<XAResourceRecovery> stop = new Consumer<>() {
                @Override
                public void accept(XAResourceRecovery recovery) {
                    recoveryRegistry.get().removeXAResourceRecovery(recovery);
                }
            };
            installers.add(ServiceInstaller.builder(recovery)
                    .onStart(start)
                    .onStop(stop)
                    .requires(List.of(recoveryRegistry))
                    .build());
        }
        return ResourceServiceInstaller.combine(installers);
    }
}
