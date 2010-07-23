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
 * Indicates that a network interface must be {@link NetworkInterface#isUp() up} 
 * to match the criteria.
 * 
 * @author Brian Stansberry
 */
public class UpCriteriaElement extends AbstractInterfaceCriteriaElement<UpCriteriaElement> {

    private static final long serialVersionUID = -649277969243521207L;

    /**
     * Creates a new UpCriteriaElement by parsing an xml stream
     * 
     * @param reader stream reader used to read the xml
     * @throws XMLStreamException if an error occurs
     */
    public UpCriteriaElement(XMLExtendedStreamReader reader) throws XMLStreamException {
        super(reader, Element.UP);
    }

    /* (non-Javadoc)
     * @see org.jboss.as.model.socket.InterfaceCriteria#matches(java.net.NetworkInterface)
     */
    @Override
    public boolean isAcceptable(NetworkInterface networkInterface, InetAddress address) throws SocketException {
        
        return networkInterface.isUp();
    }

    @Override
    protected void appendDifference(Collection<AbstractModelUpdate<UpCriteriaElement>> target, UpCriteriaElement other) {
        // no mutable state
    }

    @Override
    protected Class<UpCriteriaElement> getElementClass() {
        return UpCriteriaElement.class;
    }

}
