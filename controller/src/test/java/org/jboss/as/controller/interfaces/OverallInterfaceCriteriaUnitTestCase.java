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

package org.jboss.as.controller.interfaces;

import static org.jboss.as.controller.interfaces.InterfaceCriteriaTestUtil.allCandidates;
import static org.jboss.as.controller.interfaces.InterfaceCriteriaTestUtil.allInterfaces;
import static org.jboss.as.controller.interfaces.InterfaceCriteriaTestUtil.getRightTypeAddresses;
import static org.jboss.as.controller.interfaces.InterfaceCriteriaTestUtil.loopbackInterfaces;
import static org.jboss.as.controller.interfaces.InterfaceCriteriaTestUtil.nonLoopBackInterfaces;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

/**
 * Unit tests of {@link OverallInterfaceCriteria}
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class OverallInterfaceCriteriaUnitTestCase {

    @Test
    public void testBasic() throws Exception {

        if (allCandidates.size() < 1) {
            return;
        }

        Map.Entry<NetworkInterface, Set<InetAddress>> correct = allCandidates.entrySet().iterator().next();

        InterfaceCriteria criteria = new NicInterfaceCriteria(correct.getKey().getName());
        OverallInterfaceCriteria testee = new OverallInterfaceCriteria("test", Collections.singleton(criteria));
        Map<NetworkInterface, Set<InetAddress>> result = testee.getAcceptableAddresses(allCandidates);
        assertNotNull(result);
        assertEquals(1, result.size());

        Map.Entry<NetworkInterface, Set<InetAddress>> entry = result.entrySet().iterator().next();
        assertEquals(correct.getKey(), entry.getKey());
        assertEquals(1, entry.getValue().size());
        Set<InetAddress> set = correct.getValue();
        assertTrue(set.contains(entry.getValue().iterator().next()));
    }

    @Test
    public void testMultipleCriteria() throws Exception {

        if (nonLoopBackInterfaces.size() < 1 || loopbackInterfaces.size() < 1) {
            return;
        }

        Map<NetworkInterface, Set<InetAddress>> correct = new HashMap<NetworkInterface, Set<InetAddress>>();
        for (NetworkInterface ni : loopbackInterfaces) {
            if (ni.isUp()) {
                correct.put(ni, getRightTypeAddresses(allCandidates.get(ni)));
            }
        }

        if (correct.size() == 0) {
            return;
        }

        Set<InterfaceCriteria> criterias = new HashSet<InterfaceCriteria>();
        criterias.add(UpInterfaceCriteria.INSTANCE);
        criterias.add(LoopbackInterfaceCriteria.INSTANCE);
        OverallInterfaceCriteria testee = new OverallInterfaceCriteria("test", criterias);
        Map<NetworkInterface, Set<InetAddress>> result = testee.getAcceptableAddresses(allCandidates);
        assertNotNull(result);
        assertEquals(1, result.size());

        Map.Entry<NetworkInterface, Set<InetAddress>> entry = result.entrySet().iterator().next();
        assertEquals(1, entry.getValue().size());
        Set<InetAddress> set = correct.get(entry.getKey());
        assertNotNull(set);
        assertTrue(set.contains(entry.getValue().iterator().next()));
    }

    @Test
    public void testMultipleMatches() throws Exception {

        if (allCandidates.size() < 1) {
            return;
        }

        Map<NetworkInterface, Set<InetAddress>> correct = new HashMap<NetworkInterface, Set<InetAddress>>();
        for (NetworkInterface ni : allInterfaces) {
            if (ni.isUp()) {
                correct.put(ni, getRightTypeAddresses(allCandidates.get(ni)));
            }
        }

        if (correct.size() < 2) {
            return;
        }

        OverallInterfaceCriteria testee = new OverallInterfaceCriteria("test", Collections.singleton((InterfaceCriteria) UpInterfaceCriteria.INSTANCE));
        Map<NetworkInterface, Set<InetAddress>> result = testee.getAcceptableAddresses(allCandidates);
        assertNotNull(result);
        assertEquals(1, result.size());

        Map.Entry<NetworkInterface, Set<InetAddress>> entry = result.entrySet().iterator().next();
        assertEquals(1, entry.getValue().size());
        Set<InetAddress> set = correct.get(entry.getKey());
        assertNotNull(set);
        assertTrue(set.contains(entry.getValue().iterator().next()));
    }

    @Test
    public void testNoMatch() throws Exception {

        if (loopbackInterfaces.size() < 1) {
            return;
        }

        if (allCandidates.containsKey("bogus")) {
            // LOL  Oh well; we won't run this test on this machine :-D
            return;
        }


        Set<InterfaceCriteria> criterias = new LinkedHashSet<InterfaceCriteria>();
        criterias.add(LoopbackInterfaceCriteria.INSTANCE);
        criterias.add(new NicInterfaceCriteria("bogus"));
        OverallInterfaceCriteria testee = new OverallInterfaceCriteria("test", criterias);
        Map<NetworkInterface, Set<InetAddress>> result = testee.getAcceptableAddresses(allCandidates);
        assertNotNull(result);
        assertEquals(0, result.size());
    }
}
