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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

/**
 * Unit test of {@link NicInterfaceCriteria}.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class NicInterfaceCriteriaUnitTestCase {

    @Test
    public void testBasic() throws Exception {

        if (allCandidates.size() < 1) {
            return;
        }

        for (Map.Entry<NetworkInterface, Set<InetAddress>> entry : allCandidates.entrySet()) {
            NetworkInterface nic = entry.getKey();
            String target = nic.getName();
            NicInterfaceCriteria testee = new NicInterfaceCriteria(target);
            Map<NetworkInterface, Set<InetAddress>> result = testee.getAcceptableAddresses(allCandidates);
            assertEquals(1, result.size());
            Set<InetAddress> addresses = result.get(nic);
            assertNotNull(addresses);
            Set<InetAddress> rightType = getRightTypeAddresses(entry.getValue());
            assertEquals(rightType, addresses);
            assertTrue(addresses.containsAll(rightType));
        }
    }

    @Test
    public void testBogus() throws Exception {

        if (allCandidates.size() < 1) {
            return;
        }

        if (allCandidates.containsKey("bogus")) {
            // LOL  Oh well; we won't run this test on this machine :-D
            return;
        }

        NicInterfaceCriteria testee = new NicInterfaceCriteria("bogus");
        Map<NetworkInterface, Set<InetAddress>> result = testee.getAcceptableAddresses(allCandidates);
        assertEquals(0, result.size());
    }
}
