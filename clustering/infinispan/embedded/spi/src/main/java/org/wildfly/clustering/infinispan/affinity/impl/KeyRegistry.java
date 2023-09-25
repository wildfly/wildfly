/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.infinispan.affinity.impl;

import java.util.Set;
import java.util.concurrent.BlockingQueue;

import org.infinispan.remoting.transport.Address;

/**
 * A registry of keys with affinity to a given address.
 * @author Paul Ferraro
 */
public interface KeyRegistry<K> {

    /**
     * Returns the addresses for which pre-generated keys are available.
     * @return a set of cluster members
     */
    Set<Address> getAddresses();

    /**
     * Returns a queue of pre-generated keys with affinity for the specified address.
     * @param address the address of a cluster member.
     * @return a queue of pre-generated keys
     */
    BlockingQueue<K> getKeys(Address address);
}
