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
public class SiteLocalInterfaceCriteria extends AbstractInterfaceCriteria {

    private static final long serialVersionUID = 8701772756289369451L;

    public static final SiteLocalInterfaceCriteria INSTANCE = new SiteLocalInterfaceCriteria();

    private SiteLocalInterfaceCriteria() {}

    /**
     * {@inheritDoc}
     *
     * @return <code>address</code> if <code>address</code> is
     *         {@link InetAddress#isLinkLocalAddress() link-local}.
     */
    @Override
    protected InetAddress isAcceptable(NetworkInterface networkInterface, InetAddress address) throws SocketException {

        if( address.isSiteLocalAddress() )
            return address;
        return null;
    }

    private Object readResolve() throws ObjectStreamException {
        return INSTANCE;
    }
}
