/**
 * 
 */
package org.jboss.as.model.socket;

import java.io.ObjectStreamException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;

/**
 * {@link InterfaceCriteria} that tests whether a given interface is 
 * {@link NetworkInterface#isUp() up}.
 * 
 * @author Brian Stansberry
 */
public class UpInterfaceCriteria implements InterfaceCriteria {

    private static final long serialVersionUID = -5298203789711808552L;
    
    public static final UpInterfaceCriteria INSTANCE = new UpInterfaceCriteria();
    
    private UpInterfaceCriteria() {}
    
    /**
     * {@inheritDoc}
     * 
     * @return <code>true</code> if <code>networkInterface</code> is 
     *         {@link NetworkInterface#isUp() up}
     */
    @Override
    public boolean isAcceptable(NetworkInterface networkInterface, InetAddress address) throws SocketException {
        
        return networkInterface.isUp();
    }
    
    private Object readResolve() throws ObjectStreamException {
        return INSTANCE;
    }

}
