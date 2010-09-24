/**
 *
 */
package org.jboss.as.model.socket;

import javax.xml.stream.XMLStreamException;

import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * A named interface definition that is associated with a Server. Differs from
 * {@link InterfaceElement} in that it is required to specify
 * either an address specification or some
 * {@link AbstractInterfaceCriteriaElement criteria} for selecting an address at
 * runtime.
 *
 * @author Brian Stansberry
 */
public class ServerInterfaceElement extends AbstractInterfaceElement<ServerInterfaceElement> {

    private static final long serialVersionUID = 3412142474527180840L;

    /**
     * Creates a new ServerInterfaceElement by parsing an xml stream
     *
     * @param reader stream reader used to read the xml
     * @throws XMLStreamException if an error occurs
     */
    public ServerInterfaceElement(XMLExtendedStreamReader reader) throws XMLStreamException {
        super(reader, true);
    }

    /**
     * Creates a new ServerInterfaceElement from an existing
     * {@link AbstractInterfaceElement#isFullySpecified() fully specified}
     * {@link InterfaceElement}.
     *
     * @param fullySpecified the element to use as a base
     *
     * @throws NullPointerException if {@code fullySpecified} is {@code null}
     * @throws IllegalArgumentException if {@code fullySpecified.isFullySpecified()}
     *             returns {@code false}.
     */
    public ServerInterfaceElement(InterfaceElement fullySpecified) {
        super(fullySpecified.getName(), fullySpecified.getCriteriaElements());
        if (!fullySpecified.isFullySpecified()) {
            throw new IllegalArgumentException(fullySpecified + " is not fully specified");
        }
    }

    @Override
    protected Class<ServerInterfaceElement> getElementClass() {
        return ServerInterfaceElement.class;
    }

}
