/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.jgroups;

import org.jgroups.Address;
import org.jgroups.stack.AddressGenerator;
import org.jgroups.util.ExtendedUUID;
import org.jgroups.util.Util;
import org.wildfly.clustering.jgroups.spi.TransportConfiguration;

/**
 * An AddressGenerator which generates ExtendedUUID addresses with specified site, rack and machine ids.
 *
 * @author Tristan Tarrant
 * @author Paul Ferraro
 */
public class TopologyAddressGenerator implements AddressGenerator {
    // Based on org.jgroups.util.TopologyUUID from JGroups 3.4.x
    private static final byte[] SITE = Util.stringToBytes("site-id");
    private static final byte[] RACK = Util.stringToBytes("rack-id");
    private static final byte[] MACHINE = Util.stringToBytes("machine-id");

    private final TransportConfiguration.Topology topology;

    public TopologyAddressGenerator(TransportConfiguration.Topology topology) {
        this.topology = topology;
    }

    @Override
    public Address generateAddress() {
        ExtendedUUID uuid = ExtendedUUID.randomUUID();
        uuid.put(SITE, Util.stringToBytes(this.topology.getSite()));
        uuid.put(RACK, Util.stringToBytes(this.topology.getRack()));
        uuid.put(MACHINE, Util.stringToBytes(this.topology.getMachine()));
        return uuid;
    }
}
