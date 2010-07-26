/**
 * 
 */
package org.jboss.as.model.socket;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collection;
import java.util.NavigableMap;
import java.util.TreeMap;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.model.AbstractModelUpdate;
import org.jboss.as.model.Element;
import org.jboss.as.model.Namespace;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * Indicates that if a network interface satisfies any of a set of nested 
 * criteria, it may be used.
 * 
 * @author Brian Stansberry
 */
public class AnyCriteriaElement extends AbstractInterfaceCriteriaElement<AnyCriteriaElement> {

    private static final long serialVersionUID = -649277969243521207L;

    private final NavigableMap<Element, AbstractInterfaceCriteriaElement<?>> interfaceCriteria = 
            new TreeMap<Element, AbstractInterfaceCriteriaElement<?>>();
    /**
     * Creates a new AnyCriteriaElement by parsing an xml stream
     * 
     * @param reader stream reader used to read the xml
     * @throws XMLStreamException if an error occurs
     */
    public AnyCriteriaElement(XMLExtendedStreamReader reader) throws XMLStreamException {
        super(reader, Element.ANY);
    }

    @Override
    protected void processXmlStream(XMLExtendedStreamReader reader) throws XMLStreamException {
        // Handle attributes
        requireNoAttributes(reader);
        // Handle elements
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case DOMAIN_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    AbstractInterfaceCriteriaElement<?> criteria = ParsingUtil.parseSimpleInterfaceCriteria(reader, element);
                    interfaceCriteria.put(criteria.getElement(), criteria);
                    break;
                }
                default: throw unexpectedElement(reader);
            }
        }
        if (interfaceCriteria.isEmpty()) {
            throw ParsingUtil.missingCriteria(reader, ParsingUtil.SIMPLE_CRITERIA_STRING);
        }
    }

    /* (non-Javadoc)
     * @see org.jboss.as.model.socket.InterfaceCriteria#matches(java.net.NetworkInterface)
     */
    @Override
    public boolean isAcceptable(NetworkInterface networkInterface, InetAddress address) throws SocketException {
        for (InterfaceCriteria criteria : interfaceCriteria.values()) {
            if (criteria.isAcceptable(networkInterface, address))
                return true;
        }            
        return false;
    }

    @Override
    public long elementHash() {
        return calculateElementHashOf(interfaceCriteria.values(), 17l);
    }

    @Override
    public void writeContent(XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        for (AbstractInterfaceCriteriaElement<?> criteria : interfaceCriteria.values()) {
            streamWriter.writeStartElement(criteria.getElement().getLocalName());
            criteria.writeContent(streamWriter);
        }

        streamWriter.writeEndElement();
    }

    @Override
    protected void appendDifference(Collection<AbstractModelUpdate<AnyCriteriaElement>> target, AnyCriteriaElement other) {
        // FIXME implement appendDifference
        throw new UnsupportedOperationException("implement me");
    }

    @Override
    protected Class<AnyCriteriaElement> getElementClass() {
        return AnyCriteriaElement.class;
    }
    
    

}
