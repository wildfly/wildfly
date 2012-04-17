/**
 *
 */
package org.jboss.as.controller.interfaces;

import java.io.ObjectStreamException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;

/**
 * {@link InterfaceCriteria} that tests whether a given interface is
 * {@link NetworkInterface#isUp() up}.
 *
 * @author Brian Stansberry
 */
public class UpInterfaceCriteria extends AbstractInterfaceCriteria {

    private static final long serialVersionUID = -5298203789711808552L;

    public static final UpInterfaceCriteria INSTANCE = new UpInterfaceCriteria();

    private UpInterfaceCriteria() {}

    /**
     * {@inheritDoc}
     *
     * @return <code>address</code> if <code>networkInterface</code> is
     *         {@link NetworkInterface#isUp() up}
     */
    @Override
    protected InetAddress isAcceptable(NetworkInterface networkInterface, InetAddress address) throws SocketException {

        if( networkInterface.isUp() )
            return address;
        return null;
    }

    private Object readResolve() throws ObjectStreamException {
        return INSTANCE;
    }
}
