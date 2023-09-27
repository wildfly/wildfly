/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.infinispan.distribution;

import java.util.List;

import org.infinispan.remoting.transport.Address;

/**
 * Provides key distribution functions.
 * @author Paul Ferraro
 */
public interface KeyDistribution {

    /**
     * Returns the primary owner of the specified key.
     * @param key a cache key
     * @return the address of the primary owner
     */
    Address getPrimaryOwner(Object key);

    /**
     * Returns the owners of the specified key.
     * @param key a cache key
     * @return a list of addresses for each owner
     */
    List<Address> getOwners(Object key);
}
