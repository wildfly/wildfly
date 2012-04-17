/**
 *
 */
package org.jboss.as.controller.interfaces;

import java.io.ObjectStreamException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;

/**
 * {@link InterfaceCriteria} that tests whether a given interface is a
 * {@link NetworkInterface#isLoopback() loopback interface}
 *
 * @author Brian Stansberry
 */
public class LoopbackInterfaceCriteria extends AbstractInterfaceCriteria {

    private static final long serialVersionUID = 1922501758657303593L;

    public static final LoopbackInterfaceCriteria INSTANCE = new LoopbackInterfaceCriteria();

    private LoopbackInterfaceCriteria() {}

    /**
     * {@inheritDoc}
     *
     * @return <code>address</code> if {@link NetworkInterface#isLoopback()} is true, null otherwise.
     */
    @Override
    protected InetAddress isAcceptable(NetworkInterface networkInterface, InetAddress address) throws SocketException {

        if( networkInterface.isLoopback() )
            return address;
        return null;
    }

    private Object readResolve() throws ObjectStreamException {
        return INSTANCE;
    }
}
