/**
 *
 */
package org.jboss.as.controller.interfaces;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;

/**
 * {@link InterfaceCriteria} that tests whether a given name matches the
 * network interface's {@link NetworkInterface#getName() name}.
 *
 * @author Brian Stansberry
 */
public class NicInterfaceCriteria implements InterfaceCriteria {

    private static final long serialVersionUID = 6905500001319165842L;

    private final String name;

    /**
     * Creates a new AnyInterfaceCriteria
     *
     * @param name the criteria to check to see if any are satisfied.
     *                 Cannot be <code>null</code>
     *
     * @throws IllegalArgumentException if <code>criteria</code> is <code>null</code>
     */
    public NicInterfaceCriteria(String name) {
        if (name == null)
            throw new IllegalArgumentException("name is null");
        this.name = name;
    }

    public String getAcceptableName() {
        return name;
    }

    /**
     * {@inheritDoc}
     *
     * @return <code>address</code> if the {@link #getAcceptableName() acceptable name}
     *          equals <code>networkInterface</code>'s {@link NetworkInterface#getName() name}.
     */
    @Override
    public InetAddress isAcceptable(NetworkInterface networkInterface, InetAddress address) throws SocketException {

        if( name.equals(networkInterface.getName()) )
            return address;
        return null;
    }



}
