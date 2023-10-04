/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.server.infinispan.provider;

import java.util.Collection;

import org.infinispan.remoting.transport.Address;
import org.wildfly.clustering.ee.cache.function.SetRemoveFunction;

/**
 * @author Paul Ferraro
 */
public class AddressSetRemoveFunction extends SetRemoveFunction<Address> {

    public AddressSetRemoveFunction(Address address) {
        super(address);
    }

    public AddressSetRemoveFunction(Collection<Address> addresses) {
        super(addresses);
    }
}
