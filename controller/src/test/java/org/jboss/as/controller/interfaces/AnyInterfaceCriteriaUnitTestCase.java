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

import static org.jboss.as.controller.interfaces.InterfaceCriteriaTestUtil.allCandidates;
import static org.jboss.as.controller.interfaces.InterfaceCriteriaTestUtil.getRightTypeAddresses;
import static org.jboss.as.controller.interfaces.InterfaceCriteriaTestUtil.loopbackInterfaces;
import static org.jboss.as.controller.interfaces.InterfaceCriteriaTestUtil.nonLoopBackInterfaces;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

/**
 * Unit tests of {@link AnyInterfaceCriteria}.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class AnyInterfaceCriteriaUnitTestCase {

    @Test
    public void testMultipleCriteria() throws Exception {

        if (nonLoopBackInterfaces.size() < 1 || loopbackInterfaces.size() < 1) {
            return;
        }

        Map<NetworkInterface, Set<InetAddress>> correct = new HashMap<NetworkInterface, Set<InetAddress>>();
        for (NetworkInterface ni : loopbackInterfaces) {
            correct.put(ni, getRightTypeAddresses(allCandidates.get(ni)));
        }
        String target = null;
        for (NetworkInterface ni : nonLoopBackInterfaces) {
            Set<InetAddress> addresses = getRightTypeAddresses(allCandidates.get(ni));
            if (addresses.size() > 0) {
                correct.put(ni, addresses);
                target = ni.getName();
                break;
            }
        }

        if (target == null) {
            return;
        }

        Set<InterfaceCriteria> criterias = new HashSet<InterfaceCriteria>();
        criterias.add(new NicInterfaceCriteria(target));
        criterias.add(LoopbackInterfaceCriteria.INSTANCE);
        AnyInterfaceCriteria testee = new AnyInterfaceCriteria(criterias);
        Map<NetworkInterface, Set<InetAddress>> result = testee.getAcceptableAddresses(allCandidates);
        assertNotNull(result);
        assertEquals(correct, result);
    }

    @Test
    public void testNoMatch() throws Exception {

        if (allCandidates.size() < 1) {
            return;
        }

        if (allCandidates.containsKey("bogus")) {
            // LOL  Oh well; we won't run this test on this machine :-D
            return;
        }

        AnyInterfaceCriteria testee = new AnyInterfaceCriteria(Collections.singleton((InterfaceCriteria) new NicInterfaceCriteria("bogus")));
        Map<NetworkInterface, Set<InetAddress>> result = testee.getAcceptableAddresses(allCandidates);
        assertNotNull(result);
        assertEquals(0, result.size());
    }

}
