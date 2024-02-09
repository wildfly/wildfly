/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.singleton;

import static org.wildfly.extension.clustering.singleton.SingletonPolicyResourceDefinition.Attribute.CACHE;
import static org.wildfly.extension.clustering.singleton.SingletonPolicyResourceDefinition.Attribute.CACHE_CONTAINER;
import static org.wildfly.extension.clustering.singleton.SingletonPolicyResourceDefinition.Attribute.QUORUM;
import static org.wildfly.extension.clustering.singleton.SingletonPolicyResourceDefinition.Capability.POLICY;

import java.util.function.Consumer;

import org.jboss.as.clustering.controller.CapabilityServiceNameProvider;
import org.jboss.as.clustering.controller.ResourceServiceConfigurator;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.Service;
import org.jboss.msc.service.LifecycleEvent;
import org.jboss.msc.service.LifecycleListener;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.Registrar;
import org.wildfly.clustering.Registration;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.service.CompositeDependency;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SupplierDependency;
import org.wildfly.clustering.singleton.Singleton;
import org.wildfly.clustering.singleton.SingletonElectionPolicy;
import org.wildfly.clustering.singleton.SingletonPolicy;
import org.wildfly.clustering.singleton.SingletonServiceBuilderFactory;
import org.wildfly.clustering.singleton.service.SingletonCacheRequirement;
import org.wildfly.service.capture.ServiceValueRegistry;

/**
 * Builds a service that provides a {@link SingletonPolicy}.
 * @author Paul Ferraro
 */
@SuppressWarnings({ "removal", "deprecation" })
public class SingletonPolicyServiceConfigurator extends CapabilityServiceNameProvider implements ResourceServiceConfigurator, SingletonPolicy {

    private final ServiceValueRegistry<Singleton> registry;
    private final SupplierDependency<SingletonElectionPolicy> policy;

    private volatile Registrar<ServiceName> registrar;
    private volatile SupplierDependency<SingletonServiceBuilderFactory> factory;
    private volatile int quorum;

    public SingletonPolicyServiceConfigurator(PathAddress address, ServiceValueRegistry<Singleton> registry) {
        super(POLICY, address);
        this.policy = new ServiceSupplierDependency<>(ElectionPolicyResourceDefinition.Capability.ELECTION_POLICY.getServiceName(address.append(ElectionPolicyResourceDefinition.WILDCARD_PATH)));
        this.registry = registry;
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceBuilder<?> builder = target.addService(this.getServiceName());
        Consumer<SingletonPolicy> policy = new CompositeDependency(this.policy, this.factory).register(builder).provides(this.getServiceName());
        Service service = Service.newInstance(policy, this);
        return builder.setInstance(service).setInitialMode(ServiceController.Mode.PASSIVE);
    }

    @Override
    public ServiceConfigurator configure(OperationContext context, ModelNode model) throws OperationFailedException {
        String containerName = CACHE_CONTAINER.resolveModelAttribute(context, model).asString();
        String cacheName = CACHE.resolveModelAttribute(context, model).asStringOrNull();
        this.factory = new ServiceSupplierDependency<>(SingletonCacheRequirement.SINGLETON_SERVICE_BUILDER_FACTORY.getServiceName(context, containerName, cacheName));
        this.quorum = QUORUM.resolveModelAttribute(context, model).asInt();
        this.registrar = (SingletonPolicyResource) context.readResource(PathAddress.EMPTY_ADDRESS);
        return this;
    }

    @Override
    public ServiceConfigurator createSingletonServiceConfigurator(ServiceName name) {
        ServiceConfigurator configurator = this.factory.get().createSingletonServiceConfigurator(name)
                .electionPolicy(this.policy.get())
                .requireQuorum(this.quorum)
                ;
        return new SingletonServiceConfigurator(configurator, new SingletonServiceLifecycleListener(name, this.registrar), this.registry);
    }

    @Override
    public <T> Builder<T> createSingletonServiceBuilder(ServiceName name, org.jboss.msc.service.Service<T> service) {
        Builder<T> builder = this.factory.get().createSingletonServiceBuilder(name, service)
                .electionPolicy(this.policy.get())
                .requireQuorum(this.quorum)
                ;
        return new SingletonServiceBuilder<>(builder, new SingletonServiceLifecycleListener(name, this.registrar));
    }

    @Override
    public <T> Builder<T> createSingletonServiceBuilder(ServiceName name, org.jboss.msc.service.Service<T> primaryService, org.jboss.msc.service.Service<T> backupService) {
        Builder<T> builder = this.factory.get().createSingletonServiceBuilder(name, primaryService, backupService)
                .electionPolicy(this.policy.get())
                .requireQuorum(this.quorum)
                ;
        return new SingletonServiceBuilder<>(builder, new SingletonServiceLifecycleListener(name, this.registrar));
    }

    @Override
    public String toString() {
        return this.getServiceName().getSimpleName();
    }

    private class SingletonServiceConfigurator implements ServiceConfigurator {

        private final ServiceConfigurator configurator;
        private final LifecycleListener listener;
        private final ServiceValueRegistry<Singleton> registry;

        SingletonServiceConfigurator(ServiceConfigurator configurator, LifecycleListener listener, ServiceValueRegistry<Singleton> registry) {
            this.configurator = configurator;
            this.listener = listener;
            this.registry = registry;
        }

        @Override
        public ServiceName getServiceName() {
            return this.configurator.getServiceName();
        }

        @Override
        public ServiceBuilder<?> build(ServiceTarget target) {
            ServiceName singletonServiceName = this.getServiceName().append("singleton");
            ServiceController<?> controller = this.registry.capture(singletonServiceName).install(target);
            return this.configurator.build(target).addListener(this.listener).addListener(new LifecycleListener() {
                @Override
                public void handleEvent(ServiceController<?> c, LifecycleEvent event) {
                    if (event == LifecycleEvent.REMOVED) {
                        controller.setMode(ServiceController.Mode.REMOVE);
                    }
                }
            });
        }
    }

    private class SingletonServiceBuilder<T> implements Builder<T> {

        private final Builder<T> builder;
        private final LifecycleListener listener;

        SingletonServiceBuilder(Builder<T> builder, LifecycleListener listener) {
            this.builder = builder;
            this.listener = listener;
        }

        @Override
        public ServiceName getServiceName() {
            return this.builder.getServiceName();
        }

        @Override
        public ServiceBuilder<T> build(ServiceTarget target) {
            return this.builder.build(target).addListener(this.listener);
        }
    }

    private class SingletonServiceLifecycleListener implements LifecycleListener {
        private final ServiceName name;
        private final Registrar<ServiceName> registrar;
        private Registration registration;

        SingletonServiceLifecycleListener(ServiceName name, Registrar<ServiceName> registrar) {
            this.name = name;
            this.registrar = registrar;
        }

        @Override
        public synchronized void handleEvent(ServiceController<?> controller, LifecycleEvent event) {
            switch (event) {
                case UP: {
                    this.registration = this.registrar.register(this.name);
                    break;
                }
                default: {
                    if (this.registration != null) {
                        this.registration.close();
                    }
                    break;
                }
            }
        }
    }
}
