/**
 *
 */
package org.jboss.as.controller.interfaces;

import java.io.ObjectStreamException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;

/**
 * {@link InterfaceCriteria} that tests whether a given interface
 * {@link NetworkInterface#supportsMulticast() supports multicast}
 *
 * @author Brian Stansberry
 */
public class SupportsMulticastInterfaceCriteria extends AbstractInterfaceCriteria {

    private static final long serialVersionUID = 2594955197396893923L;

    public static final SupportsMulticastInterfaceCriteria INSTANCE = new SupportsMulticastInterfaceCriteria();

    private SupportsMulticastInterfaceCriteria() {}

    /**
     * {@inheritDoc}
     *
     * @return <code>address</code> if <code>networkInterface</code>
     *         {@link NetworkInterface#supportsMulticast() supports multicast}.
     */
    @Override
    protected InetAddress isAcceptable(NetworkInterface networkInterface, InetAddress address) throws SocketException {

        if( networkInterface.supportsMulticast() )
            return address;
        return null;
    }

    private Object readResolve() throws ObjectStreamException {
        return INSTANCE;
    }
}
