/**
 *
 */
package org.jboss.as.model.socket;

import java.net.NetworkInterface;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.interfaces.InterfaceCriteria;
import org.jboss.as.model.Element;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * Indicates that a network interface must be a {@link NetworkInterface#isLoopback() loopback interface}
 * to match the criteria.
 *
 * @author Brian Stansberry
 */
public class SimpleCriteriaElement extends AbstractInterfaceCriteriaElement<SimpleCriteriaElement> {

    private static final long serialVersionUID = -649277969243521207L;

    /**
     * Create a new SimpleCriteria element.
     *
     * @param type the criteria type
     * @param criteria the criteria
     */
    public SimpleCriteriaElement(Element type, InterfaceCriteria criteria) {
        super(type, criteria);
    }

    @Override
    protected Class<SimpleCriteriaElement> getElementClass() {
        return SimpleCriteriaElement.class;
    }

    @Override
    public void writeContent(XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        streamWriter.writeEndElement();
    }
}
