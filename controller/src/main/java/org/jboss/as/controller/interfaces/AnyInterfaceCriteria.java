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
 * address satisfy <i>any</i> of a contained set of {@link InterfaceCriteria}.
 *
 * @author Brian Stansberry
 */
public class AnyInterfaceCriteria implements InterfaceCriteria {

    private static final long serialVersionUID = 3384500068401101329L;

    private final Set<InterfaceCriteria> criteria = new HashSet<InterfaceCriteria>();

    /**
     * Creates a new AnyInterfaceCriteria
     *
     * @param criteria the criteria to check to see if any are satisfied.
     *                 Cannot be <code>null</code>
     *
     * @throws IllegalArgumentException if <code>criteria</code> is <code>null</code>
     */
    public AnyInterfaceCriteria(Set<InterfaceCriteria> criteria) {
        if (criteria == null)
            throw MESSAGES.nullVar("criteria");
        this.criteria.addAll(criteria);
    }

    @Override
    public Map<NetworkInterface, Set<InetAddress>> getAcceptableAddresses(Map<NetworkInterface, Set<InetAddress>> candidates) throws SocketException {
        final Map<NetworkInterface, Set<InetAddress>> result = new HashMap<NetworkInterface, Set<InetAddress>>();
        for (InterfaceCriteria ic : criteria) {
            final Map<NetworkInterface, Set<InetAddress>> testee = AbstractInterfaceCriteria.cloneCandidates(candidates);
            final Map<NetworkInterface, Set<InetAddress>> accepted = ic.getAcceptableAddresses(testee);
            addAccepted(accepted, result);
        }

        return result;
    }

    @Override
    public int hashCode() {
        return criteria.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof AnyInterfaceCriteria == false) {
            return false;
        }
        return criteria.equals(((AnyInterfaceCriteria)o).criteria);
    }

    private void addAccepted(Map<NetworkInterface, Set<InetAddress>> accepted, Map<NetworkInterface, Set<InetAddress>> result) {
        for (Map.Entry<NetworkInterface, Set<InetAddress>> entry : accepted.entrySet()) {
            Set<InetAddress> addresses = result.get(entry.getKey());
            if (addresses == null) {
                result.put(entry.getKey(), new HashSet<InetAddress>(entry.getValue()));
            } else {
                addresses.addAll(entry.getValue());
            }
        }
    }
}
