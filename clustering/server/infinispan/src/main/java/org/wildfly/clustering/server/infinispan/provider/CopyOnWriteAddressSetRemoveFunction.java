/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.server.infinispan.provider;

import org.infinispan.remoting.transport.Address;
import org.wildfly.clustering.ee.cache.function.CopyOnWriteSetRemoveFunction;

/**
 * Copy-on-write {@link java.util.Set#remove(Object)} function for an {@link Address}.
 * @author Paul Ferraro
 * @deprecated Superseded by {@link AddressSetRemoveFunction}.
 */
@Deprecated(forRemoval = true)
public class CopyOnWriteAddressSetRemoveFunction extends CopyOnWriteSetRemoveFunction<Address> {

    public CopyOnWriteAddressSetRemoveFunction(Address address) {
        super(address);
    }
}
