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
 * Indicates that a network interface must be a {@link NetworkInterface#isVirtual() virtual interface} 
 * to match the criteria.
 * 
 * @author Brian Stansberry
 */
public class VirtualCriteriaElement extends AbstractInterfaceCriteriaElement<VirtualCriteriaElement> {

    private static final long serialVersionUID = -649277969243521207L;

    /**
     * Creates a new VirtualCriteriaElement by parsing an xml stream
     * 
     * @param reader stream reader used to read the xml
     * @throws XMLStreamException if an error occurs
     */
    public VirtualCriteriaElement(XMLExtendedStreamReader reader) throws XMLStreamException {
        super(reader, Element.VIRTUAL);
    }

    /* (non-Javadoc)
     * @see org.jboss.as.model.socket.InterfaceCriteria#matches(java.net.NetworkInterface)
     */
    @Override
    public boolean isAcceptable(NetworkInterface networkInterface, InetAddress address) throws SocketException {        
        return networkInterface.isVirtual();
    }

    @Override
    protected void appendDifference(Collection<AbstractModelUpdate<VirtualCriteriaElement>> target,
            VirtualCriteriaElement other) {
        // no mutable state
    }

    @Override
    protected Class<VirtualCriteriaElement> getElementClass() {
        return VirtualCriteriaElement.class;
    }

}
