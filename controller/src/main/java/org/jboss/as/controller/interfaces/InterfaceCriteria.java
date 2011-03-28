/**
 *
 */
package org.jboss.as.controller.interfaces;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;

/**
 * A criteria used to determine what IP address to use for an interface.
 *
 * @author Brian Stansberry
 */
public interface InterfaceCriteria extends Serializable {

    /**
     * Gets whether the given network interface and address are acceptable for
     * use. Acceptance is indicated by returning the address which should be
     * used for binding against
     * the network interface, typically this is the input address. For those
     * criteria which override the bind address, the overriden address should
     * be returned.
     *
     * @param networkInterface the network interface. Cannot be <code>null</code>
     * @param address an address that is associated with <code>networkInterface</code>.
     * Cannot be <code>null</code>
     * @return <code>InetAddress</code> the non-null address to bind to if the
     * criteria is met, null if the criteria does not apply
     *
     * @throws SocketException
     */

    InetAddress isAcceptable(NetworkInterface networkInterface, InetAddress address) throws SocketException;
}
