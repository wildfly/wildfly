/**
 *
 */
package org.jboss.as.controller.interfaces;

import java.io.ObjectStreamException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;

/**
 * {@link InterfaceCriteria} that tests whether a given address is a
 * {@link InetAddress#isLinkLocalAddress() link-local} address.
 *
 * @author Brian Stansberry
 */
public class LinkLocalInterfaceCriteria extends AbstractInterfaceCriteria {

    private static final long serialVersionUID = 353271734087683239L;

    public static final LinkLocalInterfaceCriteria INSTANCE = new LinkLocalInterfaceCriteria();

    private LinkLocalInterfaceCriteria() {}

    /**
     * {@inheritDoc}
     *
     * @return <code>address</code> if <code>address</code> is
     *         {@link InetAddress#isLinkLocalAddress() link-local}.
     */
    @Override
    protected InetAddress isAcceptable(NetworkInterface networkInterface, InetAddress address) throws SocketException {

        if( address.isLinkLocalAddress() )
            return address;
        return null;
    }

    private Object readResolve() throws ObjectStreamException {
        return INSTANCE;
    }
}
