/**
 *
 */
package org.jboss.as.controller.interfaces;

import static org.jboss.as.controller.ControllerMessages.MESSAGES;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.HashSet;
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

    /**
     * {@inheritDoc}
     *
     * @return the first criteria {@link #isAcceptable(java.net.NetworkInterface, java.net.InetAddress)}
     * result that is non-null, or null if no criteria apply
     */
    @Override
    public InetAddress isAcceptable(NetworkInterface networkInterface, InetAddress address) throws SocketException {

        for (InterfaceCriteria ic : criteria) {
            InetAddress bindAddress = ic.isAcceptable(networkInterface, address);
            if (bindAddress != null)
                return bindAddress;
        }
        return null;
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

}
