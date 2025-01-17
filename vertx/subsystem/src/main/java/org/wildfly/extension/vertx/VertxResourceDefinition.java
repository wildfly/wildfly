/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.vertx;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;

import java.util.Collection;

import static org.wildfly.extension.vertx.VertxConstants.ATTR_OPTION_NAME;
import static org.wildfly.extension.vertx.VertxConstants.ELEMENT_VERTX;
import static org.wildfly.extension.vertx.VertxConstants.ELEMENT_VERTX;

/**
 * This represents a Vert.x instance, and there is only one Vert.x instance can be defined in this subsystem.
 */
public class VertxResourceDefinition extends PersistentResourceDefinition {

    static final String VERTX_CAPABILITY_NAME = "org.wildfly.extension.vertx";

    public static final RuntimeCapability<Void> VERTX_RUNTIME_CAPABILITY =
            RuntimeCapability.Builder.of(VERTX_CAPABILITY_NAME, false, VertxProxy.class).build();

    static VertxResourceDefinition INSTANCE = new VertxResourceDefinition();

    VertxResourceDefinition() {
        super(new SimpleResourceDefinition.Parameters(PathElement.pathElement(ELEMENT_VERTX, ELEMENT_VERTX),
                VertxSubsystemExtension.getResourceDescriptionResolver(VertxSubsystemExtension.SUBSYSTEM_NAME))
                .setAddHandler(new VertxResourceAdd())
                .setRemoveHandler(ReloadRequiredRemoveStepHandler.INSTANCE)
                .setCapabilities(VERTX_RUNTIME_CAPABILITY)
        );
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return VertxAttributes.getSimpleAttributes();
    }

    static class VertxResourceAdd extends AbstractAddStepHandler {

        @Override
        protected void performRuntime(OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {
            final String optionName = operation.hasDefined(ATTR_OPTION_NAME) ? VertxAttributes.OPTION_NAME.resolveModelAttribute(context, operation).asString() : null;
            VertxProxyService.installService(context, optionName);
        }

    }

}
