/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.server.provider.legacy;

import java.util.Set;
import java.util.stream.Collectors;

import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.server.infinispan.CacheContainerGroupMember;
import org.wildfly.clustering.server.infinispan.provider.CacheContainerServiceProviderRegistrar;
import org.wildfly.clustering.server.provider.ServiceProviderListener;
import org.wildfly.clustering.server.provider.ServiceProviderRegistration;
import org.wildfly.extension.clustering.server.group.legacy.LegacyCacheContainerGroup;
import org.wildfly.extension.clustering.server.group.legacy.LegacyCacheContainerGroupMember;

/**
 * @author Paul Ferraro
 */
@Deprecated
public interface LegacyCacheServiceProviderRegistry<T> extends org.wildfly.clustering.provider.ServiceProviderRegistry<T> {

    CacheContainerServiceProviderRegistrar<T> unwrap();

    @Override
    default LegacyCacheContainerGroup getGroup() {
        return LegacyCacheContainerGroup.wrap(this.unwrap().getGroup());
    }

    @Override
    default org.wildfly.clustering.provider.ServiceProviderRegistration<T> register(T service) {
        return this.register(service, null);
    }

    @Override
    default org.wildfly.clustering.provider.ServiceProviderRegistration<T> register(T service, org.wildfly.clustering.provider.ServiceProviderRegistration.Listener listener) {
        ServiceProviderRegistration<T, CacheContainerGroupMember> registration = this.unwrap().register(service, new ServiceProviderListener<>() {
            @Override
            public void providersChanged(Set<CacheContainerGroupMember> providers) {
                listener.providersChanged(providers.stream().map(LegacyCacheContainerGroupMember::wrap).collect(Collectors.toSet()));
            }
        });
        LegacyCacheContainerGroup group = this.getGroup();
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

    static <T> LegacyCacheServiceProviderRegistry<T> wrap(CacheContainerServiceProviderRegistrar<T> registrar) {
        return () -> registrar;
    }
}
