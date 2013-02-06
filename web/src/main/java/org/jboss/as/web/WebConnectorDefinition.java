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

package org.jboss.as.web;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.ServiceRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author Tomaz Cerar
 * @created 22.2.12 15:03
 */
public class WebConnectorDefinition extends SimpleResourceDefinition {
    protected static final WebConnectorDefinition INSTANCE = new WebConnectorDefinition();


    protected static final SimpleAttributeDefinition NAME =
            new SimpleAttributeDefinitionBuilder(Constants.NAME, ModelType.STRING)
                    .setXmlName(Constants.NAME)
                    .setAllowNull(true) // todo should be false, but 'add' won't validate then
                    .build();

    protected static final SimpleAttributeDefinition PROTOCOL =
            new SimpleAttributeDefinitionBuilder(Constants.PROTOCOL, ModelType.STRING)
                    .setAllowNull(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setValidator(new StringLengthValidator(1))
                    .setAllowExpression(true)
                    .build();

    protected static final SimpleAttributeDefinition SOCKET_BINDING =
            new SimpleAttributeDefinitionBuilder(Constants.SOCKET_BINDING, ModelType.STRING)
                    .setAllowNull(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setValidator(new StringLengthValidator(1))
                    .build();

    protected static final SimpleAttributeDefinition SCHEME =
            new SimpleAttributeDefinitionBuilder(Constants.SCHEME, ModelType.STRING)
                    .setAllowNull(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setValidator(new StringLengthValidator(1))
                    .setAllowExpression(true)
                    //.setDefaultValue(new ModelNode("http"))
                    .build();

    protected static final SimpleAttributeDefinition EXECUTOR =
            new SimpleAttributeDefinitionBuilder(Constants.EXECUTOR, ModelType.STRING)
                    .setAllowNull(true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setValidator(new StringLengthValidator(1, true))
                    .build();

    protected static final SimpleAttributeDefinition ENABLED =
            new SimpleAttributeDefinitionBuilder(Constants.ENABLED, ModelType.BOOLEAN)
                    .setAllowNull(true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode(true))
                    .setAllowExpression(true)
                    .build();

    protected static final SimpleAttributeDefinition ENABLE_LOOKUPS =
            new SimpleAttributeDefinitionBuilder(Constants.ENABLE_LOOKUPS, ModelType.BOOLEAN)
                    .setAllowNull(true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode(false))
                    .setAllowExpression(true)
                    .build();

    protected static final SimpleAttributeDefinition PROXY_NAME =
            new SimpleAttributeDefinitionBuilder(Constants.PROXY_NAME, ModelType.STRING)
                    .setAllowNull(true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setValidator(new StringLengthValidator(1, true))
                    .setAllowExpression(true)
                    .build();

    protected static final SimpleAttributeDefinition PROXY_PORT =
            new SimpleAttributeDefinitionBuilder(Constants.PROXY_PORT, ModelType.INT)
                    .setAllowNull(true)
                    .setValidator(new IntRangeValidator(1, true))
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setAllowExpression(true)
                    .build();

    protected static final SimpleAttributeDefinition MAX_POST_SIZE =
            new SimpleAttributeDefinitionBuilder(Constants.MAX_POST_SIZE, ModelType.INT)
                    .setAllowNull(true)
                    .setValidator(new IntRangeValidator(0, true))
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode(2097152))
                    .setAllowExpression(true)
                    .build();

    protected static final SimpleAttributeDefinition MAX_SAVE_POST_SIZE =
            new SimpleAttributeDefinitionBuilder(Constants.MAX_SAVE_POST_SIZE, ModelType.INT)
                    .setAllowNull(true)
                    .setValidator(new IntRangeValidator(0, true))
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode(4096))
                    .setAllowExpression(true)
                    .build();

    protected static final SimpleAttributeDefinition SECURE =
            new SimpleAttributeDefinitionBuilder(Constants.SECURE, ModelType.BOOLEAN)
                    .setAllowNull(true)
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode(false))
                    .build();

    protected static final SimpleAttributeDefinition REDIRECT_PORT =
            new SimpleAttributeDefinitionBuilder(Constants.REDIRECT_PORT, ModelType.INT)
                    .setAllowNull(true)
                    .setValidator(new IntRangeValidator(1, true))
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode(443))
                    .setAllowExpression(true)
                    .build();

    protected static final SimpleAttributeDefinition MAX_CONNECTIONS =
            new SimpleAttributeDefinitionBuilder(Constants.MAX_CONNECTIONS, ModelType.INT)
                    .setAllowNull(true)
                    .setValidator(new IntRangeValidator(1, true))
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setAllowExpression(true)
                    .build();

    protected static final StringListAttributeDefinition VIRTUAL_SERVER = new StringListAttributeDefinition.Builder(Constants.VIRTUAL_SERVER)
            .setAllowNull(true)
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .setValidator(new StringLengthValidator(1, false))
            .build();

    protected static final SimpleAttributeDefinition[] CONNECTOR_ATTRIBUTES = {
            //NAME, // name is read-only
            // IMPORTANT -- keep these in xsd order as this order controls marshalling
            PROTOCOL,
            SCHEME,
            SOCKET_BINDING,
            ENABLE_LOOKUPS,
            PROXY_NAME,
            PROXY_PORT,
            REDIRECT_PORT,
            SECURE,
            MAX_POST_SIZE,
            MAX_SAVE_POST_SIZE,
            ENABLED,
            EXECUTOR,
            MAX_CONNECTIONS

    };

    private WebConnectorDefinition() {
        super(WebExtension.CONNECTOR_PATH,
                WebExtension.getResourceDescriptionResolver(Constants.CONNECTOR),
                WebConnectorAdd.INSTANCE,
                new ServiceRemoveStepHandler(WebSubsystemServices.JBOSS_WEB_CONNECTOR, WebConnectorAdd.INSTANCE));
    }


    @Override
    public void registerAttributes(ManagementResourceRegistration connectors) {
        connectors.registerReadOnlyAttribute(NAME, null);
        for (AttributeDefinition def : CONNECTOR_ATTRIBUTES) {
            connectors.registerReadWriteAttribute(def, null, new ReloadRequiredWriteAttributeHandler(def));
        }
        connectors.registerReadWriteAttribute(VIRTUAL_SERVER,null,new ReloadRequiredWriteAttributeHandler(VIRTUAL_SERVER));

        for (final SimpleAttributeDefinition def : WebConnectorMetrics.ATTRIBUTES) {
            connectors.registerMetric(def, WebConnectorMetrics.INSTANCE);
        }
    }
}
