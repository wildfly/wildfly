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

import static org.jboss.as.connector.subsystems.datasources.AbstractDataSourceAdd.cleanupJavaContext;
import static org.jboss.as.connector.subsystems.datasources.Constants.JNDINAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.USE_JAVA_CONTEXT;

import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;

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
    public static String getJndiName(final ModelNode modelNode) {
        final String rawJndiName = modelNode.require(JNDINAME).asString();
        final String jndiName;
        if (!rawJndiName.startsWith("java:") && modelNode.hasDefined(USE_JAVA_CONTEXT) && modelNode.get(USE_JAVA_CONTEXT).asBoolean()) {
            if(rawJndiName.startsWith("jboss/")) {
                jndiName = "java:/" + rawJndiName;
            } else {
                jndiName= "java:" + rawJndiName;
            }
        } else {
            jndiName = rawJndiName;
        }
        return jndiName;
    }

    /**
     * Gets the appropriate ServiceName to use for the BinderService associated with the given {@code jndiName}
     * @param jndiName  the jndi name
     * @return the service name of the binder service
     */
    public static ServiceName getBinderServiceName(final String jndiName) {

        String bindName = cleanupJavaContext(jndiName);
        final ServiceName parentContextName;
        if (bindName.startsWith("jboss/")) {
            parentContextName = ContextNames.JBOSS_CONTEXT_SERVICE_NAME;
            bindName = bindName.substring(6);
        } else {
            parentContextName = ContextNames.JAVA_CONTEXT_SERVICE_NAME;
        }
        return parentContextName.append(bindName);
    }

    static String cleanupJavaContext(String jndiName) {
        String bindName;
        if (jndiName.startsWith("java:/")) {
            bindName = jndiName.substring(6);
        } else if(jndiName.startsWith("java:")) {
            bindName = jndiName.substring(5);
        } else {
            bindName = jndiName;
        }
        return bindName;
    }

    private Util() {
        // prevent instantiation
    }
}
