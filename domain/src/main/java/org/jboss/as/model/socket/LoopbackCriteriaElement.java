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
public class LoopbackCriteriaElement extends AbstractInterfaceCriteriaElement<LoopbackCriteriaElement> {

    private static final long serialVersionUID = -649277969243521207L;

    /**
     * Creates a new LoopbackCriteriaElement by parsing an xml stream
     * 
     * @param reader stream reader used to read the xml
     * @throws XMLStreamException if an error occurs
     */
    public LoopbackCriteriaElement(XMLExtendedStreamReader reader) throws XMLStreamException {
        super(reader, Element.LOOPBACK);
    }

    /* (non-Javadoc)
     * @see org.jboss.as.model.socket.InterfaceCriteria#matches(java.net.NetworkInterface)
     */
    @Override
    public boolean isAcceptable(NetworkInterface networkInterface, InetAddress address) throws SocketException {
        
        return networkInterface.isLoopback();
    }

    @Override
    protected Class<LoopbackCriteriaElement> getElementClass() {
        return LoopbackCriteriaElement.class;
    }

    @Override
    protected void appendDifference(Collection<AbstractModelUpdate<LoopbackCriteriaElement>> target,
            LoopbackCriteriaElement other) {
        // no mutable state        
    }
    
    

}
