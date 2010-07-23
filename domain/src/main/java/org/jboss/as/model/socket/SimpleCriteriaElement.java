/**
 * 
 */
package org.jboss.as.model.socket;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collection;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.model.AbstractModelUpdate;
import org.jboss.as.model.Element;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * Indicates that a network interface must be a {@link NetworkInterface#isLoopback() loopback interface} 
 * to match the criteria.
 * 
 * @author Brian Stansberry
 */
public class SimpleCriteriaElement extends AbstractInterfaceCriteriaElement<SimpleCriteriaElement> {

    private static final long serialVersionUID = -649277969243521207L;

    /**
     * Creates a new LoopbackCriteriaElement by parsing an xml stream
     * 
     * @param reader stream reader used to read the xml
     * @param type the specific type of this element
     * 
     * @throws XMLStreamException if an error occurs
     */
    public SimpleCriteriaElement(XMLExtendedStreamReader reader, Element type) throws XMLStreamException {
        super(reader, type);
        switch (type) {
            case LINK_LOCAL_ADDRESS:
            case LOOPBACK:
            case MULTICAST:
            case POINT_TO_POINT:
            case PUBLIC_ADDRESS:
            case SITE_LOCAL_ADDRESS:
            case UP:
            case VIRTUAL:
                // ok
                break;
            default:
                throw new IllegalArgumentException(type.getLocalName() + " is not a valid simple criteria type");
        }
    }

    /* (non-Javadoc)
     * @see org.jboss.as.model.socket.InterfaceCriteria#matches(java.net.NetworkInterface)
     */
    @Override
    public boolean isAcceptable(NetworkInterface networkInterface, InetAddress address) throws SocketException {

        switch (getElement()) {
            case LINK_LOCAL_ADDRESS:
                return address.isLinkLocalAddress();
            case LOOPBACK:
                return networkInterface.isLoopback();
            case MULTICAST:
                return networkInterface.supportsMulticast();
            case POINT_TO_POINT:
                return networkInterface.isPointToPoint();
            case PUBLIC_ADDRESS:
                return !address.isSiteLocalAddress() && !address.isLinkLocalAddress() && !address.isAnyLocalAddress();
            case SITE_LOCAL_ADDRESS:
                return address.isSiteLocalAddress();
            case UP:
                return networkInterface.isUp();
            case VIRTUAL:
                return networkInterface.isVirtual();
            default:
                // Constructor should prevent this
                throw new IllegalStateException(getElement().getLocalName() + " is not a valid simple criteria type");
        }
        
    }

    @Override
    protected Class<SimpleCriteriaElement> getElementClass() {
        return SimpleCriteriaElement.class;
    }

    @Override
    protected void appendDifference(Collection<AbstractModelUpdate<SimpleCriteriaElement>> target,
            SimpleCriteriaElement other) {
        // no mutable state        
    }
    
    

}
