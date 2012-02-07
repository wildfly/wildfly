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

import java.net.UnknownHostException;

/**
 * Utility class that sets and clears JGroups-related system properties.
 *
 * @author Brian Stansberry
 */
public class JGroupsSystemPropertySupport {
    private String preferIPv4 = System.getProperty("java.net.preferIPv4Stack");
    private String bind_addr = System.getProperty("jgroups.bind_addr");
    private String mcast_addr = System.getProperty("jgroups.udp.mcast_addr");
    private String mcast_port = System.getProperty("jgroups.udp.mcast_port");

    public void setUpProperties() throws UnknownHostException {
        System.setProperty("java.net.preferIPv4Stack", "true");
        System.setProperty("jgroups.bind_addr","127.0.0.1");
        String udpGroup = System.getProperty("jbosstest.udpGroup", "233.54.54.54");
        if (udpGroup.trim().length() == 0)
            udpGroup = "233.54.54.54";
        System.setProperty("jgroups.udp.mcast_addr", udpGroup);
        System.setProperty("jgroups.udp.mcast_port", String.valueOf(54545));
    }

    public void restoreProperties() {
        this.reset(preferIPv4, "java.net.preferIPv4Stack");
        this.reset(bind_addr, "jgroups.bind_addr");
        this.reset(mcast_addr, "jgroups.mcast_addr");
        this.reset(mcast_port, "jgroups.mcast_port");
    }
        
    private void reset(String original, String property) {
        if (original != null) {
            System.setProperty(property, original);
        } else {
            System.clearProperty(property);
        }
    }
}
