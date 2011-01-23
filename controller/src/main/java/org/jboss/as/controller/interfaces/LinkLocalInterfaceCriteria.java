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
public class LinkLocalInterfaceCriteria implements InterfaceCriteria {

    private static final long serialVersionUID = 353271734087683239L;

    public static final LinkLocalInterfaceCriteria INSTANCE = new LinkLocalInterfaceCriteria();

    private LinkLocalInterfaceCriteria() {}

    /**
     * {@inheritDoc}
     *
     * @return <code>true</code> if <code>address</code> is
     *         {@link InetAddress#isLinkLocalAddress() link-local}.
     */
    @Override
    public boolean isAcceptable(NetworkInterface networkInterface, InetAddress address) throws SocketException {

        return address.isLinkLocalAddress();
    }

    private Object readResolve() throws ObjectStreamException {
        return INSTANCE;
    }

}
