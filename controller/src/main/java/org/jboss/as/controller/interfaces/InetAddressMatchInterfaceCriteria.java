/**
 *
 */
package org.jboss.as.controller.interfaces;

import static org.jboss.as.controller.ControllerLogger.SERVER_LOGGER;
import static org.jboss.as.controller.ControllerMessages.MESSAGES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ANY_ADDRESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ANY_IPV4_ADDRESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ANY_IPV6_ADDRESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INET_ADDRESS;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jboss.as.controller.ControllerLogger;
import org.jboss.dmr.ModelNode;

/**
 * {@link InterfaceCriteria} that tests whether a given address is matches
 * the specified address.
 *
 * @author Brian Stansberry
 */
public class InetAddressMatchInterfaceCriteria extends AbstractInterfaceCriteria {

    private static final long serialVersionUID = 149404752878332750L;

    private ModelNode address;
    private String addressString;
    private InetAddress resolved;
    private boolean unknownHostLogged;
    private boolean anyLocalLogged;

    public InetAddressMatchInterfaceCriteria(final InetAddress address) {
        if (address == null)
            throw MESSAGES.nullVar("address");
        this.resolved = address;
        this.address = new ModelNode(resolved.getHostAddress());
    }

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
            throw MESSAGES.nullVar("address");
        this.address = address;
    }

    /**
     * Creates a new InetAddressMatchInterfaceCriteria
     *
     * @param address a valid String value to pass to {@link InetAddress#getByName(String)}
     *                Cannot be {@code null}
     *
     * @throws IllegalArgumentException if <code>network</code> is <code>null</code>
     */
    public InetAddressMatchInterfaceCriteria(final String address) {
        if (address == null || address.isEmpty() || address.trim().isEmpty()) {
            throw MESSAGES.nullVar("address");
        }
        this.addressString = address;
    }

    public synchronized InetAddress getAddress() throws UnknownHostException {
        if (resolved == null) {
            if (address != null) {
                resolved = InetAddress.getByName(address.resolve().asString());
            } else {
                resolved = InetAddress.getByName(addressString);
            }
        }
        return this.resolved;
    }

    @Override
    public Map<NetworkInterface, Set<InetAddress>> getAcceptableAddresses(Map<NetworkInterface, Set<InetAddress>> candidates) throws SocketException {
        Map<NetworkInterface, Set<InetAddress>> result = super.getAcceptableAddresses(candidates);

        // AS7-4509 Validate we only have a single match
        if (result.size() > 1 || (result.size() == 1 && result.values().iterator().next().size() > 1)) {
            logMultipleValidInterfaces(result);
            result = Collections.emptyMap();
        }
        return result;
    }

    /**
     * {@inheritDoc}
     *
     * @return <code>getAddress()</code> if the <code>address</code> is the same as the one returned by {@link #getAddress()}.
     */
    @Override
    protected InetAddress isAcceptable(NetworkInterface networkInterface, InetAddress address) throws SocketException {

        try {
            InetAddress toMatch = getAddress();
            // One time only warn against use of wildcard addresses
            if (!anyLocalLogged && toMatch.isAnyLocalAddress()) {
                SERVER_LOGGER.invalidWildcardAddress(this.address, INET_ADDRESS, ANY_ADDRESS, ANY_IPV4_ADDRESS, ANY_IPV6_ADDRESS);
                anyLocalLogged = true;
            }


            if( toMatch.equals(address) ) {
                if (toMatch instanceof Inet6Address) {
                    return matchIPv6((Inet6Address) toMatch, (Inet6Address) address);
                }
                return toMatch;
            }
        } catch (UnknownHostException e) {
            // One time only log a warning
            if (!unknownHostLogged) {
                SERVER_LOGGER.cannotResolveAddress(this.address);
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

    private static InetAddress matchIPv6(Inet6Address toMatch, Inet6Address address) {
        // No specified scope always matches; specified scope must match
        return (toMatch.getScopeId() == 0 || toMatch.getScopeId() == address.getScopeId()) ? address : null;
    }

    @Override
    public int hashCode() {
        if (address != null) {
            return address.hashCode();
        } else {
            return addressString.hashCode();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof InetAddressMatchInterfaceCriteria) {
            if (address != null) {
                return address.equals(((InetAddressMatchInterfaceCriteria)o).address);
            } else if (addressString != null) {
                return addressString.equals(((InetAddressMatchInterfaceCriteria)o).addressString);
            }
        }
        return false;
    }

    private void logMultipleValidInterfaces(Map<NetworkInterface, Set<InetAddress>> matches) {
        Set<String> nis = new HashSet<String>();
        Set<InetAddress> addresses = new HashSet<InetAddress>();
        for (Map.Entry<NetworkInterface, Set<InetAddress>> entry : matches.entrySet()) {
            nis.add(entry.getKey().getName());
            addresses.addAll(entry.getValue());
        }
        String toMatch = resolved != null ? resolved.getHostAddress() : addressString;


        ControllerLogger.ROOT_LOGGER.multipleMatchingAddresses(toMatch, addresses, nis);
    }
}
