package org.jboss.as.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.parsing.Element;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static org.jboss.as.controller.ControllerMessages.MESSAGES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADDRESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;

/**
 * A representation of a resource as needed by the XML parser.
 *
 * @author Stuart Douglas
 */
public class PersistentResourceXMLDescription {

    private final PersistentResourceDefinition resourceDefinition;
    private final String xmlElementName;
    private final String xmlWrapperElement;
    private final LinkedHashMap<String, AttributeDefinition> attributes;
    private final List<PersistentResourceXMLDescription> children;
    private final boolean useValueAsElementName;

    private PersistentResourceXMLDescription(final PersistentResourceDefinition resourceDefinition, final String xmlElementName, final String xmlWrapperElement, final LinkedHashMap<String, AttributeDefinition> attributes, final List<PersistentResourceXMLDescription> children, final boolean useValueAsElementName) {
        this.resourceDefinition = resourceDefinition;
        this.xmlElementName = xmlElementName;
        this.xmlWrapperElement = xmlWrapperElement;
        this.attributes = attributes;
        this.children = children;
        this.useValueAsElementName = useValueAsElementName;
    }

    public void parse(final XMLExtendedStreamReader reader, PathAddress parentAddress, List<ModelNode> list) throws XMLStreamException {
        if (xmlWrapperElement != null) {
            if (reader.getLocalName().equals(xmlWrapperElement)) {
                if (reader.hasNext()) {
                    if (reader.nextTag() == END_ELEMENT) {
                        return;
                    }
                }
            } else {
                throw ParseUtils.unexpectedElement(reader);
            }

        }
        boolean wildcard = resourceDefinition.getPathElement().isWildcard();
        String name = null;
        ModelNode op = Util.createAddOperation();
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String attributeName = reader.getAttributeLocalName(i);
            String value = reader.getAttributeValue(i);
            if (wildcard && NAME.equals(attributeName)) {
                name = value;
            } else if (attributes.containsKey(attributeName)) {
                AttributeDefinition def = attributes.get(attributeName);
                if (def instanceof SimpleAttributeDefinition) {
                    ((SimpleAttributeDefinition) def).parseAndSetParameter(value, op, reader);
                } else if (def instanceof StringListAttributeDefinition) {
                    ((StringListAttributeDefinition) def).parseAndSetParameter(value, op, reader);
                } else {
                    throw new IllegalArgumentException("we should know how to handle " + def);
                }
            } else {
                throw ParseUtils.unexpectedAttribute(reader, i);
            }
        }
        if (wildcard && name == null) {
            throw MESSAGES.missingRequiredAttributes(new StringBuilder(NAME), reader.getLocation());
        }
        PathElement path = wildcard ? PathElement.pathElement(resourceDefinition.getPathElement().getKey(), name) : resourceDefinition.getPathElement();
        PathAddress address = parentAddress.append(path);
        op.get(ADDRESS).set(address.toModelNode());
        list.add(op);
        parseChildren(reader, address, list);
        if (xmlWrapperElement != null) {
            ParseUtils.requireNoContent(reader);
        }
    }

    private Map<String, PersistentResourceXMLDescription> getChildrenMap() {
        Map<String, PersistentResourceXMLDescription> res = new HashMap<>();
        for (PersistentResourceXMLDescription child : children) {
            if (child.xmlWrapperElement != null) {
                res.put(child.xmlWrapperElement, child);
            } else {
                res.put(child.xmlElementName, child);
            }
        }
        return res;
    }

    public void parseChildren(final XMLExtendedStreamReader reader, PathAddress parentAddress, List<ModelNode> list) throws XMLStreamException {
        if (children.size() == 0) {
            ParseUtils.requireNoContent(reader);
        } else {
            Map<String, PersistentResourceXMLDescription> children = getChildrenMap();
            while (reader.hasNext() && reader.nextTag() != XMLStreamConstants.END_ELEMENT) {
                PersistentResourceXMLDescription child = children.get(reader.getLocalName());
                if (child != null) {
                    child.parse(reader, parentAddress, list);
                } else {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }
    }


    public void persist(XMLExtendedStreamWriter writer, ModelNode model) throws XMLStreamException {
        persist(writer, model, null);
    }

    private void writeStartElement(XMLExtendedStreamWriter writer, String namespaceURI, String localName) throws XMLStreamException {
        if (namespaceURI != null) {
            writer.writeStartElement(namespaceURI, localName);
        } else {
            writer.writeStartElement(localName);
        }
    }

    public void startSubsystemElement(XMLExtendedStreamWriter writer, String namespaceURI, boolean empty) throws XMLStreamException {

        if (writer.getNamespaceContext().getPrefix(namespaceURI) == null) {
            // Unknown namespace; it becomes default
            writer.setDefaultNamespace(namespaceURI);
            if (empty) {
                writer.writeEmptyElement(Element.SUBSYSTEM.getLocalName());
            } else {
                writer.writeStartElement(Element.SUBSYSTEM.getLocalName());
            }
            writer.writeNamespace(null, namespaceURI);
        } else {
            if (empty) {
                writer.writeEmptyElement(namespaceURI, Element.SUBSYSTEM.getLocalName());
            } else {
                writer.writeStartElement(namespaceURI, Element.SUBSYSTEM.getLocalName());
            }
        }

    }

    public void persist(XMLExtendedStreamWriter writer, ModelNode model, String namespaceURI) throws XMLStreamException {
        boolean wildcard = resourceDefinition.getPathElement().isWildcard();
        model = wildcard ? model.get(resourceDefinition.getPathElement().getKey()) : model.get(resourceDefinition.getPathElement().getKeyValuePair());
        if (!model.isDefined() && !useValueAsElementName) {
            return;
        }
        boolean isSubsystem = resourceDefinition.getPathElement().getKey().equals(ModelDescriptionConstants.SUBSYSTEM);

        boolean writeWrapper = xmlWrapperElement != null;
        if (writeWrapper) {
            writeStartElement(writer, namespaceURI, xmlWrapperElement);
        }

        if (wildcard) {
            for (Property p : model.asPropertyList()) {
                if (useValueAsElementName) {
                    writeStartElement(writer, namespaceURI, p.getName());
                } else {
                    writeStartElement(writer, namespaceURI, xmlElementName);
                    writer.writeAttribute(NAME, p.getName());
                }
                for (Map.Entry<String, AttributeDefinition> def : attributes.entrySet()) {
                    def.getValue().getAttributeMarshaller().marshallAsAttribute(def.getValue(), p.getValue(), false, writer);
                }
                persistChildren(writer, p.getValue());
                writer.writeEndElement();
            }
        } else {
            if (useValueAsElementName) {
                writeStartElement(writer, namespaceURI, resourceDefinition.getPathElement().getValue());
            } else if (isSubsystem) {
                startSubsystemElement(writer, namespaceURI, children.isEmpty());
            } else {
                writeStartElement(writer, namespaceURI, xmlElementName);

            }
            for (Map.Entry<String, AttributeDefinition> def : attributes.entrySet()) {
                def.getValue().getAttributeMarshaller().marshallAsAttribute(def.getValue(), model, false, writer);
            }
            persistChildren(writer, model);
            writer.writeEndElement();
        }

        if (writeWrapper) {
            writer.writeEndElement();
        }
    }

    public void persistChildren(XMLExtendedStreamWriter writer, ModelNode model) throws XMLStreamException {
        for (PersistentResourceXMLDescription child : children) {
            child.persist(writer, model);
        }
    }

    public static PersistentResourceXMLBuilder builder(PersistentResourceDefinition resource) {
        return new PersistentResourceXMLBuilder(resource);
    }

    public static class PersistentResourceXMLBuilder {


        private final PersistentResourceDefinition resourceDefinition;
        private String xmlElementName;
        private String xmlWrapperElement;
        private boolean useValueAsElementName;
        private final LinkedHashMap<String, AttributeDefinition> attributes = new LinkedHashMap<>();
        private final List<PersistentResourceXMLBuilder> children = new ArrayList<>();

        PersistentResourceXMLBuilder(final PersistentResourceDefinition resourceDefinition) {
            this.resourceDefinition = resourceDefinition;
            this.xmlElementName = resourceDefinition.getPathElement().isWildcard() ? resourceDefinition.getPathElement().getKey() : resourceDefinition.getPathElement().getValue();
        }

        public PersistentResourceXMLBuilder addChild(PersistentResourceXMLBuilder builder) {
            this.children.add(builder);
            return this;
        }

        public PersistentResourceXMLBuilder addAttribute(AttributeDefinition attribute) {
            this.attributes.put(attribute.getName(), attribute);
            return this;
        }

        public PersistentResourceXMLBuilder addAttributes(AttributeDefinition... attributes) {
            for (final AttributeDefinition at : attributes) {
                this.attributes.put(at.getName(), at);
            }
            return this;
        }

        public PersistentResourceXMLBuilder setXmlWrapperElement(final String xmlWrapperElement) {
            this.xmlWrapperElement = xmlWrapperElement;
            return this;
        }

        public PersistentResourceXMLBuilder setXmlElementName(final String xmlElementName) {
            this.xmlElementName = xmlElementName;
            return this;
        }

        public PersistentResourceXMLBuilder setUseValueAsElementName(final boolean useValueAsElementName) {
            this.useValueAsElementName = useValueAsElementName;
            return this;
        }

        public PersistentResourceXMLDescription build() {

            List<PersistentResourceXMLDescription> builtChildren = new ArrayList<>();
            for (PersistentResourceXMLBuilder b : children) {
                builtChildren.add(b.build());
            }
            return new PersistentResourceXMLDescription(resourceDefinition, xmlElementName, xmlWrapperElement, attributes, builtChildren, useValueAsElementName);
        }
    }
}
