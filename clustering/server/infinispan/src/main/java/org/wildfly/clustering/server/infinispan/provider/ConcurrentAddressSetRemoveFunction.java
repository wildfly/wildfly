/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.server.infinispan.provider;

import org.infinispan.remoting.transport.Address;
import org.wildfly.clustering.ee.cache.function.ConcurrentSetRemoveFunction;

/**
 * Concurrent {@link java.util.Set#remove(Object)} function for an {@link Address}.
 * @author Paul Ferraro
 */
public class ConcurrentAddressSetRemoveFunction extends ConcurrentSetRemoveFunction<Address> {

    public ConcurrentAddressSetRemoveFunction(Address address) {
        super(address);
    }
}
