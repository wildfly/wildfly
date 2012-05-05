package org.jboss.as.security;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXPRESSIONS_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NILLABLE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PASSWORD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE_TYPE;

import java.util.Locale;
import java.util.ResourceBundle;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.controller.operations.validation.ParametersOfValidator;
import org.jboss.as.controller.operations.validation.ParametersValidator;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author Jason T. Greene
 */
public class KeyStoreAttributeDefinition extends AttributeDefinition {
    private static final ParameterValidator keyStoreValidator;
    private static final ParameterValidator fieldValidator;
    private static final String[] FIELDS = { Constants.PASSWORD, Constants.TYPE, Constants.URL, Constants.PROVIDER, Constants.PROVIDER_ARGUMENT};
    static {
        final ParametersValidator delegate = new ParametersValidator();
        for (String field : FIELDS) {
            delegate.registerValidator(field, new ModelTypeValidator(ModelType.STRING, true, true));
        }
        keyStoreValidator = new ParametersOfValidator(delegate);
        fieldValidator = delegate;
    }

    protected KeyStoreAttributeDefinition(String name) {
        super(name, null, null, ModelType.OBJECT, true, false, null, keyStoreValidator, null, null, AttributeAccess.Flag.RESTART_ALL_SERVICES);
    }

    public void marshallAsAttribute(ModelNode resourceModel, final boolean marshallDefault, final XMLStreamWriter writer) throws XMLStreamException {
        if (isMarshallable(resourceModel, marshallDefault)) {
            resourceModel = resourceModel.get(getName());
            if (resourceModel.hasDefined(Constants.PASSWORD))
                writer.writeAttribute(getName() + "-" + Constants.PASSWORD, resourceModel.get(Constants.PASSWORD).asString());
            if (resourceModel.hasDefined(Constants.TYPE))
                writer.writeAttribute(getName() + "-" + Constants.TYPE, resourceModel.get(Constants.TYPE).asString());
            if (resourceModel.hasDefined(Constants.URL))
                writer.writeAttribute(getName() + "-" + Constants.URL, resourceModel.get(Constants.URL).asString());
            if (resourceModel.hasDefined(Constants.PROVIDER))
                writer.writeAttribute(getName() + "-" + Constants.PROVIDER, resourceModel.get(Constants.PROVIDER).asString());
            if (resourceModel.hasDefined(Constants.PROVIDER_ARGUMENT))
                writer.writeAttribute(getName() + "-" + Constants.PROVIDER_ARGUMENT, resourceModel.get(Constants.PROVIDER_ARGUMENT).asString());
        }
    }

    public static ModelNode parseField(String name, String value, XMLStreamReader reader) throws XMLStreamException {
        final String trimmed = value == null ? null : value.trim();
        ModelNode node;
        if (trimmed != null ) {
            node = new ModelNode().set(ParseUtils.parsePossibleExpression(trimmed));
        } else {
            node = new ModelNode();
        }

        try {
            fieldValidator.validateParameter(name, node);
        } catch (OperationFailedException e) {
            throw SecurityMessages.MESSAGES.xmlStreamException(e.getFailureDescription().toString(), reader.getLocation());
        }
        return node;
    }

    @Override
    public ModelNode addResourceAttributeDescription(ModelNode resourceDescription, ResourceDescriptionResolver resolver,
                                                     Locale locale, ResourceBundle bundle) {
        final ModelNode result = super.addResourceAttributeDescription(resourceDescription, resolver, locale, bundle);
        addAttributeValueTypeDescription(result, resolver, locale, bundle);
        return result;
    }

    @Override
    public ModelNode addOperationParameterDescription(ModelNode resourceDescription, String operationName,
                                                      ResourceDescriptionResolver resolver, Locale locale, ResourceBundle bundle) {
        final ModelNode result = super.addOperationParameterDescription(resourceDescription, operationName, resolver, locale, bundle);
        addOperationParameterValueTypeDescription(result, operationName, resolver, locale, bundle);
        return result;
    }

    private void addAttributeValueTypeDescription(ModelNode result, ResourceDescriptionResolver resolver, Locale locale, ResourceBundle bundle) {
        final ModelNode valueType = getNoTextValueTypeDescription(result);
        valueType.get(PASSWORD, DESCRIPTION).set(resolver.getResourceAttributeValueTypeDescription(getName(), locale, bundle, PASSWORD));
        valueType.get(Constants.TYPE, DESCRIPTION).set(resolver.getResourceAttributeValueTypeDescription(getName(), locale, bundle, Constants.TYPE));
        valueType.get(Constants.URL, DESCRIPTION).set(resolver.getResourceAttributeValueTypeDescription(getName(), locale, bundle, Constants.URL));
        valueType.get(Constants.PROVIDER, DESCRIPTION).set(resolver.getResourceAttributeValueTypeDescription(getName(), locale, bundle, Constants.PROVIDER));
        valueType.get(Constants.PROVIDER_ARGUMENT, DESCRIPTION).set(resolver.getResourceAttributeValueTypeDescription(getName(), locale, bundle, Constants.PROVIDER_ARGUMENT));
    }

    private void addOperationParameterValueTypeDescription(ModelNode result, String operationName, ResourceDescriptionResolver resolver, Locale locale, ResourceBundle bundle) {
        final ModelNode valueType = getNoTextValueTypeDescription(result);
        valueType.get(PASSWORD, DESCRIPTION).set(resolver.getOperationParameterValueTypeDescription(operationName, getName(), locale, bundle, PASSWORD));
        valueType.get(Constants.TYPE, DESCRIPTION).set(resolver.getOperationParameterValueTypeDescription(operationName, getName(), locale, bundle, Constants.TYPE));
        valueType.get(Constants.URL, DESCRIPTION).set(resolver.getOperationParameterValueTypeDescription(operationName, getName(), locale, bundle, Constants.URL));
        valueType.get(Constants.PROVIDER, DESCRIPTION).set(resolver.getOperationParameterValueTypeDescription(operationName, getName(), locale, bundle, Constants.PROVIDER));
        valueType.get(Constants.PROVIDER_ARGUMENT, DESCRIPTION).set(resolver.getOperationParameterValueTypeDescription(operationName, getName(), locale, bundle, Constants.PROVIDER_ARGUMENT));
    }

    private ModelNode getNoTextValueTypeDescription(final ModelNode parent) {
        final ModelNode valueType = parent.get(VALUE_TYPE);
        final ModelNode password = valueType.get(PASSWORD);
        password.get(DESCRIPTION); // placeholder
        password.get(TYPE).set(ModelType.STRING);
        password.get(NILLABLE).set(true);
        password.get(EXPRESSIONS_ALLOWED).set(true);

        final ModelNode type = valueType.get(Constants.TYPE);
        type.get(DESCRIPTION);  // placeholder
        type.get(TYPE).set(ModelType.STRING);
        type.get(NILLABLE).set(true);
        type.get(EXPRESSIONS_ALLOWED).set(true);

        final ModelNode url = valueType.get(Constants.URL);
        url.get(DESCRIPTION);  // placeholder
        url.get(TYPE).set(ModelType.STRING);
        url.get(NILLABLE).set(true);
        url.get(EXPRESSIONS_ALLOWED).set(true);

        final ModelNode provider = valueType.get(Constants.PROVIDER);
        provider.get(DESCRIPTION);  // placeholder
        provider.get(TYPE).set(ModelType.STRING);
        provider.get(NILLABLE).set(true);
        provider.get(EXPRESSIONS_ALLOWED).set(true);

        final ModelNode argument = valueType.get(Constants.PROVIDER_ARGUMENT);
        argument.get(DESCRIPTION);  // placeholder
        argument.get(TYPE).set(ModelType.STRING);
        argument.get(NILLABLE).set(true);
        argument.get(EXPRESSIONS_ALLOWED).set(true);

        return valueType;
    }

    @Override
    public ModelNode addResourceAttributeDescription(ResourceBundle bundle, String prefix, ModelNode resourceDescription) {
       throw SecurityMessages.MESSAGES.unsupportedOperationExceptionUseResourceDesc();
    }

    @Override
    public ModelNode addOperationParameterDescription(ResourceBundle bundle, String prefix, ModelNode operationDescription) {
       throw SecurityMessages.MESSAGES.unsupportedOperationExceptionUseResourceDesc();
    }

    @Override
    public ModelNode validateOperation(ModelNode operationObject) throws OperationFailedException {
        ModelNode validateOp = operationObject;
        if (operationObject.hasDefined(getName())) {
            // Convert any expression strings into ModelType.EXPRESSION
            validateOp = operationObject.clone();
            ModelNode attr = validateOp.get(getName());
            for (String field : FIELDS) {
                ModelNode fieldNode = attr.get(field);
                if (fieldNode.getType() == ModelType.STRING) {
                    fieldNode.set(ParseUtils.parsePossibleExpression(fieldNode.asString()));
                }
            }
        }
        return super.validateOperation(validateOp);
    }

    @Override
    public void marshallAsElement(ModelNode resourceModel, XMLStreamWriter writer) throws XMLStreamException {
        throw SecurityMessages.MESSAGES.unsupportedOperation();
    }
}
