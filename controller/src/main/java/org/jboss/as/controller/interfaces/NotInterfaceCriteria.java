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
 * address satisfy <i>none</i> of a contained set of {@link InterfaceCriteria}.
 *
 * @author Brian Stansberry
 */
public class NotInterfaceCriteria implements InterfaceCriteria {

    private static final long serialVersionUID = -2037624198837453203L;

    private final Set<InterfaceCriteria> criteria = new HashSet<InterfaceCriteria>();

    /**
     * Creates a new AnyInterfaceCriteria
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

    /**
     * {@inheritDoc}
     *
     * @return <code>address</code> if <code>networkInterface</code and
     *         <code>address</code> satisfy <i>none</i> of a contained set of criteria.
     */
    @Override
    public InetAddress isAcceptable(NetworkInterface networkInterface, InetAddress address) throws SocketException {

        for (InterfaceCriteria ic : criteria) {
            if (ic.isAcceptable(networkInterface, address) != null)
                return null;
        }
        return address;
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

}
