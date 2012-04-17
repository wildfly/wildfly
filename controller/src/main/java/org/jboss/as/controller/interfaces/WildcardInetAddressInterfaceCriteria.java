package org.jboss.as.controller.interfaces;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * {@link InterfaceCriteria} Placeholder interface criteria; enables support of wildcard addresses for inet-address.
 *
 * @author Mike Dobozy (mike.dobozy@amentra.com)
 *
 */
public class WildcardInetAddressInterfaceCriteria implements InterfaceCriteria {

    private static final long serialVersionUID = -4805776607639567774L;

    private Version version = Version.ANY;

    public enum Version {
        V4, V6, ANY
    }

    public WildcardInetAddressInterfaceCriteria(InetAddress address) {
        if (address instanceof Inet4Address) {
            version = Version.V4;
        }
        else if (address instanceof Inet6Address) {
            version = Version.V6;
        }
    }

    public Version getVersion() {
        return version;
    }

    @Override
    public Map<NetworkInterface, Set<InetAddress>> getAcceptableAddresses(Map<NetworkInterface, Set<InetAddress>> candidates) throws SocketException {
        return Collections.emptyMap();
    }

    @Override
    public int hashCode() {
        return version.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof WildcardInetAddressInterfaceCriteria)
                && version == ((WildcardInetAddressInterfaceCriteria)o).version;
    }
}
