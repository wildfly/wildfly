/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.singleton;

import java.util.Optional;
import java.util.Set;

import org.wildfly.clustering.server.GroupMember;

/**
 * An immutable snapshot of the state of a singleton.
 * @author Paul Ferraro
 */
public interface SingletonState extends SingletonStatus {
    /**
     * Returns the primary provider of the singleton, if one is present.
     * @return the primary provider of the singleton when present
     */
    Optional<GroupMember> getPrimaryProvider();

    /**
     * Returns the set of members that provide the singleton, including the primary provider.
     * @return a set of cluster members
     */
    Set<GroupMember> getProviders();
}
