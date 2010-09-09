/**
 *
 */
package org.jboss.as.model.socket;

import javax.xml.stream.XMLStreamException;

import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * A named interface definition that is associated with a Server. Differs from
 * {@link InterfaceElement the superclass} in that it is required to specify
 * either an address specification or some
 * {@link AbstractInterfaceCriteriaElement criteria} for selecting an address at
 * runtime.
 *
 * @author Brian Stansberry
 */
public class ServerInterfaceElement extends InterfaceElement {

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

}
