package org.jboss.as.controller;

import java.util.Locale;
import java.util.ResourceBundle;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Represents simple key=value map equivalent of java.util.Map<String,String>()
 *
 * @author Jason T. Greene
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 */
//todo maybe replace with SimpleMapAttributeDefinition?
public class PropertiesAttributeDefinition extends MapAttributeDefinition {

    /**
     * @param name
     * @param xmlName
     * @param allowNull
     * @deprecated use {@link Builder}
     */
    @Deprecated
    public PropertiesAttributeDefinition(final String name, final String xmlName, boolean allowNull) {
        super(name, xmlName, allowNull, 0, Integer.MAX_VALUE, new ModelTypeValidator(ModelType.STRING));
    }

    private PropertiesAttributeDefinition(final String name, final String xmlName, final boolean allowNull, boolean allowExpression,
                                          final int minSize, final int maxSize, final ParameterValidator elementValidator,
                                          final String[] alternatives, final String[] requires, final AttributeMarshaller attributeMarshaller, final boolean resourceOnly, final AttributeAccess.Flag... flags) {
        super(name, xmlName, allowNull, allowExpression, minSize, maxSize, elementValidator, alternatives, requires, attributeMarshaller, resourceOnly, flags);
    }

    @Override
    protected void addValueTypeDescription(ModelNode node, ResourceBundle bundle) {
        setValueType(node);
    }

    @Override
    protected void addAttributeValueTypeDescription(ModelNode node, ResourceDescriptionResolver resolver, Locale locale, ResourceBundle bundle) {
        setValueType(node);
    }

    @Override
    protected void addOperationParameterValueTypeDescription(ModelNode node, String operationName, ResourceDescriptionResolver resolver, Locale locale, ResourceBundle bundle) {
        setValueType(node);
    }

    void setValueType(ModelNode node) {
        node.get(ModelDescriptionConstants.VALUE_TYPE).set(ModelType.STRING);
        if (isAllowExpression()) {
            node.get(ModelDescriptionConstants.EXPRESSIONS_ALLOWED).set(new ModelNode(true));
        }
    }

    @Override
    public void marshallAsElement(ModelNode resourceModel, boolean marshalDefault, XMLStreamWriter writer) throws XMLStreamException {
        if (!isMarshallable(resourceModel)) { return; }

        resourceModel = resourceModel.get(getName());
        writer.writeStartElement(getName());
        for (ModelNode property : resourceModel.asList()) {
            writer.writeEmptyElement(getXmlName());
            writer.writeAttribute(org.jboss.as.controller.parsing.Attribute.NAME.getLocalName(), property.asProperty().getName());
            writer.writeAttribute(org.jboss.as.controller.parsing.Attribute.VALUE.getLocalName(), property.asProperty().getValue().asString());
        }
        writer.writeEndElement();
    }

    public static class Builder extends AbstractAttributeDefinitionBuilder<Builder, PropertiesAttributeDefinition> {

        public Builder(final String name, boolean allowNull) {
            super(name, ModelType.OBJECT, allowNull);
        }

        public Builder(final PropertiesAttributeDefinition basis) {
            super(basis);
        }

        @Override
        public PropertiesAttributeDefinition build() {
            if (validator == null) {
                validator = new ModelTypeValidator(ModelType.STRING, allowNull, allowExpression);
            }
            return new PropertiesAttributeDefinition(name, xmlName, allowNull, allowExpression, minSize, maxSize, validator, alternatives, requires, attributeMarshaller, resourceOnly, flags);
        }
    }
}
