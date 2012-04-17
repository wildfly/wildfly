/**
 *
 */
package org.jboss.as.controller.interfaces;

import static org.jboss.as.controller.ControllerMessages.MESSAGES;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Arrays;

/**
 * {@link InterfaceCriteria} that tests whether a given address is on the
 * desired subnet.
 *
 * @author Brian Stansberry
 */
public class SubnetMatchInterfaceCriteria extends AbstractInterfaceCriteria {


    private static final long serialVersionUID = 149404752878332750L;

    private byte[] network;
    private int mask;

    /**
     * Creates a new SubnetMatchInterfaceCriteria
     *
     * @param network an InetAddress in byte[] form.
     *                 Cannot be <code>null</code>
     * @param mask the number of bytes in <code>network</code> that represent
     *             the network
     *
     * @throws IllegalArgumentException if <code>network</code> is <code>null</code>
     */
    public SubnetMatchInterfaceCriteria(byte[] network, int mask) {
        if (network == null)
            throw MESSAGES.nullVar("network");
        this.network = network;
        this.mask = mask;
    }

    /**
     * {@inheritDoc}
     *
     * @return <code>address</code> if the <code>address</code> is on the correct subnet.
     */
    @Override
    protected InetAddress isAcceptable(NetworkInterface networkInterface, InetAddress address) throws SocketException {

        byte[] addr = address.getAddress();
        if (addr.length != network.length) {
            // different address type
            return null;
        }
        int last = addr.length - mask;
        for (int i = 0; i < last; i++) {
            if (addr[i] != network[i]) {
                return null;
            }
        }
        return address;
    }

    @Override
    public int hashCode() {
        int i = 17;
        i = 31 * i + mask;
        i = 31 * i + Arrays.hashCode(network);
        return i;
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof SubnetMatchInterfaceCriteria)
                && Arrays.equals(network, ((SubnetMatchInterfaceCriteria)o).network)
                && mask == ((SubnetMatchInterfaceCriteria)o).mask;
    }
}
