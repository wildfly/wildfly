/*
 * JBoss, Home of Professional Open Source
 * Copyright 2022, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
