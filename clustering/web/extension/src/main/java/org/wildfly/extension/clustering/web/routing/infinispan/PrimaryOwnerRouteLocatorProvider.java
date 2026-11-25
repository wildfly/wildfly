/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web.routing.infinispan;

import java.util.function.BiFunction;

import org.infinispan.Cache;
import org.wildfly.clustering.cache.Key;
import org.wildfly.clustering.function.Function;
import org.wildfly.clustering.function.UnaryOperator;
import org.wildfly.clustering.server.infinispan.CacheContainerGroupMember;
import org.wildfly.clustering.server.infinispan.affinity.UnaryGroupMemberAffinity;
import org.wildfly.clustering.server.infinispan.registry.CacheContainerRegistry;
import org.wildfly.clustering.session.cache.affinity.SessionAffinityRegistryGroupMemberMapper;
import org.wildfly.clustering.session.cache.affinity.UnarySessionAffinity;

/**
 * Factory for creating a service configurator for a primary owner route locator.
 * @author Paul Ferraro
 */
public class PrimaryOwnerRouteLocatorProvider extends InfinispanRouteLocatorProvider {

    public PrimaryOwnerRouteLocatorProvider() {
        super(new BiFunction<>() {
            @Override
            public UnaryOperator<String> apply(Cache<Key<String>, ?> cache, CacheContainerRegistry<String, Void> registry) {
                Function<String, CacheContainerGroupMember> affinity = new UnaryGroupMemberAffinity<>(cache, registry.getGroup());
                return new UnarySessionAffinity<>(affinity, new SessionAffinityRegistryGroupMemberMapper<>(registry));
            }
        });
    }
}
