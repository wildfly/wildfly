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
 * {@link NetworkInterface#isVirtual() virtual}.
 *
 * @author Brian Stansberry
 */
public class VirtualInterfaceCriteria extends AbstractInterfaceCriteria {

    private static final long serialVersionUID = -2714634628678015738L;

    public static final VirtualInterfaceCriteria INSTANCE = new VirtualInterfaceCriteria();

    private VirtualInterfaceCriteria() {}

    /**
     * {@inheritDoc}
     *
     * @return <code>address</code> if <code>networkInterface</code> is
     *         {@link NetworkInterface#isVirtual() virtual}
     */
    @Override
    protected InetAddress isAcceptable(NetworkInterface networkInterface, InetAddress address) throws SocketException {

        if (networkInterface.isVirtual())
            return address;
        return null;
    }

    private Object readResolve() throws ObjectStreamException {
        return INSTANCE;
    }
}
