/**
 *
 */
package org.jboss.as.model;

import org.jboss.msc.service.ServiceActivator;
import org.jboss.msc.service.ServiceActivatorContext;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

/**
 * An element representing the set of subsystems that make up a server profile.
 *
 * @author Brian Stansberry
 */
public class ProfileElement extends AbstractModelElement<ProfileElement> implements ServiceActivator {
    private static final long serialVersionUID = -7412521588206707920L;

    private final String name;
    private final NavigableMap<String, ProfileIncludeElement> includedProfiles = new TreeMap<String, ProfileIncludeElement>();
    private final NavigableMap<String, AbstractSubsystemElement<? extends AbstractSubsystemElement<?>>> subsystems =
        new TreeMap<String, AbstractSubsystemElement<? extends AbstractSubsystemElement<?>>>();
    private final RefResolver<String, ProfileElement> includedProfileResolver;

    /**
     * Construct a new instance.
     *
     * @param includedProfileResolver {@link org.jboss.as.model.RefResolver} to use to resolve references
     *           to included profiles. Should not be used in the constructor
     *           itself as referenced profiles may not have been created yet.
     */
    public ProfileElement(final String name, final RefResolver<String, ProfileElement> includedProfileResolver) {
        if (name != null) throw new IllegalArgumentException("name is null");
        this.name = name;
        this.includedProfileResolver = includedProfileResolver;
    }

    /**
     * Construct a new instance.
     *
     * @param reader the reader from which to build this element
     * @param includedProfileResolver {@link RefResolver} to use to resolve references
     *           to included profiles. Should not be used in the constructor
     *           itself as referenced profiles may not have been parsed yet.
     *           May be <code>null</code>, in which case any nested {@link Element#INCLUDE}
     *           element will result in an
     *           {@link #unexpectedElement(XMLExtendedStreamReader) unexpected element exception}
     * @throws XMLStreamException if an error occurs
     */
    public ProfileElement(XMLExtendedStreamReader reader, final RefResolver<String, ProfileElement> includedProfileResolver) throws XMLStreamException {

        this.includedProfileResolver = includedProfileResolver;

        // Handle attributes
        String name = null;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i ++) {
            final String value = reader.getAttributeValue(i);
            if (reader.getAttributeNamespace(i) != null) {
                throw ParseUtils.unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case NAME: {
                        name = value;
                        break;
                    }
                    default:
                        throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        if (name == null) {
            throw ParseUtils.missingRequired(reader, Collections.singleton(Attribute.NAME));
        }
        this.name = name;
        // Handle elements
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case DOMAIN_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case INCLUDE: {
                            if (includedProfileResolver == null) {
                                throw ParseUtils.unexpectedElement(reader);
                            }
                            final ProfileIncludeElement include = new ProfileIncludeElement(reader);
                            if (includedProfiles.containsKey(include.getProfile())) {
                                throw new XMLStreamException("Included profile " + include.getProfile() + " already declared", reader.getLocation());
                            }
                            includedProfiles.put(include.getProfile(), include);
                            break;
                        }
                        default:
                            throw ParseUtils.unexpectedElement(reader);
                    }
                    break;
                }
                default: {
                    ParseResult<AbstractSubsystemElement<?>> result = new ParseResult<AbstractSubsystemElement<?>>();
                    reader.handleAny(result);
                    AbstractSubsystemElement<?> subsystem = result.getResult();
                    QName qname = subsystem.getElementName();
                    if (subsystems.containsKey(qname)) {
                        throw new XMLStreamException("Subsystem " + qname + " already declared", reader.getLocation());
                    }
                    subsystems.put(qname.getNamespaceURI(), subsystem);
                }
            }
        }
        if (subsystems.size() == 0) {
            throw new XMLStreamException("Profile " + name + " has no subsystem configurations", reader.getLocation());
        }
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
            this.includedProfiles.putAll(source.includedProfiles);
        }


        final NavigableMap<String, ProfileElement> resolvedProfiles = new TreeMap<String, ProfileElement>();
        for (Map.Entry<String, ProfileIncludeElement> entry : this.includedProfiles.entrySet()) {
            ProfileElement prof = source.includedProfileResolver.resolveRef(entry.getKey());
            if (prof == null) {
                throw new IllegalStateException("Profile referenced by '" + Element.INCLUDE.getLocalName() +
                        "' refers to non-existent profile '" + entry.getValue().getProfile() + "'");
            }
            resolvedProfiles.put(entry.getKey(), new ProfileElement(prof));
        }
        this.includedProfileResolver = new SimpleRefResolver<String, ProfileElement>(resolvedProfiles);
    }

    /**
     * Gets the name of the profile
     *
     * @return the profile name
     */
    public String getName() {
        return name;
    }

    public Set<ProfileIncludeElement> getIncludedProfiles() {
        synchronized (includedProfiles) {
            return new HashSet<ProfileIncludeElement>(includedProfiles.values());
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
                for (ProfileIncludeElement included : includedProfiles.values()) {
                    streamWriter.writeStartElement(Element.INCLUDE.getLocalName());
                    included.writeContent(streamWriter);
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

    @Override
    public void activate(final ServiceActivatorContext context) {

        // Activate included profiles
        for (ProfileIncludeElement includeEl : includedProfiles.values()) {
            ProfileElement prof = includedProfileResolver.resolveRef(includeEl.getProfile());
            if (prof == null) {
                throw new IllegalStateException("Profile referenced by '" + Element.INCLUDE.getLocalName() +
                        "' refers to non-existent profile '" + includeEl.getProfile() + "'");
            }
            prof.activate(context);
        }

        // Activate sub-systems
        final Map<String, AbstractSubsystemElement<? extends AbstractSubsystemElement<?>>> subsystems = this.subsystems;
        for(AbstractSubsystemElement<? extends AbstractSubsystemElement<?>> subsystem : subsystems.values()) {
            subsystem.activate(context);
        }
    }

    boolean addSubsystem(final String uri, final AbstractSubsystemElement<?> element) {
        assert uri != null;
        assert element != null;
        return subsystems.put(uri, element) == null;
    }

    boolean removeSubsystem(final String uri) {
        return subsystems.remove(uri) != null;
    }

    public AbstractSubsystemElement<?> getSubsystem(final String namespaceUri) {
        return subsystems.get(namespaceUri);
    }
}
