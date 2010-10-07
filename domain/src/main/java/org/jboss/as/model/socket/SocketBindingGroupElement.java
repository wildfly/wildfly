/**
 *
 */
package org.jboss.as.model.socket;

import java.util.Collections;
import java.util.HashSet;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.model.AbstractModelElement;
import org.jboss.as.model.Attribute;
import org.jboss.as.model.Element;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * A named group of socket binding configurations that can be applied to a
 * server group.
 *
 * @author Brian Stansberry
 */
public class SocketBindingGroupElement extends AbstractModelElement<SocketBindingGroupElement> {

    private static final long serialVersionUID = -7389975620327080290L;

    private final String name;
    private String defaultInterface;
    private final NavigableMap<String, SocketBindingGroupIncludeElement> includedGroups = new TreeMap<String, SocketBindingGroupIncludeElement>();
    private final NavigableMap<String, SocketBindingElement> socketBindings = new TreeMap<String, SocketBindingElement>();

    /**
     * Create a new {@code SocketBindinggroupElement}
     *
     * @param name the binding group name.
     */
    public SocketBindingGroupElement(final String name) {
        this.name = name;
    }

    /**
     * Gets the name of the socket binding group.
     *
     * @return the group name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the name of the default interface to use for socket bindings
     * that do not declare an interface.
     *
     * @return the interface name
     */
    public String getDefaultInterface() {
        return defaultInterface;
    }

    void setDefaultInterface(String defaultInterface) {
        this.defaultInterface = defaultInterface;
    }

    public Set<String> getIncludedSocketBindingGroups() {
        return Collections.unmodifiableSet(new HashSet<String>(includedGroups.keySet()));
    }

    public Set<SocketBindingElement> getSocketBindings() {
        return Collections.unmodifiableSet(new HashSet<SocketBindingElement>(socketBindings.values()));
    }

    /* (non-Javadoc)
     * @see org.jboss.as.model.AbstractModelElement#getElementClass()
     */
    @Override
    protected Class<SocketBindingGroupElement> getElementClass() {
        return SocketBindingGroupElement.class;
    }

    /* (non-Javadoc)
     * @see org.jboss.as.model.AbstractModelElement#writeContent(org.jboss.staxmapper.XMLExtendedStreamWriter)
     */
    @Override
    public void writeContent(XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        streamWriter.writeAttribute(Attribute.NAME.getLocalName(), name);
        streamWriter.writeAttribute(Attribute.DEFAULT_INTERFACE.getLocalName(), defaultInterface);
        synchronized (includedGroups) {
            for (SocketBindingGroupIncludeElement included : includedGroups.values()) {
                streamWriter.writeStartElement(Element.INCLUDE.getLocalName());
                included.writeContent(streamWriter);
            }
        }
        synchronized (socketBindings) {
            for (SocketBindingElement included : socketBindings.values()) {
                streamWriter.writeStartElement(Element.SOCKET_BINDING.getLocalName());
                included.writeContent(streamWriter);
            }
        }

        streamWriter.writeEndElement();
    }

    void addIncludedGroup(final String groupName) {
        this.includedGroups.put(groupName, new SocketBindingGroupIncludeElement(groupName));
    }

    boolean removeIncludedGroup(final String groupName) {
        return this.includedGroups.remove(groupName) != null;
    }

    boolean addSocketBinding(String name, SocketBindingElement binding) {
        return socketBindings.put(name, binding) != null;
    }

    SocketBindingElement getSocketBinding(String name) {
        return socketBindings.get(name);
    }

    boolean removeSocketBinding(String name) {
        return socketBindings.remove(name) != null;
    }
}
