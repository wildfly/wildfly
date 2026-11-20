/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.server.provider;

import java.util.Set;

import org.jboss.as.server.suspend.SuspendableActivityRegistrar;
import org.wildfly.clustering.function.Consumer;
import org.wildfly.clustering.function.Supplier;
import org.wildfly.clustering.server.GroupMember;
import org.wildfly.clustering.server.Registrar;
import org.wildfly.clustering.server.Registration;
import org.wildfly.clustering.server.local.provider.DefaultServiceProviderRegistration;
import org.wildfly.clustering.server.provider.ServiceProviderRegistrar;
import org.wildfly.clustering.server.provider.ServiceProviderRegistration;
import org.wildfly.clustering.server.provider.ServiceProviderRegistrationListener;
import org.wildfly.clustering.server.provider.ServiceProviderRegistryListener;
import org.wildfly.extension.clustering.server.SuspendableRegistrar;

/**
 * A {@link ServiceProviderRegistrar} decorator that creates suspendable registrations.
 * @author Paul Ferraro
 */
public abstract class AbstractSuspendableServiceProviderRegistrar<T, M extends GroupMember> implements ServiceProviderRegistrar<T, M> {

    private final ServiceProviderRegistrar<T, M> registrar;
    private final Registrar<Supplier<ServiceProviderRegistration<T, M>>> suspendableRegistrar;

    AbstractSuspendableServiceProviderRegistrar(ServiceProviderRegistrar<T, M> registrar, SuspendableActivityRegistrar activityRegistrar) {
        this.registrar = registrar;
        this.suspendableRegistrar = new SuspendableRegistrar<>(activityRegistrar);
    }

    @Override
    public Set<M> getProviders(T service) {
        return this.registrar.getProviders(service);
    }

    @Override
    public Set<T> getServices() {
        return this.registrar.getServices();
    }

    @Override
    public Registration register(ServiceProviderRegistryListener<T, M> listener) {
        return this.registrar.register(listener);
    }

    @Override
    public ServiceProviderRegistration<T, M> register(T service, ServiceProviderRegistrationListener<M> listener) {
        return this.register(service, () -> this.registrar.register(service, listener));
    }

    @Override
    public ServiceProviderRegistration<T, M> register(T service) {
        return this.register(service, Supplier.of(service).thenApply(this.registrar::register));
    }

    private ServiceProviderRegistration<T, M> register(T service, Supplier<ServiceProviderRegistration<T, M>> registrationFactory) {
        Registration registration = this.suspendableRegistrar.register(registrationFactory);
        return new DefaultServiceProviderRegistration<>(this.registrar, service, Supplier.of(registration).thenAccept(Consumer.close()));
    }
}
