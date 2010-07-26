/**
 * 
 */
package org.jboss.as.model.socket;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;

/**
 * A criteria used to determine what IP address to use for an interface.
 * 
 * @author Brian Stansberry
 */
public interface InterfaceCriteria {

    /**
     * Gets whether the given network interface and address are acceptable for 
     * use.
     * 
     * @param networkInterface the network interface. Cannot be <code>null</code>
     * @param address an address that is associated with <code>networkInterface</code>. Cannot be <code>null</code>
     * @return <code>true</code> if the given interface/address meets this object's
     *         criteria
     *         
     * @throws SocketException
     */
    boolean isAcceptable(NetworkInterface networkInterface, InetAddress address) throws SocketException;
}
