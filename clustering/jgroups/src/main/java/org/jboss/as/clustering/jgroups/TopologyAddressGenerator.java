/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.clustering.jgroups;

import org.jgroups.Address;
import org.jgroups.Channel;
import org.jgroups.stack.AddressGenerator;
import org.jgroups.util.TopologyUUID;

/**
 * An AddressGenerator which generates TopologyUUID addresses with specified site, rack and machine ids.
 *
 * @author Tristan Tarrant
 *
 */
public class TopologyAddressGenerator implements AddressGenerator {

    private final Channel channel;
    private final String machine;
    private final String rack;
    private final String site;

    public TopologyAddressGenerator(Channel channel, String site, String rack, String machine) {
        this.channel = channel;
        this.site = site;
        this.rack = rack;
        this.machine = machine;
    }

    @Override
    public Address generateAddress() {
        return TopologyUUID.randomUUID(this.channel.getName(), this.site, this.rack, this.machine);
    }
}
