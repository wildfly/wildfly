package org.jboss.as.controller.interfaces;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;

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
    };

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

    /**
     * Always false, since this should be translated into any-address during parsing.
     */
    @Override
    public InetAddress isAcceptable(NetworkInterface networkInterface, InetAddress address) throws SocketException {
        return null;
    }

    @Override
    public int hashCode() {
        return version.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof WildcardInetAddressInterfaceCriteria == false) {
            return false;
        }
        return version == ((WildcardInetAddressInterfaceCriteria)o).version;
    }
}
