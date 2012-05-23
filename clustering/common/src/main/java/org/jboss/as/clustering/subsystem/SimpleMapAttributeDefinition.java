package org.jboss.as.clustering.subsystem;

import org.jboss.as.controller.MapAttributeDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * @author Tomaz Cerar
 * @created 6.3.12 15:36
 */
public class SimpleMapAttributeDefinition extends MapAttributeDefinition {

    public SimpleMapAttributeDefinition(final String name, final String xmlName, boolean allowNull,boolean expressionAllowed) {
        super(name, xmlName, allowNull,expressionAllowed, 0, Integer.MAX_VALUE, new ModelTypeValidator(ModelType.STRING,allowNull,expressionAllowed), null, null, AttributeAccess.Flag.RESTART_ALL_SERVICES);
    }

    @Override
    protected void addValueTypeDescription(ModelNode node, ResourceBundle bundle) {
        node.get(ModelDescriptionConstants.VALUE_TYPE).set(ModelType.STRING);
        node.get(ModelDescriptionConstants.EXPRESSIONS_ALLOWED).set(new ModelNode(isAllowExpression()));
      }

    @Override
    protected void addAttributeValueTypeDescription(ModelNode node, ResourceDescriptionResolver resolver, Locale locale, ResourceBundle bundle) {
        node.get(ModelDescriptionConstants.VALUE_TYPE).set(ModelType.STRING);
        node.get(ModelDescriptionConstants.EXPRESSIONS_ALLOWED).set(new ModelNode(isAllowExpression()));
    }

    @Override
    protected void addOperationParameterValueTypeDescription(ModelNode node, String operationName, ResourceDescriptionResolver resolver, Locale locale, ResourceBundle bundle) {
        node.get(ModelDescriptionConstants.VALUE_TYPE).set(ModelType.STRING);
        node.get(ModelDescriptionConstants.EXPRESSIONS_ALLOWED).set(new ModelNode(isAllowExpression()));
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

