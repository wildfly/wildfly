/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.controller.interfaces;

import static org.jboss.as.controller.interfaces.InterfaceCriteriaTestUtil.*;
import static org.junit.Assert.*;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

/**
 * Unit test of {@link LoopbackAddressInterfaceCriteria}.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class LoopbackAddressInterfaceCriteriaUnitTestCase {

    @Test
    public void testBasic() throws Exception {

        if (allCandidates.size() < 1) {
            return;
        }

        InetAddress target = InetAddress.getByName("127.0.0.2");
        LoopbackAddressInterfaceCriteria testee = new LoopbackAddressInterfaceCriteria(target);
        Map<NetworkInterface, Set<InetAddress>> result = testee.getAcceptableAddresses(allCandidates);
        assertEquals(loopbackInterfaces.size(), result.size());
        if (result.size() > 0) {
            for (Set<InetAddress> set : result.values()) {
                assertEquals(1, set.size());
                assertTrue(set.contains(target));
            }
        }
    }

    @Test
    public void testNoLoopback() throws Exception {

        if (nonLoopBackInterfaces.size() < 1) {
            return;
        }

        InetAddress target = InetAddress.getByName("127.0.0.2");
        LoopbackAddressInterfaceCriteria testee = new LoopbackAddressInterfaceCriteria(target);
        Map<NetworkInterface, Set<InetAddress>> candidates = new HashMap<NetworkInterface, Set<InetAddress>>();
        for (NetworkInterface ni : nonLoopBackInterfaces) {
            candidates.put(ni, allCandidates.get(ni));
        }
        Map<NetworkInterface, Set<InetAddress>> result = testee.getAcceptableAddresses(candidates);
        assertEquals(0, result.size());
    }
}
