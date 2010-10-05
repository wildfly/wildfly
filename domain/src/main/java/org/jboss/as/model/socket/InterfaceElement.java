/**
 *
 */
package org.jboss.as.model.socket;

import javax.xml.stream.XMLStreamException;

import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * A named interface definition, with an optional specification of what address
 * to use for the interface or an
 * optional set of {@link AbstractInterfaceCriteriaElement criteria} for
 * determining at runtime what address to use for the interface.
 *
 * @author Brian Stansberry
 */
public class InterfaceElement extends AbstractInterfaceElement<InterfaceElement> {

    private static final long serialVersionUID = -5256526713311518506L;

    public InterfaceElement(final String name) {
        super(name);
    }

    /**
     * Creates a new InterfaceElement by parsing an xml stream
     *
     * @param reader stream reader used to read the xml
     * @throws XMLStreamException if an error occurs
     */
    public InterfaceElement(XMLExtendedStreamReader reader) throws XMLStreamException {
        super(reader, false);
    }

    @Override
    protected Class<InterfaceElement> getElementClass() {
        return InterfaceElement.class;
    }

}
