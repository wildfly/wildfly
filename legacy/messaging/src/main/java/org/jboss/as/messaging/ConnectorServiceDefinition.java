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

package org.jboss.as.messaging;

import static org.jboss.as.controller.registry.AttributeAccess.Flag.STORAGE_RUNTIME;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hornetq.core.config.Configuration;
import org.hornetq.core.config.ConnectorServiceConfiguration;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

/**
 * Connector service resource definition
 *
 * @author <a href="http://jmesnil.net">Jeff Mesnil</a> (c) 2012 Red Hat Inc.
 */
public class ConnectorServiceDefinition extends SimpleResourceDefinition {

    public static final PathElement PATH = PathElement.pathElement(CommonAttributes.CONNECTOR_SERVICE);

    public static final AttributeDefinition[] ATTRIBUTES = { CommonAttributes.FACTORY_CLASS };

    private final boolean registerRuntimeOnly;

    public ConnectorServiceDefinition(final boolean registerRuntimeOnly) {
        super(PATH,
                MessagingExtension.getResourceDescriptionResolver(false, CommonAttributes.CONNECTOR_SERVICE),
                new HornetQReloadRequiredHandlers.AddStepHandler(ATTRIBUTES),
                new HornetQReloadRequiredHandlers.RemoveStepHandler());
        this.registerRuntimeOnly = registerRuntimeOnly;
        setDeprecated(MessagingExtension.DEPRECATED_SINCE);
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
        final Map<String, Object> params = new HashMap<String, Object>();
        if (model.hasDefined(CommonAttributes.PARAM)) {
            for (Property property : model.get(CommonAttributes.PARAM).asPropertyList()) {
                String value = ConnectorServiceParamDefinition.VALUE.resolveModelAttribute(context, property.getValue()).asString();
                params.put(property.getName(), value);
            }
        }
        return new ConnectorServiceConfiguration(factoryClass, params, name);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration registry) {
        super.registerAttributes(registry);
        OperationStepHandler writeHandler = new ReloadRequiredWriteAttributeHandler(ATTRIBUTES);
        for (AttributeDefinition attr : ATTRIBUTES) {
            if (registerRuntimeOnly || !attr.getFlags().contains(STORAGE_RUNTIME)) {
                registry.registerReadWriteAttribute(attr, null, writeHandler);
            }
        }
    }

    @Override
    public void registerChildren(ManagementResourceRegistration registry) {
        registry.registerSubModel(new ConnectorServiceParamDefinition());
    }
}