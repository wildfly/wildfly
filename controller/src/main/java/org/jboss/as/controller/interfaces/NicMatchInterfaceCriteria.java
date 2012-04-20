/**
 *
 */
package org.jboss.as.controller.interfaces;

import static org.jboss.as.controller.ControllerMessages.MESSAGES;

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
public class NicMatchInterfaceCriteria extends AbstractInterfaceCriteria {

    private static final long serialVersionUID = 6456168020697683203L;

    private final Pattern pattern;

    /**
     * Creates a new AnyInterfaceCriteria
     *
     * @param pattern the criteria to check to see if any are satisfied.
     *                 Cannot be <code>null</code>
     *
     * @throws IllegalArgumentException if <code>criteria</code> is <code>null</code>
     */
    public NicMatchInterfaceCriteria(Pattern pattern) {
        if (pattern == null)
            throw MESSAGES.nullVar("pattern");
        this.pattern = pattern;
    }

    public Pattern getAcceptablePattern() {
        return pattern;
    }

    /**
     * {@inheritDoc}
     *
     * @return <code>address</code> if the {@link #getAcceptablePattern() acceptable pattern}
     *          matches <code>networkInterface</code>'s {@link NetworkInterface#getName() name}.
     */
    @Override
    protected InetAddress isAcceptable(NetworkInterface networkInterface, InetAddress address) throws SocketException {

        if( pattern.matcher(networkInterface.getName()).matches() )
            return address;
        return null;
    }

    @Override
    public int hashCode() {
        return pattern.toString().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof NicMatchInterfaceCriteria)
                && pattern.toString().equals(((NicMatchInterfaceCriteria)o).pattern.toString());
    }

}
