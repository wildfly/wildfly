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
 * Indicates that an address must be {@link InetAddress#isLinkLocalAddress() link-local} in order to match 
 * the criteria.
 * 
 * @author Brian Stansberry
 */
public class LinkLocalAddressCriteriaElement extends AbstractInterfaceCriteriaElement<LinkLocalAddressCriteriaElement> {

    private static final long serialVersionUID = -649277969243521207L;

    /**
     * Creates a new LinkLocalAddressCriteriaElement by parsing an xml stream
     * 
     * @param reader stream reader used to read the xml
     * @throws XMLStreamException if an error occurs
     */
    public LinkLocalAddressCriteriaElement(XMLExtendedStreamReader reader) throws XMLStreamException {
        super(reader, Element.LINK_LOCAL_ADDRESS);
    }

    /* (non-Javadoc)
     * @see org.jboss.as.model.socket.InterfaceCriteria#matches(java.net.NetworkInterface)
     */
    @Override
    public boolean isAcceptable(NetworkInterface networkInterface, InetAddress address) throws SocketException {    
        return address.isLinkLocalAddress();
    }

    @Override
    protected Class<LinkLocalAddressCriteriaElement> getElementClass() {
        return LinkLocalAddressCriteriaElement.class;
    }

    @Override
    protected void appendDifference(Collection<AbstractModelUpdate<LinkLocalAddressCriteriaElement>> target,
            LinkLocalAddressCriteriaElement other) {
        // no mutable state        
    }    

}
