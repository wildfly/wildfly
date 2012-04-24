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
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

/**
 * Unit tests of {@link InetAddressMatchInterfaceCriteria}
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class InetAddressMatchInterfaceCriteriaTestCase {

    @Test
    public void testBasicMatch() throws Exception {
        if (allCandidates.size() < 1) {
            return;
        }

        for (Map.Entry<NetworkInterface, Set<InetAddress>> entry : allCandidates.entrySet()) {
            InetAddress target = entry.getValue().iterator().next();
            InterfaceCriteria criteria = new InetAddressMatchInterfaceCriteria(target);
            Map<NetworkInterface, Set<InetAddress>> accepted = criteria.getAcceptableAddresses(allCandidates);
            assertNotNull(accepted);
            accepted = OverallInterfaceCriteria.pruneAliasDuplicates(accepted);
            assertEquals(1, accepted.size());
            Set<InetAddress> set = accepted.get(entry.getKey());
            assertNotNull(set);
            assertEquals(1, set.size());
            assertTrue(set.contains(target));
        }
    }

    @Test
    public void testAmbiguousScopeId() throws Exception {

        if (allInterfaces.size() < 2) {
            return;
        }

        Map<NetworkInterface, Set<InetAddress>> candidates = new HashMap<NetworkInterface, Set<InetAddress>>();
        int i = 1;
        for (Iterator<NetworkInterface> iter = allInterfaces.iterator(); iter.hasNext(); i++) {
            Set<InetAddress> set = Collections.unmodifiableSet(Collections.singleton(InetAddress.getByName("::1%" + i)));
            candidates.put(iter.next(), set);
        }

        String ambiguous = "::1";
        InterfaceCriteria criteria = new InetAddressMatchInterfaceCriteria(ambiguous);
        Map<NetworkInterface, Set<InetAddress>> accepted = criteria.getAcceptableAddresses(candidates);
        assertNotNull(accepted);
        assertEquals(0, accepted.size());
    }
}
