package org.jboss.as.controller;

import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * @author Jason T. Greene
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 */
public class PropertiesAttributeDefinition extends MapAttributeDefinition {

    public PropertiesAttributeDefinition(final String name, final String xmlName, boolean allowNull) {
        //super(name, xmlName, allowNull, 0, Integer.MAX_VALUE, new PropertyValidator(false, new StringLengthValidator(1)), null, null, AttributeAccess.Flag.RESTART_ALL_SERVICES);
        super(name, xmlName, allowNull, 0, Integer.MAX_VALUE, new ModelTypeValidator(ModelType.STRING));
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
    }

    @Override
    public void marshallAsElement(ModelNode resourceModel, XMLStreamWriter writer) throws XMLStreamException {
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
}
