/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.server.infinispan.provider;

import java.util.Collection;

import org.infinispan.remoting.transport.Address;
import org.wildfly.clustering.ee.cache.function.SetAddFunction;

/**
 * @author Paul Ferraro
 */
public class AddressSetAddFunction extends SetAddFunction<Address> {

    public AddressSetAddFunction(Address address) {
        super(address);
    }

    public AddressSetAddFunction(Collection<Address> addresses) {
        super(addresses);
    }
}
