/**
 * 
 */
package org.jboss.as.model.socket;

import java.net.NetworkInterface;
import java.util.Collection;
import java.util.Collections;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.model.AbstractModelUpdate;
import org.jboss.as.model.Attribute;
import org.jboss.as.model.Element;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * Indicates that a network interface must have a {@link NetworkInterface#getName() name}
 * that matches a given regular expression in order to match the criteria.
 * 
 * @author Brian Stansberry
 */
public class NicMatchCriteriaElement extends AbstractInterfaceCriteriaElement<NicMatchCriteriaElement> {

    private static final long serialVersionUID = 52177844089594172L;

    private Pattern pattern;
    
    /**
     * Creates a new NicMatchCriteriaElement by parsing an xml stream
     * 
     * @param reader stream reader used to read the xml
     * @throws XMLStreamException if an error occurs
     */
    public NicMatchCriteriaElement(XMLExtendedStreamReader reader) throws XMLStreamException {
        super(reader, Element.NIC_MATCH);
        
        // Handle attributes
        Pattern pattern = null;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i ++) {
            final String value = reader.getAttributeValue(i);
            if (reader.getAttributeNamespace(i) != null) {
                throw unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case PATTERN: {
                        try {
                            pattern = Pattern.compile(value);
                        }
                        catch (PatternSyntaxException e) {
                            throw new XMLStreamException("Invalid pattern " + value + " (" + e.getLocalizedMessage() +")", reader.getLocation(), e);
                        }
                        break;
                    }
                    default: throw unexpectedAttribute(reader, i);
                }
            }
        }
        if (pattern == null) {
            throw missingRequired(reader, Collections.singleton(Attribute.PATTERN));
        }
        this.pattern = pattern;
        // Handle elements
        requireNoContent(reader);
        

        setInterfaceCriteria(new NicMatchInterfaceCriteria(pattern));
    }

    @Override
    public long elementHash() {
        return pattern.hashCode() & 0xffffffffL;
    }

    @Override
    public void writeContent(XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        streamWriter.writeAttribute(Attribute.PATTERN.getLocalName(), pattern.pattern());
        streamWriter.writeEndElement();
    }

    @Override
    protected void appendDifference(Collection<AbstractModelUpdate<NicMatchCriteriaElement>> target, NicMatchCriteriaElement other) {
        // no mutable state
    }

    @Override
    protected Class<NicMatchCriteriaElement> getElementClass() {
        return NicMatchCriteriaElement.class;
    }

}
