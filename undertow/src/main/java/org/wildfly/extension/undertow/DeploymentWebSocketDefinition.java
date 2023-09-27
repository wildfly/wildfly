/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelType;

/**
 * @author Stuart Douglas
 */
public class DeploymentWebSocketDefinition extends SimpleResourceDefinition {

    static final SimpleAttributeDefinition PATH = new SimpleAttributeDefinitionBuilder("path", ModelType.STRING, false).setStorageRuntime().build();
    static final SimpleAttributeDefinition ENDPOINT_CLASS = new SimpleAttributeDefinitionBuilder("endpoint-class", ModelType.STRING, false).setStorageRuntime().build();


    DeploymentWebSocketDefinition() {
        super(PathElement.pathElement("websocket"), UndertowExtension.getResolver("deployment.websocket"));
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration registration) {
        registration.registerReadOnlyAttribute(PATH, null);
        registration.registerReadOnlyAttribute(ENDPOINT_CLASS, null);

    }
}
