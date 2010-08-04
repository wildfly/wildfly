/**
 * 
 */
package org.jboss.as.model.socket;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;

/**
 * {@link InterfaceCriteria} that tests whether a given interface
 * {@link NetworkInterface#supportsMulticast() supports multicast}
 * 
 * @author Brian Stansberry
 */
public class SupportsMulticastInterfaceCriteria implements InterfaceCriteria {

    private static final long serialVersionUID = 2594955197396893923L;
    
    public static final SupportsMulticastInterfaceCriteria INSTANCE = new SupportsMulticastInterfaceCriteria();
    
    private SupportsMulticastInterfaceCriteria() {}
    
    /**
     * {@inheritDoc}
     * 
     * @return <code>true</code> if <code>networkInterface</code> 
     *         {@link NetworkInterface#supportsMulticast() supports multicast}.
     */
    @Override
    public boolean isAcceptable(NetworkInterface networkInterface, InetAddress address) throws SocketException {
        
        return networkInterface.supportsMulticast();
    }

    
}
