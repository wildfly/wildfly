/**
 *
 */
package org.jboss.as.model.socket;

import java.net.NetworkInterface;
import java.util.regex.Pattern;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.interfaces.NicMatchInterfaceCriteria;
import org.jboss.as.model.Attribute;
import org.jboss.as.model.Element;
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
     * Creates a new NicMatchCriteriaElement
     *
     * @param pattern the pattern
     */
    public NicMatchCriteriaElement(final Pattern pattern) {
        super(Element.NIC_MATCH);
        this.pattern = pattern;
        setInterfaceCriteria(new NicMatchInterfaceCriteria(pattern));
    }

    @Override
    public void writeContent(XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        streamWriter.writeAttribute(Attribute.PATTERN.getLocalName(), pattern.pattern());
        streamWriter.writeEndElement();
    }

    @Override
    protected Class<NicMatchCriteriaElement> getElementClass() {
        return NicMatchCriteriaElement.class;
    }

}
