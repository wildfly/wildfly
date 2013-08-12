package org.jboss.as.connector.util;

import org.jboss.as.controller.ObjectListAttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PropertiesAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.dmr.ModelNode;
import org.jboss.jca.common.api.metadata.common.Extension;
import org.jboss.jca.common.api.validator.ValidateException;

import java.util.HashMap;
import java.util.Map;

public class ModelNodeUtil {
    public static Long getLongIfSetOrGetDefault(final OperationContext context, final ModelNode dataSourceNode, final SimpleAttributeDefinition key) throws OperationFailedException {

        ModelNode resolvedNode = key.resolveModelAttribute(context, dataSourceNode);
        return resolvedNode.isDefined() ? resolvedNode.asLong() : null;
    }

    public static Integer getIntIfSetOrGetDefault(final OperationContext context, final ModelNode dataSourceNode, final SimpleAttributeDefinition key) throws OperationFailedException {

        ModelNode resolvedNode = key.resolveModelAttribute(context, dataSourceNode);
        return resolvedNode.isDefined() ? resolvedNode.asInt() : null;
    }

    public static Boolean getBooleanIfSetOrGetDefault(final OperationContext context, final ModelNode dataSourceNode, final SimpleAttributeDefinition key) throws OperationFailedException {
        ModelNode resolvedNode = key.resolveModelAttribute(context, dataSourceNode);
        return resolvedNode.isDefined() ? resolvedNode.asBoolean() : null;

    }

    public static String getResolvedStringIfSetOrGetDefault(final OperationContext context, final ModelNode dataSourceNode, final SimpleAttributeDefinition key) throws OperationFailedException {
        ModelNode resolvedNode = key.resolveModelAttribute(context, dataSourceNode);
        String resolvedString = resolvedNode.isDefined() ? resolvedNode.asString() : null;
        if (resolvedString != null && resolvedString.trim().length() == 0) {
            resolvedString = null;
        }
        return resolvedString;

    }

    public static Extension extractExtension(final OperationContext operationContext, final ModelNode dataSourceNode,
                                             final SimpleAttributeDefinition className, final PropertiesAttributeDefinition propertyName)
            throws ValidateException, OperationFailedException {
        if (dataSourceNode.hasDefined(className.getName())) {
            String exceptionSorterClassName = getResolvedStringIfSetOrGetDefault(operationContext, dataSourceNode, className);

            Map<String, String> unwrapped = propertyName.unwrap(operationContext, dataSourceNode);
            Map<String, String> exceptionSorterProperty = unwrapped.size() > 0 ? unwrapped : null;

            return new Extension(exceptionSorterClassName, exceptionSorterProperty);
        } else {
            return null;
        }
    }

    public static Map<String,String> extractMap(ModelNode operation,  ObjectListAttributeDefinition objList, SimpleAttributeDefinition keyAttribute, SimpleAttributeDefinition valueAttribute) {
            Map<String, String> returnMap = null;
            if (operation.hasDefined(objList.getName())) {
                returnMap = new HashMap<String, String>(operation.get(objList.getName()).asList().size());
                for (ModelNode node : operation.get(objList.getName()).asList()) {
                    returnMap.put(node.asObject().get(keyAttribute.getName()).asString(), node.asObject().get(valueAttribute.getName()).asString());
                }

            }
            return returnMap;
        }
}
