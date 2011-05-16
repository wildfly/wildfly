/**
 *
 */
package org.jboss.as.controller.interfaces;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ANY_ADDRESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ANY_IPV4_ADDRESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ANY_IPV6_ADDRESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INET_ADDRESS;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;

import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;

/**
 * {@link InterfaceCriteria} that tests whether a given address is matches
 * the specified address.
 *
 * @author Brian Stansberry
 */
public class InetAddressMatchInterfaceCriteria implements InterfaceCriteria {

    private static final Logger log = Logger.getLogger("org.jboss.as.server");

    private static final long serialVersionUID = 149404752878332750L;

    private ModelNode address;
    private InetAddress resolved;
    private boolean unknownHostLogged;
    private boolean anyLocalLogged;

    /**
     * Creates a new InetAddressMatchInterfaceCriteria
     *
     * @param address a valid value to pass to {@link InetAddress#getByName(String)}
     *                Cannot be {@code null}
     *
     * @throws IllegalArgumentException if <code>network</code> is <code>null</code>
     */
    public InetAddressMatchInterfaceCriteria(final ModelNode address) {
        if (address == null)
            throw new IllegalArgumentException("address is null");
        this.address = address;
    }

    public synchronized InetAddress getAddress() throws UnknownHostException {
        if (resolved == null) {
            resolved = InetAddress.getByName(address.resolve().asString());
        }
        return this.resolved;
    }

    /**
     * {@inheritDoc}
     *
     * @return <code>getAddress()</code> if the <code>address</code> is the same as the one returned by {@link #getAddress()}.
     */
    @Override
    public InetAddress isAcceptable(NetworkInterface networkInterface, InetAddress address) throws SocketException {

        try {
            InetAddress toMatch = getAddress();
            // One time only warn against use of wildcard addresses
            if (!anyLocalLogged && toMatch.isAnyLocalAddress()) {
                log.warnf("Address %1$s is a wildcard address, which will not match " +
                    "against any specific address. Do not use the '%2$s' configuration element" +
                    "to specify that an interface should use a wildcard address; " +
                    "use '%3$s', '%4$s', or '%5$s'", this.address,
                    INET_ADDRESS, ANY_ADDRESS, ANY_IPV4_ADDRESS, ANY_IPV6_ADDRESS);
                anyLocalLogged = true;
            }

            if( getAddress().equals(address) )
                return getAddress();
        } catch (UnknownHostException e) {
            // One time only log a warning
            if (!unknownHostLogged) {
                log.warnf("Cannot resolve address %s, so cannot match it to any InetAddress", this.address);
                unknownHostLogged = true;
            }
            return null;
        }
        return null;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("InetAddressMatchInterfaceCriteria(");
        sb.append("address=");
        sb.append(address);
        sb.append(",resolved=");
        sb.append(resolved);
        sb.append(')');
        return sb.toString();
    }

}
