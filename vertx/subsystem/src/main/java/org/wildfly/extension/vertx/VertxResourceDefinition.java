/*
 * Copyright (C) 2020 RedHat
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.extension.vertx;

import static org.wildfly.extension.vertx.VertxConstants.ATTR_JGROUPS_STACK_FILE;
import static org.wildfly.extension.vertx.VertxConstants.ATTR_OPTION_NAME;
import static org.wildfly.extension.vertx.VertxConstants.ELEMENT_VERTX;
import static org.wildfly.extension.vertx.VertxConstants.VERTX_SERVICE;
import static org.wildfly.extension.vertx.logging.VertxLogger.VERTX_LOGGER;

import java.util.Collection;

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

/**
 * This represents a Vert.x instance, and there is only one Vert.x instance can be defined in this subsystem.
 */
public class VertxResourceDefinition extends PersistentResourceDefinition {

    static final String VERTX_CAPABILITY_NAME = "org.wildfly.extension.vertx";

    public static final RuntimeCapability<Void> VERTX_RUNTIME_CAPABILITY =
            RuntimeCapability.Builder.of(VERTX_CAPABILITY_NAME, false, VertxProxy.class).build();

    static VertxResourceDefinition INSTANCE = new VertxResourceDefinition();

    VertxResourceDefinition() {
        super(new SimpleResourceDefinition.Parameters(PathElement.pathElement(VERTX_SERVICE, ELEMENT_VERTX),
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
        VertxResourceAdd() {
            super(new Parameters().addAttribute(VertxAttributes.getSimpleAttributes()));
        }

        @Override
        protected void performRuntime(OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {
            final boolean clustered = VertxAttributes.CLUSTERED.resolveModelAttribute(context, operation).asBoolean();
            String jgroupChannel = operation.hasDefined(VertxConstants.ATTR_JGROUPS_CHANNEL) ? VertxAttributes.JGROUPS_CHANNEL.resolveModelAttribute(context, operation).asString() : null;
            final boolean forkedChannel = VertxAttributes.FORKED_CHANNEL.resolveModelAttribute(context, operation).asBoolean();
            final String jgroupsStackFile = operation.hasDefined(ATTR_JGROUPS_STACK_FILE) ? VertxAttributes.JGROUPS_STACK_FILE.resolveModelAttribute(context, operation).asString() : null;
            if (clustered && jgroupChannel != null && jgroupsStackFile != null) {
                throw VERTX_LOGGER.onlyOneJgroupsConfigNeeded();
            }
            String optionName = operation.hasDefined(ATTR_OPTION_NAME) ? VertxAttributes.OPTION_NAME.resolveModelAttribute(context, operation).asString() : null;
            final VertxProxy vertxProxy = new VertxProxy(clustered, jgroupChannel, forkedChannel, jgroupsStackFile, optionName);
            VertxProxyService.installService(context, vertxProxy);
        }

    }

}
