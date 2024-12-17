/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.server.provider.legacy;

import java.util.Set;
import java.util.stream.Collectors;

import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.server.group.GroupMember;
import org.wildfly.clustering.server.provider.ServiceProviderListener;
import org.wildfly.clustering.server.provider.ServiceProviderRegistrar;
import org.wildfly.clustering.server.provider.ServiceProviderRegistration;
import org.wildfly.extension.clustering.server.group.legacy.LegacyGroup;

/**
 * @author Paul Ferraro
 */
@Deprecated
public interface LegacyServiceProviderRegistry<T, A extends Comparable<A>, M extends GroupMember<A>> extends org.wildfly.clustering.provider.ServiceProviderRegistry<T> {

    ServiceProviderRegistrar<T, M> unwrap();

    @Override
    LegacyGroup<A, M> getGroup();

    @Override
    default org.wildfly.clustering.provider.ServiceProviderRegistration<T> register(T service) {
        return this.register(service, null);
    }

    @Override
    default org.wildfly.clustering.provider.ServiceProviderRegistration<T> register(T service, org.wildfly.clustering.provider.ServiceProviderRegistration.Listener listener) {
        LegacyGroup<A, M> group = this.getGroup();
        ServiceProviderRegistration<T, M> registration = this.unwrap().register(service, new ServiceProviderListener<>() {
            @Override
            public void providersChanged(Set<M> providers) {
                listener.providersChanged(providers.stream().map(group::wrap).collect(Collectors.toSet()));
            }
        });
        return new org.wildfly.clustering.provider.ServiceProviderRegistration<>() {
            @Override
            public void close() {
                registration.close();
            }

            @Override
            public T getService() {
                return registration.getService();
            }

            @Override
            public Set<Node> getProviders() {
                return registration.getProviders().stream().map(group::wrap).collect(Collectors.toSet());
            }
        };
    }

    @Override
    default Set<Node> getProviders(T service) {
        return this.unwrap().getProviders(service).stream().map(this.getGroup()::wrap).collect(Collectors.toSet());
    }

    @Override
    default Set<T> getServices() {
        return this.unwrap().getServices();
    }
}
