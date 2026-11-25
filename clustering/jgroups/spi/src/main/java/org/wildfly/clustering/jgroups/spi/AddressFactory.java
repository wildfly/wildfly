/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.jgroups.spi;

import org.jgroups.Address;

/**
 * A factory for creating JGroups addresses.
 * @author Paul Ferraro
 */
public interface AddressFactory {
    /**
     * Creates a JGroups address using the specified logical name and topology.
     * @param name a logical name
     * @param topology a topology
     * @return a JGroups address using the specified logical name and topology.
     */
    Address createAddress(String name, TransportConfiguration.Topology topology);
}
