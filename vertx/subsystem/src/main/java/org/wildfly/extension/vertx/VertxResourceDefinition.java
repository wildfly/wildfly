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

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.capability.DynamicNameMappers;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.logging.ControllerLogger;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;

import java.util.List;

import static org.wildfly.extension.vertx.VertxConstants.*;
import static org.wildfly.extension.vertx.logging.VertxLogger.VERTX_LOGGER;

/**
 * Each VertxDefinition represents a Vert.x instance.
 */
public class VertxResourceDefinition extends SimpleResourceDefinition {

    static final String VERTX_CAPABILITY_NAME = "org.wildfly.extension.vertx";

    static final RuntimeCapability<Void> VERTX_RUNTIME_CAPABILITY =
            RuntimeCapability.Builder.of(VERTX_CAPABILITY_NAME, true, VertxProxy.class)
                    .setDynamicNameMapper(DynamicNameMappers.PARENT)
                    .build();

    static VertxResourceDefinition INSTANCE = new VertxResourceDefinition();

    VertxResourceDefinition() {
        super(new SimpleResourceDefinition.Parameters(PathElement.pathElement(ELEMENT_VERTX),
                VertxSubsystemExtension.getResourceDescriptionResolver(VertxSubsystemExtension.SUBSYSTEM_NAME))
                .setAddHandler(new VertxResourceAdd())
                .setRemoveHandler(ReloadRequiredRemoveStepHandler.INSTANCE)
                .setCapabilities(VERTX_RUNTIME_CAPABILITY)
        );
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);
        List<AttributeDefinition> attributes = VertxAttributes.getSimpleAttributes();
        ReloadRequiredWriteAttributeHandler handler = new ReloadRequiredWriteAttributeHandler(attributes);
        for (AttributeDefinition attr : attributes) {
            if(!attr.getFlags().contains(AttributeAccess.Flag.RESTART_ALL_SERVICES)) {
                throw ControllerLogger.ROOT_LOGGER.attributeWasNotMarkedAsReloadRequired(attr.getName(), resourceRegistration.getPathAddress());
            }
            if (attr.equals(VertxAttributes.JNDI_NAME)) {
                resourceRegistration.registerReadWriteAttribute(attr, new JndiNameReadAttributeHandler(), handler);
            } else {
                resourceRegistration.registerReadWriteAttribute(attr, null, handler);
            }
        }
    }

    static class JndiNameReadAttributeHandler implements OperationStepHandler {

        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            if (context.isNormalServer()) {
                String name = context.getCurrentAddressValue();
                VertxProxy vertxProxy = VertxRegistry.INSTANCE.getVertx(name);
                if (vertxProxy == null) {
                    throw VERTX_LOGGER.vertxNotFound(name);
                }
                context.getResult().set(vertxProxy.getJndiName());
            }
        }
    }

    static class VertxResourceAdd extends AbstractAddStepHandler {
        VertxResourceAdd() {
            super(new Parameters()
                    .addAttribute(VertxAttributes.getSimpleAttributes())
            );
        }

        @Override
        protected void performRuntime(OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {
            final String name = context.getCurrentAddressValue();
            final String jndiName;
            if (operation.hasDefined(VertxConstants.ATTR_JNDI_NAME)) {
                jndiName = VertxAttributes.JNDI_NAME.resolveModelAttribute(context, operation).asString();
            } else {
                jndiName = VertxConstants.DEFAULT_JNDI_PREFIX + name;
            }
            final boolean clustered = VertxAttributes.CLUSTERED.resolveModelAttribute(context, operation).asBoolean();
            String jgroupChannel = operation.hasDefined(VertxConstants.ATTR_JGROUPS_CHANNEL) ? VertxAttributes.JGROUPS_CHANNEL.resolveModelAttribute(context, operation).asString() : null;
            final boolean forkedChannel = VertxAttributes.FORKED_CHANNEL.resolveModelAttribute(context, operation).asBoolean();
            final String jgroupsStackFile = operation.hasDefined(ATTR_JGROUPS_STACK_FILE) ? VertxAttributes.JGROUPS_STACK_FILE.resolveModelAttribute(context, operation).asString() : null;
            if (clustered && jgroupChannel != null && jgroupsStackFile != null) {
                throw VERTX_LOGGER.onlyOneJgroupsConfigNeeded(name);
            }
            String optionName = operation.hasDefined(ATTR_OPTION_NAME) ? VertxAttributes.OPTION_NAME.resolveModelAttribute(context, operation).asString() : null;
            final List<String> aliases = operation.hasDefined(VertxConstants.ATTR_ALIAS) ? VertxAttributes.ALIAS.unwrap(context, operation) : null;
            final VertxProxy vertxProxy = new VertxProxy(name, jndiName, clustered, jgroupChannel, forkedChannel, jgroupsStackFile);
            if (aliases != null) {
                vertxProxy.setAliases(aliases);
                if (aliases.contains(name)) {
                    throw VERTX_LOGGER.aliasUsedAlready(name, name);
                }
                for (String alias : aliases) {
                    VertxProxy vp = VertxRegistry.getInstance().getByNameOrAlias(alias);
                    if (vp != null) {
                        throw VERTX_LOGGER.aliasUsedAlready(alias, vp.getName());
                    }
                }
            }
            VertxProxyService.installService(context, vertxProxy, optionName);
        }

    }

}
