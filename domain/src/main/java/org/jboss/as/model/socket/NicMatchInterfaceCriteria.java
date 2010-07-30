/**
 * 
 */
package org.jboss.as.model.socket;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.regex.Pattern;

/**
 * {@link InterfaceCriteria} that tests whether a given {@link Pattern regex pattern} 
 * matches the network interface's {@link NetworkInterface#getName() name}.
 * 
 * @author Brian Stansberry
 */
public class NicMatchInterfaceCriteria implements InterfaceCriteria {

    private final Pattern pattern;
    
    /**
     * Creates a new AnyInterfaceCriteria
     * 
     * @param criteria the criteria to check to see if any are satisfied. 
     *                 Cannot be <code>null</code>
     *                 
     * @throws IllegalArgumentException if <code>criteria</code> is <code>null</code>
     */
    public NicMatchInterfaceCriteria(Pattern pattern) {
        if (pattern == null)
            throw new IllegalArgumentException("pattern is null");
        this.pattern = pattern;
    }
    
    public Pattern getAcceptablePattern() {
        return pattern;
    }
    
    /**
     * {@inheritDoc}
     * 
     * @return <code>true</code> if the {@link #getAcceptablePattern() acceptable pattern}
     *          matches <code>networkInterface</code>'s {@link NetworkInterface#getName() name}.
     */
    @Override
    public boolean isAcceptable(NetworkInterface networkInterface, InetAddress address) throws SocketException {
        
        return pattern.matcher(networkInterface.getName()).matches();
    }
    
    

}
