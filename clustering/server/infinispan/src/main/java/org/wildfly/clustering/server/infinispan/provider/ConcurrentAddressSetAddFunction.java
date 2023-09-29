/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.server.infinispan.provider;

import org.infinispan.remoting.transport.Address;
import org.wildfly.clustering.ee.cache.function.ConcurrentSetAddFunction;

/**
 * Concurrent {@link java.util.Set#add(Object)} function for an {@link Address}.
 * @author Paul Ferraro
 * @deprecated Superseded by {@link AddressSetAddFunction}.
 */
@Deprecated(forRemoval = true)
public class ConcurrentAddressSetAddFunction extends ConcurrentSetAddFunction<Address> {

    public ConcurrentAddressSetAddFunction(Address address) {
        super(address);
    }
}
