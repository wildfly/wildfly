package org.jboss.as.jacorb;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;
import org.jboss.dmr.ValueExpression;
import org.wildfly.iiop.openjdk.Constants;

/**
 * @author <a href=mailto:tadamski@redhat.com>Tomasz Adamski</a>
 */
public class TransformUtils {

    private TransformUtils() {
    }

    static List<String> validateDeprecatedProperites(final ModelNode model) {
        final List<String> propertiesToReject = new LinkedList<>();
        for (final AttributeDefinition attribute : JacORBSubsystemDefinitions.ON_OFF_ATTRIBUTES_TO_REJECT) {
            if (model.hasDefined(attribute.getName())
                    && model.get(attribute.getName()).equals(JacORBSubsystemDefinitions.DEFAULT_ENABLED_PROPERTY)) {
                propertiesToReject.add(attribute.getName());
            }
        }
        for (final AttributeDefinition attribute : JacORBSubsystemDefinitions.ATTRIBUTES_TO_REJECT) {
            if (model.hasDefined(attribute.getName())) {
                propertiesToReject.add(attribute.getName());
            }
        }
        return propertiesToReject;
    }

    static ModelNode transformModel(final ModelNode legacyModel) {
        final ModelNode model = new ModelNode();
        for (Property property : legacyModel.asPropertyList()) {
            String name = property.getName();
            ModelNode legacyValue = property.getValue();
            if (legacyValue.isDefined()) {
                if(name.equals(JacORBSubsystemConstants.IOR_SETTINGS)){
                    transformIorSettings(model, legacyValue);
                    continue;
                }
                final boolean expression;
                final String expressionVariable;
                if(legacyValue.getType()==ModelType.EXPRESSION){
                    expression = true;
                    final Matcher matcher = Pattern.compile("\\A\\$\\{(.*):(.*)\\}\\Z").matcher(legacyValue.asExpression().getExpressionString());
                    if(matcher.find()){
                        expressionVariable = matcher.group(1);
                        String abc = matcher.group(2);
                        legacyValue = new ModelNode(abc);
                    } else {
                        model.get(name).set(legacyValue);
                        continue;
                    }
                } else {
                    expression = false;
                    expressionVariable = null;
                }
                ModelNode value;
                switch (name) {
                    case JacORBSubsystemConstants.ORB_GIOP_MINOR_VERSION:
                        name = Constants.ORB_GIOP_VERSION;
                        value = new ModelNode(new StringBuilder().append("1.").append(legacyValue.asString()).toString());
                        break;
                    case JacORBSubsystemConstants.ORB_INIT_TRANSACTIONS:
                        if (legacyValue.asString().equals(JacORBSubsystemConstants.ON)) {
                            value = new ModelNode(Constants.FULL);
                        } else if (legacyValue.asString().equals(JacORBSubsystemConstants.OFF)) {
                            value = new ModelNode(Constants.NONE);
                        } else {
                            value = legacyValue;
                        }
                        break;
                    case JacORBSubsystemConstants.ORB_INIT_SECURITY:
                        if (legacyValue.asString().equals(JacORBSubsystemConstants.OFF)) {
                            value = new ModelNode(Constants.NONE);
                        } else {
                            value = legacyValue;
                        }
                        break;
                    case JacORBSubsystemConstants.SECURITY_SUPPORT_SSL:
                    case JacORBSubsystemConstants.SECURITY_ADD_COMP_VIA_INTERCEPTOR:
                    case JacORBSubsystemConstants.NAMING_EXPORT_CORBALOC:
                    case JacORBSubsystemConstants.INTEROP_IONA:
                        if (legacyValue.asString().equals(JacORBSubsystemConstants.ON)) {
                            value = new ModelNode(true);
                        } else {
                            value = new ModelNode(false);
                        }
                        break;
                    default:
                        value = legacyValue;
                }
                if (!value.asString().equals(JacORBSubsystemConstants.OFF)) {
                    if (expression) {
                        String newExpression = "${" + expressionVariable;
                        if(expressionVariable != null){
                            newExpression += (":" + value.asString());
                        }
                        newExpression += "}";
                        value = new ModelNode(new ValueExpression(newExpression));
                    }
                    model.get(name).set(value);
                }
            }
        }
        if(!legacyModel.get(JacORBSubsystemConstants.ORB_SOCKET_BINDING).isDefined()){
            model.get(JacORBSubsystemConstants.ORB_SOCKET_BINDING).set(JacORBSubsystemDefinitions.ORB_SOCKET_BINDING.getDefaultValue());
        }
        return model;
    }

    private static void transformIorSettings(final ModelNode model, final ModelNode legacyValue) {
        for (final Property category : legacyValue.get(JacORBSubsystemConstants.DEFAULT).get(JacORBSubsystemConstants.SETTING)
                .asPropertyList()) {
            for (final Property property : category.getValue().asPropertyList()) {
                model.get(property.getName()).set(property.getValue());
            }
        }
    }
}
