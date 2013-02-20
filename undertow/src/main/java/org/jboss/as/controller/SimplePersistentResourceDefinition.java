package org.jboss.as.controller;

import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static org.jboss.as.controller.ControllerMessages.MESSAGES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADDRESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2013 Red Hat Inc.
 */
public abstract class SimplePersistentResourceDefinition extends SimpleResourceDefinition implements PersistentResourceDefinition {

    protected SimplePersistentResourceDefinition(PathElement pathElement, ResourceDescriptionResolver descriptionResolver) {
        super(pathElement, descriptionResolver);
    }

    protected SimplePersistentResourceDefinition(PathElement pathElement, ResourceDescriptionResolver descriptionResolver, OperationStepHandler addHandler, OperationStepHandler removeHandler) {
        super(pathElement, descriptionResolver, addHandler, removeHandler);
    }

    protected SimplePersistentResourceDefinition(PathElement pathElement, ResourceDescriptionResolver descriptionResolver, OperationStepHandler addHandler, OperationStepHandler removeHandler, OperationEntry.Flag addRestartLevel, OperationEntry.Flag removeRestartLevel) {
        super(pathElement, descriptionResolver, addHandler, removeHandler, addRestartLevel, removeRestartLevel);
    }


    protected Map<String, AttributeDefinition> getAttributeMap() {
        Map<String, AttributeDefinition> res = new HashMap<>();
        for (AttributeDefinition def : getAttributes()) {
            res.put(def.getName(), def);
        }
        return res;
    }

    @Override
    public String getXmlElementName() {
        return getPathElement().getKey();
    }

    @Override
    public String getXmlWrapperElement() {
        return null;
    }

    @Override
    public PersistentResourceDefinition[] getChildren() {
        return new PersistentResourceDefinition[0];
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        super.registerChildren(resourceRegistration);
        for (PersistentResourceDefinition child : getChildren()) {
            resourceRegistration.registerSubModel(child);
        }
    }

    @Override
    public void parse(final XMLExtendedStreamReader reader, PathAddress parentAddress, List<ModelNode> list) throws XMLStreamException {
        if (getXmlWrapperElement() != null) {
            if (reader.getLocalName().equals(getXmlWrapperElement())) {
                if (reader.hasNext()) {
                    if (reader.nextTag() == END_ELEMENT) {
                        return;
                    }
                }
            } else {
                throw ParseUtils.unexpectedElement(reader);
            }

        }
        boolean wildcard = getPathElement().isWildcard();
        String name = null;
        ModelNode op = Util.createAddOperation();
        Map<String, AttributeDefinition> attributes = getAttributeMap();
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String attributeName = reader.getAttributeLocalName(i);
            String value = reader.getAttributeValue(i);
            if (wildcard && NAME.equals(attributeName)) {
                name = value;
            } else if (attributes.containsKey(attributeName)) {
                AttributeDefinition def = attributes.get(attributeName);
                if (def instanceof SimpleAttributeDefinition) {
                    ((SimpleAttributeDefinition) def).parseAndSetParameter(value, op, reader);
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
        PathElement path = wildcard ? PathElement.pathElement(getPathElement().getKey(), name) : getPathElement();
        PathAddress address = parentAddress.append(path);
        op.get(ADDRESS).set(address.toModelNode());
        list.add(op);
        parseChildren(reader, address, list);
        if (getXmlWrapperElement() != null) {
            ParseUtils.requireNoContent(reader);
        }
    }

    private Map<String, PersistentResourceDefinition> getChildrenMap() {
        Map<String, PersistentResourceDefinition> res = new HashMap<>();
        for (PersistentResourceDefinition child : getChildren()) {
            res.put(child.getXmlElementName(), child);
        }
        return res;
    }

    public void parseChildren(final XMLExtendedStreamReader reader, PathAddress parentAddress, List<ModelNode> list) throws XMLStreamException {
        if (getChildren().length == 0) {
            ParseUtils.requireNoContent(reader);
        } else {
            Map<String, PersistentResourceDefinition> children = getChildrenMap();
            while (reader.hasNext() && reader.nextTag() != XMLStreamConstants.END_ELEMENT) {
                PersistentResourceDefinition child = children.get(reader.getLocalName());
                if (child != null) {
                    child.parse(reader, parentAddress, list);
                } else {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }
    }

    protected boolean useValueAsElementName() {
        return false;
    }

    @Override
    public void persist(XMLExtendedStreamWriter writer, ModelNode model) throws XMLStreamException {
        boolean wildcard = getPathElement().isWildcard();
        model = wildcard ? model.get(getPathElement().getKey()) : model.get(getPathElement().getKeyValuePair());
        if (!model.isDefined() && !useValueAsElementName()) {
            return;
        }
        boolean writeWrapper = getXmlWrapperElement() != null;
        if (writeWrapper) {
            writer.writeStartElement(getXmlWrapperElement());
        }

        if (wildcard) {
            for (Property p : model.asPropertyList()) {
                if (useValueAsElementName()) {
                    writer.writeStartElement(p.getName());
                } else {
                    writer.writeStartElement(getXmlElementName());
                    writer.writeAttribute(NAME, p.getName());
                }
                for (AttributeDefinition def : getAttributes()) {
                    def.getAttributeMarshaller().marshallAsAttribute(def, p.getValue(), false, writer);
                }
                persistChildren(writer, p.getValue());
                writer.writeEndElement();
            }
        } else {
            if (useValueAsElementName()){
                writer.writeStartElement(getPathElement().getValue());
            }else{
                writer.writeStartElement(getXmlElementName());
            }
            for (AttributeDefinition def : getAttributes()) {
                def.getAttributeMarshaller().marshallAsAttribute(def, model, false, writer);
            }
            persistChildren(writer, model);
            writer.writeEndElement();
        }

        if (writeWrapper) {
            writer.writeEndElement();
        }
    }

    public void persistChildren(XMLExtendedStreamWriter writer, ModelNode model) throws XMLStreamException {
        for (PersistentResourceDefinition child : getChildren()) {
            child.persist(writer, model);
        }
    }


}
