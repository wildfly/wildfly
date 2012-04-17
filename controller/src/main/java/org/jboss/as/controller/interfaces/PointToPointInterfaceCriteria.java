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
 * {@link NetworkInterface#isPointToPoint() point-to-point interface}.
 *
 * @author Brian Stansberry
 */
public class PointToPointInterfaceCriteria extends AbstractInterfaceCriteria {

    private static final long serialVersionUID = -3434237413172720854L;

    public static final PointToPointInterfaceCriteria INSTANCE = new PointToPointInterfaceCriteria();

    private PointToPointInterfaceCriteria() {}

    /**
     * {@inheritDoc}
     *
     * @return <code>address</code> if <code>networkInterface</code> is a
     *         {@link NetworkInterface#isPointToPoint() point-to-point interface}.
     */
    @Override
    protected InetAddress isAcceptable(NetworkInterface networkInterface, InetAddress address) throws SocketException {

        if( networkInterface.isPointToPoint() )
            return address;
        return null;
    }

    private Object readResolve() throws ObjectStreamException {
        return INSTANCE;
    }
}
