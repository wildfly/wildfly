/**
 *
 */
package org.jboss.as.model.socket;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.model.AbstractModelElement;
import org.jboss.as.model.Attribute;
import org.jboss.as.model.ParseUtils;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * A element identifying a socket binding group that should be included in another socket binding group.
 *
 * @author Brian Stansberry
 */
public class SocketBindingGroupIncludeElement extends AbstractModelElement<SocketBindingGroupIncludeElement> {

    private static final long serialVersionUID = 6868487634991345679L;

    private final String groupName;

    /**
     */
    public SocketBindingGroupIncludeElement(final String groupName) {
        this.groupName = groupName;
    }

    /**
     * @param reader
     * @throws XMLStreamException
     */
    public SocketBindingGroupIncludeElement(XMLExtendedStreamReader reader) throws XMLStreamException {
        this.groupName = ParseUtils.readStringAttributeElement(reader, Attribute.SOCKET_BINDING_GROUP.getLocalName());
    }

    /**
     * Gets the name of the included socket-binding-group.
     *
     * @return the profile name. Will not be <code>null</code>
     */
    public String getGroupName() {
        return groupName;
    }

    /* (non-Javadoc)
     * @see org.jboss.as.model.AbstractModelElement#getElementClass()
     */
    @Override
    protected Class<SocketBindingGroupIncludeElement> getElementClass() {
        return SocketBindingGroupIncludeElement.class;
    }

    /* (non-Javadoc)
     * @see org.jboss.as.model.AbstractModelElement#writeContent(org.jboss.staxmapper.XMLExtendedStreamWriter)
     */
    @Override
    public void writeContent(XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        streamWriter.writeAttribute(Attribute.SOCKET_BINDING_GROUP.getLocalName(), groupName);
        streamWriter.writeEndElement();
    }

}
