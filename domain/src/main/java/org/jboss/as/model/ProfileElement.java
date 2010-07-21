/**
 * 
 */
package org.jboss.as.model;

import java.util.Collection;
import java.util.Collections;
import java.util.NavigableMap;
import java.util.TreeMap;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import org.jboss.msc.service.Location;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * An element representing the set of subsystems that make up a server profile.
 * 
 * @author Brian Stansberry
 */
public class ProfileElement extends AbstractModelElement<ProfileElement> {
    private static final long serialVersionUID = -7412521588206707920L;

    private final String name;
    private final NavigableMap<String, ProfileIncludeElement> includedProfiles = new TreeMap<String, ProfileIncludeElement>();
    @SuppressWarnings("unchecked")
    private final NavigableMap<QName, AbstractSubsystemElement<? extends AbstractSubsystemElement>> subsystems = new TreeMap<QName, AbstractSubsystemElement<? extends AbstractSubsystemElement>>();

    public ProfileElement(final Location location, final String name) {
        super(location);
        if (name != null) throw new IllegalArgumentException("name is null");
        this.name = name;
    }
    
    public ProfileElement(XMLExtendedStreamReader reader) throws XMLStreamException {
        super(reader);
        // Handle attributes
        String name = null;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i ++) {
            final String value = reader.getAttributeValue(i);
            if (reader.getAttributeNamespace(i) != null) {
                throw unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case NAME: {
                        name = value;
                        break;
                    }
                    default: throw unexpectedAttribute(reader, i);
                }
            }
        }
        if (name == null) {
            throw missingRequired(reader, Collections.singleton(Attribute.NAME));
        }
        this.name = name;
        // Handle elements
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case DOMAIN_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case INCLUDE: {
                            final ProfileIncludeElement include = new ProfileIncludeElement(reader);
                            if (includedProfiles.containsKey(include.getProfile())) {
                                throw new XMLStreamException("Included profile " + include.getProfile() + " already declared", reader.getLocation());
                            }
                            includedProfiles.put(include.getProfile(), include);
                            break;
                        }
                        default: throw unexpectedElement(reader);
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
                    subsystems.put(qname, subsystem);
                }
            }
        }
    }
    
    public String getName() {
        return name;
    }

    @Override
    protected void appendDifference(Collection<AbstractModelUpdate<ProfileElement>> target, ProfileElement other) {
        calculateDifference(target, includedProfiles, other.includedProfiles,
                new DifferenceHandler<String, ProfileIncludeElement, ProfileElement>() {
                    public void handleAdd(final Collection<AbstractModelUpdate<ProfileElement>> target,
                            final String name, final ProfileIncludeElement newElement) {
                        // todo add-included-profile operation YIKES!
                        throw new UnsupportedOperationException("implement me");
                    }

                    public void handleRemove(final Collection<AbstractModelUpdate<ProfileElement>> target,
                            final String name, final ProfileIncludeElement oldElement) {
                        // todo remove-included-profile operation YIKES!
                        throw new UnsupportedOperationException("implement me");
                    }

                    public void handleChange(final Collection<AbstractModelUpdate<ProfileElement>> target,
                            final String name, final ProfileIncludeElement oldElement,
                            final ProfileIncludeElement newElement) {
                        // not possible
                        throw new IllegalStateException();
                    }
                });
        calculateDifference(
                target,
                subsystems,
                other.subsystems,
                new DifferenceHandler<QName, AbstractSubsystemElement<? extends AbstractSubsystemElement>, ProfileElement>() {
                    public void handleAdd(final Collection<AbstractModelUpdate<ProfileElement>> target,
                            final QName name,
                            final AbstractSubsystemElement<? extends AbstractSubsystemElement> newElement) {
                        // todo add-subsystem operation
                        throw new UnsupportedOperationException("implement me");
                    }

                    public void handleRemove(final Collection<AbstractModelUpdate<ProfileElement>> target,
                            final QName name,
                            final AbstractSubsystemElement<? extends AbstractSubsystemElement> oldElement) {
                        // todo remove-subsystem operation
                        throw new UnsupportedOperationException("implement me");
                    }

                    public void handleChange(final Collection<AbstractModelUpdate<ProfileElement>> target,
                            final QName name,
                            final AbstractSubsystemElement<? extends AbstractSubsystemElement> oldElement,
                            final AbstractSubsystemElement<? extends AbstractSubsystemElement> newElement) {
                        // todo update-subsystem operation
                        throw new UnsupportedOperationException("implement me");
                    }
                });
    }

    @Override
    public long elementHash() {
        long hash = name.hashCode() & 0xffffffffL;
        hash = calculateElementHashOf(includedProfiles.values(), hash);
        hash = calculateElementHashOf(subsystems.values(), hash);
        return hash;
    }

    @Override
    protected Class<ProfileElement> getElementClass() {
        return ProfileElement.class;
    }

    @Override
    public void writeContent(XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        streamWriter.writeAttribute(Attribute.NAME.getLocalName(), name);
        if (!includedProfiles.isEmpty()) {
            for (ProfileIncludeElement included : includedProfiles.values()) {
                streamWriter.writeStartElement(Element.INCLUDE.getLocalName());
                included.writeContent(streamWriter);
            }
        }
        if (!subsystems.isEmpty()) {
            for (AbstractSubsystemElement<? extends AbstractSubsystemElement> subsystem : subsystems.values()) {
                QName qname = subsystem.getElementName();
                streamWriter.writeStartElement(qname.getNamespaceURI(), qname.getLocalPart());
                subsystem.writeContent(streamWriter);
            }
        }
        streamWriter.writeEndElement();
    }

}
