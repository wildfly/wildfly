/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.subsystems.common.jndi;

import static org.jboss.as.connector.subsystems.common.jndi.Constants.JNDI_NAME;
import static org.jboss.as.connector.subsystems.common.jndi.Constants.USE_JAVA_CONTEXT;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;

/**
 * Common use utility methods.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class Util {

    /**
     * Extracts the raw JNDI_NAME value from the given model node, and depending on the value and
     * the value of any USE_JAVA_CONTEXT child node, converts the raw name into a compliant jndi name.
     *
     * @param modelNode the model node; either an operation or the model behind a datasource resource
     *
     * @return the compliant jndi name
     */
    public static String getJndiName(final OperationContext context, final ModelNode modelNode) throws OperationFailedException {
        final String rawJndiName = JNDI_NAME.resolveModelAttribute(context, modelNode).asString();
        return cleanJndiName(rawJndiName, USE_JAVA_CONTEXT.resolveModelAttribute(context, modelNode).asBoolean());
    }

    public static String cleanJndiName(String rawJndiName, Boolean useJavaContext) {
        final String jndiName;
        if (!rawJndiName.startsWith("java:") && useJavaContext) {
            if(rawJndiName.startsWith("jboss/")) {
                // Bind to java:jboss/ namespace
                jndiName = "java:" + rawJndiName;
            } else {
                // Bind to java:/ namespace
                jndiName= "java:/" + rawJndiName;
            }
        } else {
            jndiName = rawJndiName;
        }
        return jndiName;
    }

    private Util() {
        // prevent instantiation
    }
}
