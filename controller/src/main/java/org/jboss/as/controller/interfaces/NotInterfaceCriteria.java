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

/**
 *
 */
package org.jboss.as.controller.interfaces;

import static org.jboss.as.controller.ControllerMessages.MESSAGES;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * {@link InterfaceCriteria} that tests whether a given network interface and
 * address satisfy <i>none</i> of a contained set of {@link InterfaceCriteria}.
 *
 * @author Brian Stansberry
 */
public class NotInterfaceCriteria implements InterfaceCriteria {

    private static final long serialVersionUID = -2037624198837453203L;

    private final Set<InterfaceCriteria> criteria = new HashSet<InterfaceCriteria>();

    /**
     * Creates a new NotInterfaceCriteria
     *
     * @param criteria the criteria to check to see if none are satisfied.
     *                 Cannot be <code>null</code>
     *
     * @throws IllegalArgumentException if <code>criteria</code> is <code>null</code>
     */
    public NotInterfaceCriteria(Set<InterfaceCriteria> criteria) {
        if (criteria == null)
            throw MESSAGES.nullVar("criteria");
        this.criteria.addAll(criteria);
    }

    @Override
    public Map<NetworkInterface, Set<InetAddress>> getAcceptableAddresses(final Map<NetworkInterface, Set<InetAddress>> candidates) throws SocketException {
        Map<NetworkInterface, Set<InetAddress>> testee = AbstractInterfaceCriteria.cloneCandidates(candidates);
        for (InterfaceCriteria ic : criteria) {
            testee = removeMatches(testee, ic.getAcceptableAddresses(AbstractInterfaceCriteria.cloneCandidates(testee)));
            if (testee.size() == 0) {
                break;
            }
        }
        return testee;
    }

    Set<InterfaceCriteria> getAllCriteria(){
        return criteria;
    }

    @Override
    public int hashCode() {
        return criteria.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof NotInterfaceCriteria == false) {
            return false;
        }
        return criteria.equals(((NotInterfaceCriteria)o).criteria);
    }

    private Map<NetworkInterface, Set<InetAddress>> removeMatches(Map<NetworkInterface, Set<InetAddress>> candidates,
                                                                  Map<NetworkInterface, Set<InetAddress>> toRemove) {

        Map<NetworkInterface, Set<InetAddress>> result = new HashMap<NetworkInterface, Set<InetAddress>>();
        for (Map.Entry<NetworkInterface, Set<InetAddress>> entry : candidates.entrySet()) {
            Set<InetAddress> retained = new HashSet<InetAddress>(entry.getValue());
            Set<InetAddress> badAddresses = toRemove.get(entry.getKey());
            if (badAddresses != null && badAddresses.size() > 0) {
                retained.removeAll(badAddresses);
                if (retained.size() > 0) {
                    result.put(entry.getKey(), retained);
                }
            } else {
                result.put(entry.getKey(), retained);
            }
        }

        return result;
    }

}
