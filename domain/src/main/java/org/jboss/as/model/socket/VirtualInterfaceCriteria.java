/**
 * 
 */
package org.jboss.as.model.socket;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;

/**
 * {@link InterfaceCriteria} that tests whether a given interface is 
 * {@link NetworkInterface#isVirtual() virtual}.
 * 
 * @author Brian Stansberry
 */
public class VirtualInterfaceCriteria implements InterfaceCriteria {

    public static final VirtualInterfaceCriteria INSTANCE = new VirtualInterfaceCriteria();
    
    private VirtualInterfaceCriteria() {}
    
    /**
     * {@inheritDoc}
     * 
     * @return <code>true</code> if <code>networkInterface</code> is 
     *         {@link NetworkInterface#isVirtual() virtual}
     */
    @Override
    public boolean isAcceptable(NetworkInterface networkInterface, InetAddress address) throws SocketException {
        
        return networkInterface.isVirtual();
    }

}
