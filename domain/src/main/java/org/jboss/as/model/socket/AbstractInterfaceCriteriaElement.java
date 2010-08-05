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
    extends AbstractModelElement<T> {

    private static final long serialVersionUID = 396313309912557378L;
    
    private final Element element;
    private InterfaceCriteria interfaceCriteria;
    
    /**
     * Creates a new AbstractInterfaceCriteriaElement by parsing an xml stream.
     * Subclasses using this constructor are responsible for calling 
     * {@link #setInterfaceCriteria(InterfaceCriteria)} before returning from
     * their constructor.
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
    }
    
    /**
     * Creates a new AbstractInterfaceCriteriaElement by parsing an xml stream
     * 
     * @param reader stream reader used to read the xml
     * @param element the element being read
     * @param interfaceCriteria the criteria to use to check whether an network 
     *         interface and address is acceptable for use by an interface
     * 
     * @throws XMLStreamException if an error occurs
     */
    protected AbstractInterfaceCriteriaElement(XMLExtendedStreamReader reader, final Element element, final InterfaceCriteria interfaceCriteria) throws XMLStreamException {
        this(reader, element);
        setInterfaceCriteria(interfaceCriteria);
    }
    
    /**
     * Gets the InterfaceCriteria associated with this element.
     * 
     * @return the criteria. May be <code>null</code> if this method is invoked
     *                  before any subclass constructor has completed; otherwise
     *                  will not be <code>null</code>
     */
    InterfaceCriteria getInterfaceCriteria() {
        return interfaceCriteria;
    }
    
    /**
     * Sets the InterfaceCriteria associated with this element.
     * 
     * @param the criteria. Cannot be <code>null</code>
     */
    protected final void setInterfaceCriteria(InterfaceCriteria criteria) {
        if (criteria == null) {
            throw new IllegalArgumentException("criteria is null");
        }
        this.interfaceCriteria = criteria;
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
