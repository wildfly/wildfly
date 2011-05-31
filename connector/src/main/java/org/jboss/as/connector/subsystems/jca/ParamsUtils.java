package org.jboss.as.connector.subsystems.jca;

import org.jboss.dmr.ModelNode;

public class ParamsUtils {

    public static boolean has(ModelNode operation, String name) {
        return operation.has(name) && operation.get(name).isDefined();
    }

    public static boolean parseBooleanParameter(ModelNode operation, String name) {
        return parseBooleanParameter(operation, name, false);
    }

    public static boolean parseBooleanParameter(ModelNode operation, String name, boolean defaultValue) {
        return has(operation, name) ? operation.get(name).asBoolean() : defaultValue;
    }

    public static String parseStringParameter(ModelNode operation, String name) {
        return has(operation, name) ? operation.get(name).toString() : null;
    }
}
