/**
 * 
 */
package org.jboss.as.model.socket;

import java.io.ObjectStreamException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;

/**
 * {@link InterfaceCriteria} that tests whether a given address is not
 * {@link InetAddress#isSiteLocalAddress() site-local},
 * {@link InetAddress#isLinkLocalAddress() link-local}
 * or a {@link InetAddress#isAnyLocalAddress() wildcard address}.
 * 
 * @author Brian Stansberry
 */
public class PublicAddressInterfaceCriteria implements InterfaceCriteria {

    private static final long serialVersionUID = 8151472860427215473L;
    
    public static final PublicAddressInterfaceCriteria INSTANCE = new PublicAddressInterfaceCriteria();
    
    private PublicAddressInterfaceCriteria() {}
    
    /**
     * {@inheritDoc}
     * 
     * @return <code>true</code> if <code>address</code> is not
     *         {@link InetAddress#isSiteLocalAddress() site-local}, 
     *         {@link InetAddress#isLinkLocalAddress() link-local}
     *         or a {@link InetAddress#isAnyLocalAddress() wildcard address}.
     */
    @Override
    public boolean isAcceptable(NetworkInterface networkInterface, InetAddress address) throws SocketException {
        
        return !address.isSiteLocalAddress() && !address.isLinkLocalAddress() && !address.isAnyLocalAddress();
    }
    
    private Object readResolve() throws ObjectStreamException {
        return INSTANCE;
    }

}
