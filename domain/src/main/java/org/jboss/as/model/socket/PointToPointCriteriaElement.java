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
 * Indicates that a network interface must be a {@link NetworkInterface#isPointToPoint() point-to-point interface}
 * to match the criteria.
 * 
 * @author Brian Stansberry
 */
public class PointToPointCriteriaElement extends AbstractInterfaceCriteriaElement<PointToPointCriteriaElement> {

    private static final long serialVersionUID = -649277969243521207L;

    /**
     * Creates a new PointToPointCriteriaElement by parsing an xml stream
     * 
     * @param reader stream reader used to read the xml
     * @throws XMLStreamException if an error occurs
     */
    public PointToPointCriteriaElement(XMLExtendedStreamReader reader) throws XMLStreamException {
        super(reader, Element.POINT_TO_POINT);
    }

    /* (non-Javadoc)
     * @see org.jboss.as.model.socket.InterfaceCriteria#matches(java.net.NetworkInterface)
     */
    @Override
    public boolean isAcceptable(NetworkInterface networkInterface, InetAddress address) throws SocketException {        
        return networkInterface.isPointToPoint();
    }

    @Override
    protected void appendDifference(Collection<AbstractModelUpdate<PointToPointCriteriaElement>> target,
            PointToPointCriteriaElement other) {
        // no mutable state
    }

    @Override
    protected Class<PointToPointCriteriaElement> getElementClass() {
        return PointToPointCriteriaElement.class;
    }

}
