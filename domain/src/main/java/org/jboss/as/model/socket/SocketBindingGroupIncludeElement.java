/**
 * 
 */
package org.jboss.as.model.socket;

import java.util.Collection;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.model.AbstractModelElement;
import org.jboss.as.model.AbstractModelUpdate;
import org.jboss.as.model.Attribute;
import org.jboss.msc.service.Location;
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
     * @param location
     */
    public SocketBindingGroupIncludeElement(Location location, final String groupName) {
        super(location);
        this.groupName = groupName;
    }

    /**
     * @param reader
     * @throws XMLStreamException
     */
    public SocketBindingGroupIncludeElement(XMLExtendedStreamReader reader) throws XMLStreamException {
        super(reader);
        this.groupName = readStringAttributeElement(reader, Attribute.SOCKET_BINDING_GROUP.getLocalName());
    }
    
    /**
     * Gets the name of the included socket-binding-group.
     * 
     * @return the profile name. Will not be <code>null</code>
     */
    public String getGroupName() {
        return groupName;
    }

    @Override
    protected void appendDifference(Collection<AbstractModelUpdate<SocketBindingGroupIncludeElement>> target,
            SocketBindingGroupIncludeElement other) {
        // no mutable state
    }

    @Override
    public long elementHash() {
        return groupName.hashCode() & 0xffffffffL;
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
