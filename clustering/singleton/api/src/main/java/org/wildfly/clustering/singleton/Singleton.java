/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.singleton;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Encapsulates an object that can be provided by multiple cluster members, but is only active on one member a time.
 * @author Paul Ferraro
 */
public interface Singleton {
    /**
     * Indicates whether this node is the primary provider of the singleton.
     * @return true, if this node is the primary node, false if it is a backup node.
     * @deprecated Use {@link #getSingletonState()} instead.
     */
    @Deprecated(forRemoval = true)
    default boolean isPrimary() {
        return this.getSingletonState().isPrimaryProvider();
    }

    /**
     * Returns the current primary provider of the singleton.
     * @return a cluster member
     * @deprecated Use {@link #getSingletonState()} instead.
     */
    @Deprecated(forRemoval = true)
    default org.wildfly.clustering.group.Node getPrimaryProvider() {
        return this.getSingletonState().getPrimaryProvider().map(LegacyMember::new).orElse(null);
    }

    /**
     * Returns the providers on which the given singleton is available.
     * @return a set of cluster members
     * @deprecated Use {@link #getSingletonState()} instead.
     */
    @Deprecated(forRemoval = true)
    default Set<org.wildfly.clustering.group.Node> getProviders() {
        return this.getSingletonState().getProviders().stream().map(LegacyMember::new).collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Returns a snapshot of the state of this singleton.
     * @return a snapshot of the state of this singleton.
     */
    SingletonState getSingletonState();
}
