/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.messaging.activemq;

import static org.jboss.as.controller.registry.AttributeAccess.Flag.STORAGE_RUNTIME;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.config.ConnectorServiceConfiguration;
import org.apache.activemq.artemis.utils.ClassloadingUtil;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.wildfly.extension.messaging.activemq.logging.MessagingLogger;

/**
 * Connector service resource definition
 *
 * @author <a href="http://jmesnil.net">Jeff Mesnil</a> (c) 2012 Red Hat Inc.
 */
public class ConnectorServiceDefinition extends PersistentResourceDefinition {

    private static final AttributeDefinition[] ATTRIBUTES = {
            CommonAttributes.FACTORY_CLASS,
            CommonAttributes.PARAMS };

    static final ConnectorServiceDefinition INSTANCE = new ConnectorServiceDefinition();

    private ConnectorServiceDefinition() {
        super(MessagingExtension.CONNECTOR_SERVICE_PATH,
                MessagingExtension.getResourceDescriptionResolver(false, CommonAttributes.CONNECTOR_SERVICE),
                new ConnectorServiceAddHandler(ATTRIBUTES),
                new ActiveMQReloadRequiredHandlers.RemoveStepHandler());
    }

    static void addConnectorServiceConfigs(final OperationContext context, final Configuration configuration, final ModelNode model)  throws OperationFailedException {
        if (model.hasDefined(CommonAttributes.CONNECTOR_SERVICE)) {
            final List<ConnectorServiceConfiguration> configs = configuration.getConnectorServiceConfigurations();
            for (Property prop : model.get(CommonAttributes.CONNECTOR_SERVICE).asPropertyList()) {
                configs.add(createConnectorServiceConfiguration(context, prop.getName(), prop.getValue()));
            }
        }
    }

    static ConnectorServiceConfiguration createConnectorServiceConfiguration(final OperationContext context, final String name, final ModelNode model) throws OperationFailedException {

        final String factoryClass = CommonAttributes.FACTORY_CLASS.resolveModelAttribute(context, model).asString();
        Map<String, String> unwrappedParameters = CommonAttributes.PARAMS.unwrap(context, model);
        Map<String, Object> parameters = new HashMap<String, Object>(unwrappedParameters);
        return new ConnectorServiceConfiguration()
                .setFactoryClassName(factoryClass)
                .setParams(parameters)
                .setName(name);
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Arrays.asList(ATTRIBUTES);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration registry) {
        OperationStepHandler writeHandler = new ConnectorServiceWriteAttributeHandler(ATTRIBUTES);
        for (AttributeDefinition attr : ATTRIBUTES) {
            if (!attr.getFlags().contains(STORAGE_RUNTIME)) {
                registry.registerReadWriteAttribute(attr, null, writeHandler);
            }
        }
    }

    private static void checkFactoryClass(final String factoryClass) throws OperationFailedException {
        try {
            ClassloadingUtil.newInstanceFromClassLoader(factoryClass);
        } catch (Throwable t) {
            throw MessagingLogger.ROOT_LOGGER.unableToLoadConnectorServiceFactoryClass(factoryClass);
        }
    }

    static class ConnectorServiceWriteAttributeHandler extends ReloadRequiredWriteAttributeHandler {
        ConnectorServiceWriteAttributeHandler(AttributeDefinition... attributes) {
            super(attributes);
        }
        @Override
        protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName,
                ModelNode resolvedValue, ModelNode currentValue,
                org.jboss.as.controller.AbstractWriteAttributeHandler.HandbackHolder<Void> voidHandback)
                throws OperationFailedException {
            if (CommonAttributes.FACTORY_CLASS.getName().equals(attributeName)) {
                checkFactoryClass(resolvedValue.asString());
            }
            return super.applyUpdateToRuntime(context, operation, attributeName, resolvedValue, currentValue, voidHandback);
        }
    }

    static class ConnectorServiceAddHandler extends ActiveMQReloadRequiredHandlers.AddStepHandler {
        ConnectorServiceAddHandler(AttributeDefinition... attributes) {
            super(attributes);
        }

        @Override
        protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model)
                throws OperationFailedException {
            final String factoryClass = CommonAttributes.FACTORY_CLASS.resolveModelAttribute(context, model).asString();
            checkFactoryClass(factoryClass);
            super.performRuntime(context, operation, model);
        }

    }
}