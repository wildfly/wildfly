/**
 * 
 */
package org.jboss.as.model.socket;

import java.util.Collection;
import java.util.HashSet;
import java.util.NavigableMap;
import java.util.Set;
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
public class CompoundCriteriaElement extends AbstractInterfaceCriteriaElement<CompoundCriteriaElement> {

    private static final long serialVersionUID = -649277969243521207L;

    private final NavigableMap<Element, AbstractInterfaceCriteriaElement<?>> interfaceCriteria = 
            new TreeMap<Element, AbstractInterfaceCriteriaElement<?>>();
    /**
     * Creates a new AnyCriteriaElement by parsing an xml stream
     * 
     * @param reader stream reader used to read the xml
     * @param isAny true if this type {@link Element#ANY}, false if it is {@link Element#NOT}.
     * 
     * @throws XMLStreamException if an error occurs
     */
    public CompoundCriteriaElement(XMLExtendedStreamReader reader, boolean isAny) throws XMLStreamException {
        super(reader, isAny ? Element.ANY : Element.NOT);
        
        Set<InterfaceCriteria> criteria = new HashSet<InterfaceCriteria>(interfaceCriteria.size());
        for (AbstractInterfaceCriteriaElement<?> element : interfaceCriteria.values()) {
            criteria.add(element.getInterfaceCriteria());
        }
        
        InterfaceCriteria ours = isAny ? new AnyInterfaceCriteria(criteria) : new NotInterfaceCriteria(criteria);
        setInterfaceCriteria(ours);
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
    protected void appendDifference(Collection<AbstractModelUpdate<CompoundCriteriaElement>> target, CompoundCriteriaElement other) {
        // FIXME implement appendDifference
        throw new UnsupportedOperationException("implement me");
    }

    @Override
    protected Class<CompoundCriteriaElement> getElementClass() {
        return CompoundCriteriaElement.class;
    }
    
    

}
