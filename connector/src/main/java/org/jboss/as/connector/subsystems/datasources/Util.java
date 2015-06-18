/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.connector.subsystems.datasources;

import static org.jboss.as.connector.subsystems.datasources.Constants.JNDI_NAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.USE_JAVA_CONTEXT;

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
     * Extracts the raw JNDINAME value from the given model node, and depending on the value and
     * the value of any USE_JAVA_CONTEXT child node, converts the raw name into a compliant jndi name.
     *
     * @param modelNode the model node; either an operation or the model behind a datasource resource
     *
     * @return the compliant jndi name
     */
    public static String getJndiName(final OperationContext context, final ModelNode modelNode) throws OperationFailedException {
        final String rawJndiName = JNDI_NAME.resolveModelAttribute(context, modelNode).asString();
        return cleanJndiName(rawJndiName, modelNode.hasDefined(USE_JAVA_CONTEXT.getName()) && modelNode.get(USE_JAVA_CONTEXT.getName()).asBoolean());
    }

    public static String cleanJndiName(String rawJndiName, boolean useJavaContext) {
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
