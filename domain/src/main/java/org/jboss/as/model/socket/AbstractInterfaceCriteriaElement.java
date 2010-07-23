/**
 * 
 */
package org.jboss.as.model.socket;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.model.AbstractModelElement;
import org.jboss.as.model.Element;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * Base class for domain model elements that represent 
 * {@link InterfaceCriteria the criteria for choosing an IP address} for a 
 * {@link InterfaceElement named interface}.
 * 
 * @author Brian Stansberry
 */
public abstract class AbstractInterfaceCriteriaElement<T extends AbstractInterfaceCriteriaElement<T>> 
    extends AbstractModelElement<T> implements InterfaceCriteria {

    private static final long serialVersionUID = 396313309912557378L;
    
    private final Element element;
    
    /**
     * Creates a new AbstractInterfaceCriteriaElement by parsing an xml stream
     * 
     * @param reader stream reader used to read the xml
     * @param element the element being read
     * 
     * @throws XMLStreamException if an error occurs
     */
    protected AbstractInterfaceCriteriaElement(XMLExtendedStreamReader reader, final Element element) throws XMLStreamException {
        super(reader);
        if (element == null)
            throw new IllegalArgumentException("element is null");
        this.element = element;
        processXmlStream(reader);
    }

    /**
     * Hook for subclasses to process the xml stream during object construction. This
     * default implementation checks that there are no attributes and no child elements.
     * 
     * @param reader stream reader used to read the xml
     * @throws XMLStreamException if an error occurs
     */
    protected void processXmlStream(XMLExtendedStreamReader reader) throws XMLStreamException {
        requireNoAttributes(reader);
        requireNoContent(reader);
    }
    
    /**
     * Gets the {@link Element} type this object represents.
     * 
     * @return the element type. Will not be <code>null</code>
     */
    Element getElement() {
        return element;
    }

    /**
     * {@inheritDoc}
     * 
     * This default implementation uses the hash code of the {@link Element}
     * passed to the constructor. This is appropriate for subclasses with
     * no internal state.
     */
    @Override
    public long elementHash() {
        return element.hashCode() & 0xFFFFFFFF;
    }

    /**
     * {@inheritDoc}
     * 
     * This default implementation simple writes the end of the element. This
     * is appropriate for subclasses whose element type has no attributes or child elements.
     */
    @Override
    public void writeContent(XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        streamWriter.writeEndElement();        
    }

}
