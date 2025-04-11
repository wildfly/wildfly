/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.singleton;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.ResourceRegistration;
import org.jboss.as.controller.ServiceNameFactory;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.DelegatingServiceBuilder;
import org.jboss.msc.service.DelegatingServiceTarget;
import org.jboss.msc.service.LifecycleEvent;
import org.jboss.msc.service.LifecycleListener;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.infinispan.service.InfinispanCacheConfigurationAttributeGroup;
import org.wildfly.clustering.server.Registrar;
import org.wildfly.clustering.server.Registration;
import org.wildfly.clustering.server.service.BinaryServiceConfiguration;
import org.wildfly.clustering.server.service.CacheConfigurationAttributeGroup;
import org.wildfly.clustering.singleton.Singleton;
import org.wildfly.clustering.singleton.compat.SingletonServiceTargetFactory;
import org.wildfly.clustering.singleton.election.SingletonElectionListener;
import org.wildfly.clustering.singleton.election.SingletonElectionPolicy;
import org.wildfly.clustering.singleton.service.ServiceTargetFactory;
import org.wildfly.clustering.singleton.service.SingletonPolicy;
import org.wildfly.clustering.singleton.service.SingletonServiceBuilder;
import org.wildfly.clustering.singleton.service.SingletonServiceController;
import org.wildfly.clustering.singleton.service.SingletonServiceTarget;
import org.wildfly.service.capture.ServiceValueExecutorRegistry;
import org.wildfly.service.capture.ServiceValueRegistry;
import org.wildfly.subsystem.resource.ChildResourceDefinitionRegistrar;
import org.wildfly.subsystem.resource.ManagementResourceRegistrar;
import org.wildfly.subsystem.resource.ManagementResourceRegistrationContext;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;
import org.wildfly.subsystem.service.ResourceServiceConfigurator;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.capability.CapabilityServiceInstaller;

/**
 * Registers a resource definition for a singleton policy.
 * @author Paul Ferraro
 */
public class SingletonPolicyResourceDefinitionRegistrar implements ChildResourceDefinitionRegistrar, ResourceServiceConfigurator {

    static final ResourceRegistration REGISTRATION = ResourceRegistration.of(PathElement.pathElement("singleton-policy"));

    private static final RuntimeCapability<Void> CAPABILITY = RuntimeCapability.Builder.of(ServiceTargetFactory.SERVICE_DESCRIPTOR).build();

    static final CacheConfigurationAttributeGroup CACHE_ATTRIBUTE_GROUP = new InfinispanCacheConfigurationAttributeGroup(CAPABILITY);
    static final AttributeDefinition QUORUM = new SimpleAttributeDefinitionBuilder("quorum", ModelType.INT)
            .setRequired(false)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(1))
            .setValidator(IntRangeValidator.POSITIVE)
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .build();

    private final ServiceValueExecutorRegistry<Singleton> registry = ServiceValueExecutorRegistry.newInstance();

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent, ManagementResourceRegistrationContext context) {
        ResourceDescriptionResolver resolver = SingletonSubsystemResourceDefinitionRegistrar.RESOLVER.createChildResolver(REGISTRATION.getPathElement());
        ResourceDescriptor descriptor = ResourceDescriptor.builder(resolver)
                .addCapability(CAPABILITY)
                .addAttributes(CACHE_ATTRIBUTE_GROUP.getAttributes())
                .addAttributes(List.of(QUORUM))
                .requireSingletonChildResource(ElectionPolicyResourceRegistration.SIMPLE)
                .withRuntimeHandler(ResourceOperationRuntimeHandler.configureService(this))
                .withResourceTransformation(SingletonPolicyResource::new)
                .build();
        ManagementResourceRegistration registration = parent.registerSubModel(ResourceDefinition.builder(REGISTRATION, resolver).build());
        ManagementResourceRegistrar.of(descriptor).register(registration);

        new RandomElectionPolicyResourceDefinitionRegistrar().register(registration, context);
        new SimpleElectionPolicyResourceDefinitionRegistrar().register(registration, context);

        if (context.isRuntimeOnlyRegistrationValid()) {
            new SingletonDeploymentResourceDefinitionRegistrar(this.registry).register(registration, context);
            new SingletonServiceResourceDefinitionRegistrar(this.registry).register(registration, context);
        }

        return registration;
    }

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        String name = context.getCurrentAddressValue();
        BinaryServiceConfiguration configuration = CACHE_ATTRIBUTE_GROUP.resolve(context, model);
        int quorum = QUORUM.resolveModelAttribute(context, model).asInt();

        ServiceDependency<SingletonElectionPolicy> electionPolicy = ServiceDependency.on(SingletonElectionPolicy.SERVICE_DESCRIPTOR, name);
        ServiceDependency<SingletonServiceTargetFactory> targetFactory = configuration.getServiceDependency(org.wildfly.clustering.singleton.service.SingletonServiceTargetFactory.SERVICE_DESCRIPTOR).map(SingletonServiceTargetFactory.class::cast);
        ServiceValueRegistry<Singleton> registry = this.registry;
        Registrar<ServiceName> registrar = (SingletonPolicyResource) context.readResource(PathAddress.EMPTY_ADDRESS);

        Supplier<SingletonServiceTargetFactory> factory = targetFactory.map(new UnaryOperator<>() {
            @Override
            public SingletonServiceTargetFactory apply(SingletonServiceTargetFactory factory) {
                return new SingletonServiceTargetFactory() {
                    private final SingletonElectionPolicy policy = electionPolicy.get();

                    @Override
                    public SingletonServiceTarget createSingletonServiceTarget(ServiceTarget target) {
                        SingletonServiceTarget singletonTarget = factory.createSingletonServiceTarget(target);
                        return new ConfiguredSingletonServiceTarget(singletonTarget, builder -> builder.withElectionPolicy(this.policy).requireQuorum(quorum), registrar, registry);
                    }

                    @Deprecated
                    @Override
                    public SingletonServiceConfigurator createSingletonServiceConfigurator(ServiceName name) {
                        org.wildfly.clustering.singleton.compat.SingletonServiceConfigurator configurator = factory.createSingletonServiceConfigurator(name)
                                .withElectionPolicy(this.policy)
                                .requireQuorum(quorum)
                                ;
                        return new SingletonServiceConfigurator(configurator, registrar, registry);
                    }

                    @Deprecated
                    @Override
                    public <T> org.wildfly.clustering.singleton.compat.SingletonServiceBuilder<T> createSingletonServiceBuilder(ServiceName name, org.jboss.msc.service.Service<T> service) {
                        org.wildfly.clustering.singleton.compat.SingletonServiceBuilder<T> builder = factory.createSingletonServiceBuilder(name, service)
                                .withElectionPolicy(this.policy)
                                .requireQuorum(quorum)
                                ;
                        return new LegacySingletonServiceBuilder<>(builder, registrar, registry);
                    }

                    @Deprecated
                    @Override
                    public <T> org.wildfly.clustering.singleton.compat.SingletonServiceBuilder<T> createSingletonServiceBuilder(ServiceName name, org.jboss.msc.service.Service<T> primaryService, org.jboss.msc.service.Service<T> backupService) {
                        org.wildfly.clustering.singleton.compat.SingletonServiceBuilder<T> builder = factory.createSingletonServiceBuilder(name, primaryService, backupService)
                                .withElectionPolicy(this.policy)
                                .requireQuorum(quorum)
                                ;
                        return new LegacySingletonServiceBuilder<>(builder, registrar, registry);
                    }

                    @Override
                    public String toString() {
                        return name;
                    }
                };
            }
        });
        return CapabilityServiceInstaller.builder(CAPABILITY, factory)
                .requires(List.of(electionPolicy, targetFactory))
                .provides(ServiceNameFactory.resolveServiceName(SingletonPolicy.SERVICE_DESCRIPTOR, name))
                .provides(ServiceNameFactory.resolveServiceName(org.wildfly.clustering.singleton.SingletonPolicy.SERVICE_DESCRIPTOR, name))
                .build();
    }

    private static class ConfiguredSingletonServiceTarget extends DelegatingServiceTarget implements SingletonServiceTarget {
        private final SingletonServiceTarget target;
        private final UnaryOperator<SingletonServiceBuilder<?>> configurator;
        private final Registrar<ServiceName> registrar;
        private final ServiceValueRegistry<Singleton> registry;

        ConfiguredSingletonServiceTarget(SingletonServiceTarget target, UnaryOperator<SingletonServiceBuilder<?>> configurator, Registrar<ServiceName> registrar, ServiceValueRegistry<Singleton> registry) {
            super(target);
            this.target = target;
            this.configurator = configurator;
            this.registrar = registrar;
            this.registry = registry;
        }

        @Override
        public SingletonServiceBuilder<?> addService() {
            return new ConfiguredSingletonServiceBuilder<>(null, this.configurator.apply(this.target.addService()), this.registrar, this.registry);
        }

        @Deprecated
        @Override
        public SingletonServiceBuilder<?> addService(ServiceName name) {
            return new ConfiguredSingletonServiceBuilder<>(name, this.configurator.apply(this.target.addService(name)), this.registrar, this.registry);
        }

        @Deprecated
        @Override
        public <T> SingletonServiceBuilder<T> addService(ServiceName name, org.jboss.msc.service.Service<T> service) {
            SingletonServiceBuilder<T> builder = this.target.addService(name, service);
            this.configurator.apply(builder);
            return new ConfiguredSingletonServiceBuilder<>(name, builder, this.registrar, this.registry);
        }

        @Override
        public SingletonServiceTarget subTarget() {
            return new ConfiguredSingletonServiceTarget(this.target.subTarget(), this.configurator, this.registrar, this.registry);
        }
    }

    private static class ConfiguredSingletonServiceBuilder<T> extends DelegatingServiceBuilder<T> implements SingletonServiceBuilder<T> {
        private final SingletonServiceBuilder<T> builder;
        private final AtomicReference<ServiceName> name = new AtomicReference<>();
        private final Registrar<ServiceName> registrar;
        private final ServiceValueRegistry<Singleton> registry;

        ConfiguredSingletonServiceBuilder(ServiceName name, SingletonServiceBuilder<T> builder, Registrar<ServiceName> registrar, ServiceValueRegistry<Singleton> registry) {
            super(builder);
            this.name.set(name);
            this.builder = builder;
            this.registrar = registrar;
            this.registry = registry;
        }

        @Override
        public SingletonServiceBuilder<T> requireQuorum(int quorum) {
            this.builder.requireQuorum(quorum);
            return this;
        }

        @Override
        public SingletonServiceBuilder<T> withElectionPolicy(SingletonElectionPolicy policy) {
            this.builder.withElectionPolicy(policy);
            return this;
        }

        @Override
        public SingletonServiceBuilder<T> withElectionListener(SingletonElectionListener listener) {
            this.builder.withElectionListener(listener);
            return this;
        }

        @Override
        public <V> Consumer<V> provides(ServiceName... names) {
            // For anonymous services, identify service by its provided value
            this.name.compareAndSet(null, names[0]);
            return super.provides(names);
        }

        @Override
        public SingletonServiceBuilder<T> setInstance(org.jboss.msc.Service service) {
            // For anonymous services with no provided value, generate a ServiceName from the service class name
            this.name.compareAndSet(null, ServiceName.parse(service.getClass().getName()));
            this.builder.setInstance(service);
            return this;
        }

        @Override
        public SingletonServiceBuilder<T> setInitialMode(Mode mode) {
            this.builder.setInitialMode(mode);
            return this;
        }

        @Override
        public SingletonServiceBuilder<T> addListener(LifecycleListener listener) {
            this.builder.addListener(listener);
            return this;
        }

        @Override
        public SingletonServiceController<T> install() {
            SingletonServiceController<T> controller = this.builder.install();
            ServiceName name = this.name.get();
            if (name != null) {
                ServiceValueRegistry<Singleton> registry = this.registry;
                Consumer<Singleton> captor = registry.add(name);
                captor.accept(controller);
                Registration registration = this.registrar.register(name);
                controller.addListener(new LifecycleListener() {
                    @Override
                    public void handleEvent(ServiceController<?> controller, LifecycleEvent event) {
                        if (event == LifecycleEvent.REMOVED) {
                            registration.close();
                            captor.accept(null);
                            registry.remove(name);
                        }
                    }
                });
            }
            return controller;
        }
    }

    @Deprecated
    private class SingletonServiceConfigurator implements org.wildfly.clustering.singleton.compat.SingletonServiceConfigurator {
        private final org.wildfly.clustering.singleton.compat.SingletonServiceConfigurator configurator;
        private final Registrar<ServiceName> registrar;
        private final ServiceValueRegistry<Singleton> registry;

        SingletonServiceConfigurator(org.wildfly.clustering.singleton.compat.SingletonServiceConfigurator configurator, Registrar<ServiceName> registrar, ServiceValueRegistry<Singleton> registry) {
            this.configurator = configurator;
            this.registrar = registrar;
            this.registry = registry;
        }

        @Override
        public ServiceName getServiceName() {
            return this.configurator.getServiceName();
        }

        @Override
        public SingletonServiceBuilder<?> build(ServiceTarget target) {
            return new ConfiguredSingletonServiceBuilder<>(this.getServiceName(), this.configurator.build(target), this.registrar, this.registry);
        }

        @Override
        public org.wildfly.clustering.singleton.compat.SingletonServiceConfigurator withElectionPolicy(SingletonElectionPolicy policy) {
            this.configurator.withElectionPolicy(policy);
            return this;
        }

        @Override
        public org.wildfly.clustering.singleton.compat.SingletonServiceConfigurator withElectionListener(SingletonElectionListener listener) {
            this.configurator.withElectionListener(listener);
            return this;
        }

        @Override
        public org.wildfly.clustering.singleton.compat.SingletonServiceConfigurator requireQuorum(int quorum) {
            this.configurator.requireQuorum(quorum);
            return this;
        }
    }

    @Deprecated
    private class LegacySingletonServiceBuilder<T> implements org.wildfly.clustering.singleton.compat.SingletonServiceBuilder<T> {
        private final org.wildfly.clustering.singleton.compat.SingletonServiceBuilder<T> builder;
        private final Registrar<ServiceName> registrar;
        private final ServiceValueRegistry<Singleton> registry;

        LegacySingletonServiceBuilder(org.wildfly.clustering.singleton.compat.SingletonServiceBuilder<T> builder, Registrar<ServiceName> registrar, ServiceValueRegistry<Singleton> registry) {
            this.builder = builder;
            this.registrar = registrar;
            this.registry = registry;
        }

        @Override
        public ServiceName getServiceName() {
            return this.builder.getServiceName();
        }

        @Override
        public SingletonServiceBuilder<T> build(ServiceTarget target) {
            return new ConfiguredSingletonServiceBuilder<>(this.getServiceName(), this.builder.build(target), this.registrar, this.registry);
        }

        @Override
        public org.wildfly.clustering.singleton.compat.SingletonServiceBuilder<T> withElectionPolicy(SingletonElectionPolicy policy) {
            this.builder.withElectionPolicy(policy);
            return this;
        }

        @Override
        public org.wildfly.clustering.singleton.compat.SingletonServiceBuilder<T> withElectionListener(SingletonElectionListener listener) {
            this.builder.withElectionListener(listener);
            return this;
        }

        @Override
        public org.wildfly.clustering.singleton.compat.SingletonServiceBuilder<T> requireQuorum(int quorum) {
            this.builder.requireQuorum(quorum);
            return this;
        }
    }
}
