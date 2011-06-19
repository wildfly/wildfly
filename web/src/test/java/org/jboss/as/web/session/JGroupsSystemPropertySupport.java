/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.web.session;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Utility class that sets and clears JGroups-related system properties.
 *
 * @author Brian Stansberry
 */
public class JGroupsSystemPropertySupport {
    private String bind_addr = System.getProperty("jgroups.bind_addr");
    private String mcast_addr = System.getProperty("jgroups.udp.mcast_addr");
    private String mcast_port = System.getProperty("jgroups.udp.mcast_port");

    public void setUpProperties() throws UnknownHostException {
        System.setProperty("jgroups.bind_addr",
                System.getProperty("jbosstest.cluster.node1", InetAddress.getLocalHost().getHostAddress()));
        String udpGroup = System.getProperty("jbosstest.udpGroup", "233.54.54.54");
        if (udpGroup.trim().length() == 0)
            udpGroup = "233.54.54.54";
        System.setProperty("jgroups.udp.mcast_addr", udpGroup);
        System.setProperty("jgroups.udp.mcast_port", String.valueOf(54545));
    }

    public void restoreProperties() {
        if (bind_addr == null)
            System.clearProperty("jgroups.bind_addr");
        else
            System.setProperty("jgroups.bind_addr", bind_addr);
        if (mcast_addr == null)
            System.clearProperty("jgroups.udp.mcast_addr");
        else
            System.setProperty("jgroups.udp.mcast_addr", mcast_addr);
        if (mcast_port == null)
            System.clearProperty("jgroups.udp.mcast_port");
        else
            System.setProperty("jgroups.udp.mcast_port", mcast_port);
    }
}
