/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.naming;

import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.naming.deployment.JndiName;

/**
 * Factory methods for creating a JndiName.
 * @author Paul Ferraro
 */
public class JndiNameFactory {
    public static final String DEFAULT_JNDI_NAMESPACE = "java:jboss";

    public static JndiName parse(String value) {
        return value.startsWith("java:") ? JndiName.of(value) : createJndiName(DEFAULT_JNDI_NAMESPACE, value.startsWith("/") ? value.substring(1) : value);
    }

    public static JndiName createJndiName(String namespace, String... contexts) {
        JndiName name = JndiName.of(namespace);
        for (String context: contexts) {
            name = name.append((context != null) ? context : ModelDescriptionConstants.DEFAULT);
        }
        return name;
    }

    private JndiNameFactory() {
        // Hide
    }
}
