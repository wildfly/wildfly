/**
 *
 */
package org.jboss.as.model;

import org.jboss.staxmapper.XMLExtendedStreamWriter;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import java.util.HashSet;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

/**
 * An element representing the set of subsystems that make up a server profile.
 *
 * @author Brian Stansberry
 */
public class ProfileElement extends AbstractModelElement<ProfileElement> {
    private static final long serialVersionUID = -7412521588206707920L;

    private final String name;
    private final Set<String> includedProfiles = new HashSet<String>();
    private final NavigableMap<String, AbstractSubsystemElement<? extends AbstractSubsystemElement<?>>> subsystems =
        new TreeMap<String, AbstractSubsystemElement<? extends AbstractSubsystemElement<?>>>();

    /**
     * Construct a new instance.
     *
     * @param name the name of the profile. Cannot be {@code null}
     */
    public ProfileElement(final String name) {
        if (name == null) throw new IllegalArgumentException("name is null");
        this.name = name;
    }

    /**
     * Creates a new ProfileElement based on an existing element. The key thing
     * this constructor does is use the given <code>source</code> element's
     * {@link RefResolver} to resolve any included profiles. It then creates
     * its own RefResolver from only those included profiles. The effect of this
     * is to eliminate any extraneous ProfileElement references that may be
     * associated with <code>source</code>'s object graph.
     *
     * @param source
     */
    public ProfileElement(ProfileElement source) {

        this.name = source.name;
        synchronized (source.subsystems) {
            this.subsystems.putAll(source.subsystems);
        }
        synchronized (source.includedProfiles) {
            this.includedProfiles.addAll(source.includedProfiles);
        }
    }

    /**
     * Gets the name of the profile
     *
     * @return the profile name
     */
    public String getName() {
        return name;
    }

    public Set<String> getIncludedProfiles() {
        synchronized (includedProfiles) {
            return new HashSet<String>(includedProfiles);
        }
    }

    public Set<AbstractSubsystemElement<? extends AbstractSubsystemElement<?>>> getSubsystems() {
        synchronized (subsystems) {
            return new HashSet<AbstractSubsystemElement<? extends AbstractSubsystemElement<?>>>(subsystems.values());
        }
    }

    @Override
    protected Class<ProfileElement> getElementClass() {
        return ProfileElement.class;
    }

    @Override
    public void writeContent(XMLExtendedStreamWriter streamWriter) throws XMLStreamException {

        streamWriter.writeAttribute(Attribute.NAME.getLocalName(), name);

        synchronized (includedProfiles) {
            if (!includedProfiles.isEmpty()) {
                for (String included : includedProfiles) {
                    streamWriter.writeEmptyElement(Element.INCLUDE.getLocalName());
                    streamWriter.writeAttribute(Attribute.PROFILE.getLocalName(), included);
                }
            }
        }

        synchronized (subsystems) {
            if (!subsystems.isEmpty()) {

                String defaultNamespace = streamWriter.getNamespaceContext().getNamespaceURI(XMLConstants.DEFAULT_NS_PREFIX);

                for (AbstractSubsystemElement<? extends AbstractSubsystemElement<?>> subsystem : subsystems.values()) {
                    try {
                        QName qname = subsystem.getElementName();
                        if (streamWriter.getNamespaceContext().getPrefix(qname.getNamespaceURI()) == null) {
                            // Unknown namespace; it becomes default
                            streamWriter.setDefaultNamespace(qname.getNamespaceURI());
                            streamWriter.writeStartElement(qname.getLocalPart());
                            streamWriter.writeNamespace(null, qname.getNamespaceURI());
                        }
                        else {
                            streamWriter.writeStartElement(qname.getNamespaceURI(), qname.getLocalPart());
                        }
                        subsystem.writeContent(streamWriter);
                    }
                    finally {
                        streamWriter.setDefaultNamespace(defaultNamespace);
                    }
                }
            }
        }

        streamWriter.writeEndElement();
    }

    boolean addSubsystem(final String uri, final AbstractSubsystemElement<?> element) {
        assert uri != null;
        assert element != null;
        return subsystems.put(uri, element) == null;
    }

    boolean removeSubsystem(final String uri) {
        return subsystems.remove(uri) != null;
    }

    boolean addIncludedProfile(String includedProfileName) {
        return includedProfiles.add(includedProfileName);
    }

    boolean removeIncludedProfile(String includedProfileName) {
        return includedProfiles.remove(includedProfileName);
    }

    public AbstractSubsystemElement<?> getSubsystem(final String namespaceUri) {
        return subsystems.get(namespaceUri);
    }
}
