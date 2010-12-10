/**
 *
 */
package org.jboss.as.model.socket;

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
            throw new IllegalArgumentException("criteria is null");
        this.criteria.addAll(criteria);
    }

    /**
     * {@inheritDoc}
     *
     * @return <code>true</code> if <code>networkInterface</code and
     *         <code>address</code> satisfy <i>any</i> of a contained set of criteria.
     */
    @Override
    public boolean isAcceptable(NetworkInterface networkInterface, InetAddress address) throws SocketException {

        for (InterfaceCriteria ic : criteria) {
            if (ic.isAcceptable(networkInterface, address))
                return true;
        }
        return false;
    }



}
