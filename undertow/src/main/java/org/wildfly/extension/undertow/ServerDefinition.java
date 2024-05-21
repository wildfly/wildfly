/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow;

import java.util.Collection;
import java.util.List;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.web.host.CommonWebServer;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2013 Red Hat Inc.
 */
class ServerDefinition extends SimpleResourceDefinition {
    static final PathElement PATH_ELEMENT = PathElement.pathElement(Constants.SERVER);
    static final RuntimeCapability<Void> SERVER_CAPABILITY = RuntimeCapability.Builder.of(Server.SERVICE_DESCRIPTOR)
            .addRequirements(Capabilities.CAPABILITY_UNDERTOW)
            .build();

    static final SimpleAttributeDefinition DEFAULT_HOST = new SimpleAttributeDefinitionBuilder(Constants.DEFAULT_HOST, ModelType.STRING)
            .setRequired(false)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode("default-host"))
            .setRestartAllServices()
            .build();
    static final SimpleAttributeDefinition SERVLET_CONTAINER = new SimpleAttributeDefinitionBuilder(Constants.SERVLET_CONTAINER, ModelType.STRING)
            .setRequired(false)
            .setDefaultValue(new ModelNode("default"))
            .setRestartAllServices()
            .build();

    static final Collection<AttributeDefinition> ATTRIBUTES = List.of(DEFAULT_HOST, SERVLET_CONTAINER);

    ServerDefinition() {
        super(new SimpleResourceDefinition.Parameters(PATH_ELEMENT, UndertowExtension.getResolver(PATH_ELEMENT.getKey()))
                .setAddHandler(new ServerAdd())
                .setRemoveHandler(ReloadRequiredRemoveStepHandler.INSTANCE)
                .addCapabilities(SERVER_CAPABILITY, CommonWebServer.CAPABILITY)
        );
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration registration) {
        for (AttributeDefinition attribute : ATTRIBUTES) {
            registration.registerReadWriteAttribute(attribute, null, ReloadRequiredWriteAttributeHandler.INSTANCE);
        }
    }

    @Override
    public void registerChildren(ManagementResourceRegistration registration) {
        registration.registerSubModel(new AjpListenerResourceDefinition());
        registration.registerSubModel(new HttpListenerResourceDefinition());
        registration.registerSubModel(new HttpsListenerResourceDefinition());
        registration.registerSubModel(new HostDefinition());
    }
}
