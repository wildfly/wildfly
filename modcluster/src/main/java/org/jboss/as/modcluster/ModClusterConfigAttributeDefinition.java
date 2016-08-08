package org.jboss.as.modcluster;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NILLABLE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE_TYPE;

import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.as.controller.ListAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.controller.operations.validation.ParametersOfValidator;
import org.jboss.as.controller.operations.validation.ParametersValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

public class ModClusterConfigAttributeDefinition extends ListAttributeDefinition {

    public static final ParameterValidator validator;
    public static final ParameterValidator fieldValidator;

    static {
        final ParametersValidator delegate = new ParametersValidator();


        for(int i = 0; i < ModClusterConfigResourceDefinition.ATTRIBUTES.length; i++){
            SimpleAttributeDefinition attribute = ModClusterConfigResourceDefinition.ATTRIBUTES[i];

            switch(attribute.getType()){
                case STRING:
                    delegate.registerValidator(attribute.getName(), new ModelTypeValidator(ModelType.STRING, attribute.isAllowNull(), attribute.isAllowExpression()));
                break;
                case INT:
                    delegate.registerValidator(attribute.getName(), new ModelTypeValidator(ModelType.INT, attribute.isAllowNull(), attribute.isAllowExpression()));
                    break;
                case BOOLEAN:
                    delegate.registerValidator(attribute.getName(), new ModelTypeValidator(ModelType.BOOLEAN, attribute.isAllowNull(), attribute.isAllowExpression()));
                    break;
                default:
                    break;
            }
        }

        validator = new ParametersOfValidator(delegate);
        fieldValidator = delegate;
    }

    public ModClusterConfigAttributeDefinition(String name, String xmlName) {
        super(name, xmlName, true, 1, Integer.MAX_VALUE, validator, null, null, AttributeAccess.Flag.RESTART_ALL_SERVICES);
    }

    @Override
    protected void addValueTypeDescription(ModelNode node, ResourceBundle bundle) {
        // This method being used indicates a misuse of this class
        // throw ModClusterMessages.MESSAGES.unsupportedOperationExceptionUseResourceDesc();

    }

    @Override
    protected void addAttributeValueTypeDescription(ModelNode node, ResourceDescriptionResolver resolver, Locale locale,
            ResourceBundle bundle) {
        final ModelNode valueType = getNoTextValueTypeDescription(node);

        for(int i = 0; i < ModClusterConfigResourceDefinition.ATTRIBUTES.length; i++){
            String name = ModClusterConfigResourceDefinition.ATTRIBUTES[i].getName();

            valueType.get(name, DESCRIPTION).set(
                    resolver.getResourceAttributeValueTypeDescription(getName(), locale, bundle, name));

        }

    }

    @Override
    protected void addOperationParameterValueTypeDescription(ModelNode node, String operationName,
            ResourceDescriptionResolver resolver, Locale locale, ResourceBundle bundle) {
        final ModelNode valueType = getNoTextValueTypeDescription(node);

        for(int i = 0; i < ModClusterConfigResourceDefinition.ATTRIBUTES.length; i++){
            String name = ModClusterConfigResourceDefinition.ATTRIBUTES[i].getName();

            valueType.get(name, DESCRIPTION).set(
                    resolver.getOperationParameterValueTypeDescription(operationName, getName(), locale, bundle,
                            name));
        }

    }

    private ModelNode getNoTextValueTypeDescription(final ModelNode parent) {
        final ModelNode valueType = parent.get(VALUE_TYPE);

        for(int i = 0; i < ModClusterConfigResourceDefinition.ATTRIBUTES.length; i++){
            String name = ModClusterConfigResourceDefinition.ATTRIBUTES[i].getName();

            ModelNode advertiseSocket = valueType.get(name);
            advertiseSocket.get(DESCRIPTION);
            advertiseSocket.get(TYPE).set(ModClusterConfigResourceDefinition.ATTRIBUTES[i].getType());
            boolean allowNull = ModClusterConfigResourceDefinition.ATTRIBUTES[i].isAllowNull();
            advertiseSocket.get(NILLABLE).set(allowNull);

        }

        return valueType;
    }

}
