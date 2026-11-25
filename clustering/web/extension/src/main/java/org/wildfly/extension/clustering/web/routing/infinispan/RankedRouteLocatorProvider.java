/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web.routing.infinispan;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import org.infinispan.Cache;
import org.wildfly.clustering.cache.Key;
import org.wildfly.clustering.server.infinispan.CacheContainerGroupMember;
import org.wildfly.clustering.server.infinispan.affinity.NaryGroupMemberAffinity;
import org.wildfly.clustering.server.infinispan.registry.CacheContainerRegistry;
import org.wildfly.clustering.session.cache.affinity.NarySessionAffinity;
import org.wildfly.clustering.session.cache.affinity.NarySessionAffinityConfiguration;
import org.wildfly.clustering.session.cache.affinity.SessionAffinityRegistryGroupMemberMapper;

/**
 * Factory for creating a service configurator for a ranked route locator.
 * @author Paul Ferraro
 */
public class RankedRouteLocatorProvider extends InfinispanRouteLocatorProvider {

    private final NarySessionAffinityConfiguration config;

    public RankedRouteLocatorProvider(NarySessionAffinityConfiguration config) {
        super(new BiFunction<>() {
            @Override
            public UnaryOperator<String> apply(Cache<Key<String>, ?> cache, CacheContainerRegistry<String, Void> registry) {
                Function<String, List<CacheContainerGroupMember>> affinity = new NaryGroupMemberAffinity<>(cache, registry.getGroup());
                return new NarySessionAffinity<>(affinity, new SessionAffinityRegistryGroupMemberMapper<>(registry), config);
            }
        });
        this.config = config;
    }

    public NarySessionAffinityConfiguration getNarySessionAffinityConfiguration() {
        return this.config;
    }
}
