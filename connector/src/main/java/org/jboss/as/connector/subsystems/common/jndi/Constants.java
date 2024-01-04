/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.connector.subsystems.common.jndi;

import org.jboss.as.connector.logging.ConnectorLogger;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.jca.common.api.metadata.Defaults;
import org.jboss.jca.common.api.metadata.ds.DataSource;

public class Constants {

    private static final String JNDINAME_NAME = "jndi-name";

    private static final String USE_JAVA_CONTEXT_NAME = "use-java-context";

    public static SimpleAttributeDefinition JNDI_NAME = new SimpleAttributeDefinitionBuilder(JNDINAME_NAME, ModelType.STRING, false)
            .setXmlName(DataSource.Attribute.JNDI_NAME.getLocalName())
            .setAllowExpression(true)
            .setValidator((__, value) -> {
                if (value.getType() != ModelType.EXPRESSION) {
                    String str = value.asString();
                    if (str.endsWith("/") || str.indexOf("//") != -1) {
                        throw ConnectorLogger.ROOT_LOGGER.jndiNameShouldValidate();
                    }
                }
            })
            .setRestartAllServices()
            .build();

    public static SimpleAttributeDefinition USE_JAVA_CONTEXT = new SimpleAttributeDefinitionBuilder(USE_JAVA_CONTEXT_NAME, ModelType.BOOLEAN, true)
            .setXmlName(DataSource.Attribute.USE_JAVA_CONTEXT.getLocalName())
            .setDefaultValue(new ModelNode(Defaults.USE_JAVA_CONTEXT))
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();
}
