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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelOnlyResourceDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.controller.access.management.AccessConstraintDefinition;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
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
public class WebConnectorDefinition extends ModelOnlyResourceDefinition {
    protected static final SimpleAttributeDefinition NAME =
            new SimpleAttributeDefinitionBuilder(Constants.NAME, ModelType.STRING)
                    .setXmlName(Constants.NAME)
                    .setRequired(false) // todo should be false, but 'add' won't validate then
                    .build();
    protected static final SimpleAttributeDefinition PROTOCOL =
            new SimpleAttributeDefinitionBuilder(Constants.PROTOCOL, ModelType.STRING)
                    .setRequired(true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setValidator(new StringLengthValidator(1))
                    .setAllowExpression(true)
                    .build();
    protected static final SimpleAttributeDefinition SOCKET_BINDING =
            new SimpleAttributeDefinitionBuilder(Constants.SOCKET_BINDING, ModelType.STRING)
                    .setRequired(true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setValidator(new StringLengthValidator(1))
                    .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.SOCKET_BINDING_REF)
                    .build();
    protected static final SimpleAttributeDefinition SCHEME =
            new SimpleAttributeDefinitionBuilder(Constants.SCHEME, ModelType.STRING)
                    .setRequired(true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setValidator(new StringLengthValidator(1))
                    .setAllowExpression(true)
                            //.setDefaultValue(new ModelNode("http"))
                    .build();
    protected static final SimpleAttributeDefinition EXECUTOR =
            new SimpleAttributeDefinitionBuilder(Constants.EXECUTOR, ModelType.STRING)
                    .setRequired(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setValidator(new StringLengthValidator(1, true))
                    .build();
    protected static final SimpleAttributeDefinition ENABLED =
            new SimpleAttributeDefinitionBuilder(Constants.ENABLED, ModelType.BOOLEAN)
                    .setRequired(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode(true))
                    .setAllowExpression(true)
                    .build();
    protected static final SimpleAttributeDefinition ENABLE_LOOKUPS =
            new SimpleAttributeDefinitionBuilder(Constants.ENABLE_LOOKUPS, ModelType.BOOLEAN)
                    .setRequired(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode(false))
                    .setAllowExpression(true)
                    .build();
    protected static final SimpleAttributeDefinition PROXY_BINDING =
            new SimpleAttributeDefinitionBuilder(Constants.PROXY_BINDING, ModelType.STRING)
                    .setRequired(false)
                    .setValidator(new StringLengthValidator(1))
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.SOCKET_BINDING_REF)
                    .setAlternatives(Constants.PROXY_NAME, Constants.PROXY_PORT)
                    .build();
    protected static final SimpleAttributeDefinition PROXY_NAME =
            new SimpleAttributeDefinitionBuilder(Constants.PROXY_NAME, ModelType.STRING)
                    .setRequired(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setValidator(new StringLengthValidator(1, true))
                    .setAllowExpression(true)
                    .setAlternatives(Constants.PROXY_BINDING)
                    .build();
    protected static final SimpleAttributeDefinition PROXY_PORT =
            new SimpleAttributeDefinitionBuilder(Constants.PROXY_PORT, ModelType.INT)
                    .setRequired(false)
                    .setValidator(new IntRangeValidator(1, true))
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setAllowExpression(true)
                    .setAlternatives(Constants.PROXY_BINDING)
                    .build();
    protected static final SimpleAttributeDefinition MAX_POST_SIZE =
            new SimpleAttributeDefinitionBuilder(Constants.MAX_POST_SIZE, ModelType.INT)
                    .setRequired(false)
                    .setValidator(new IntRangeValidator(0, true))
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode(2097152))
                    .setAllowExpression(true)
                    .build();
    protected static final SimpleAttributeDefinition MAX_SAVE_POST_SIZE =
            new SimpleAttributeDefinitionBuilder(Constants.MAX_SAVE_POST_SIZE, ModelType.INT)
                    .setRequired(false)
                    .setValidator(new IntRangeValidator(0, true))
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode(4096))
                    .setAllowExpression(true)
                    .build();
    protected static final SimpleAttributeDefinition SECURE =
            new SimpleAttributeDefinitionBuilder(Constants.SECURE, ModelType.BOOLEAN)
                    .setRequired(false)
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode(false))
                    .build();
    protected static final SimpleAttributeDefinition REDIRECT_BINDING =
            new SimpleAttributeDefinitionBuilder(Constants.REDIRECT_BINDING, ModelType.STRING)
                    .setRequired(false)
                    .setValidator(new StringLengthValidator(1))
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.SOCKET_BINDING_REF)
                    .setAlternatives(Constants.REDIRECT_PORT)
                    .build();
    protected static final SimpleAttributeDefinition REDIRECT_PORT =
            new SimpleAttributeDefinitionBuilder(Constants.REDIRECT_PORT, ModelType.INT)
                    .setRequired(false)
                    .setValidator(new IntRangeValidator(1, true))
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode(443))
                    .setAlternatives(Constants.REDIRECT_BINDING)
                    .setAllowExpression(true)
                    .build();
    protected static final SimpleAttributeDefinition MAX_CONNECTIONS =
            new SimpleAttributeDefinitionBuilder(Constants.MAX_CONNECTIONS, ModelType.INT)
                    .setRequired(false)
                    .setValidator(new IntRangeValidator(1, true))
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setAllowExpression(true)
                    .build();
    protected static final StringListAttributeDefinition VIRTUAL_SERVER = new StringListAttributeDefinition.Builder(Constants.VIRTUAL_SERVER)
            .setRequired(false)
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .setElementValidator(new StringLengthValidator(1, false))
            .build();
    protected static final AttributeDefinition[] CONNECTOR_ATTRIBUTES = {
            //NAME, // name is read-only
            // IMPORTANT -- keep these in xsd order as this order controls marshalling
            PROTOCOL,
            SCHEME,
            SOCKET_BINDING,
            ENABLE_LOOKUPS,
            PROXY_BINDING,
            PROXY_NAME,
            PROXY_PORT,
            REDIRECT_BINDING,
            REDIRECT_PORT,
            SECURE,
            MAX_POST_SIZE,
            MAX_SAVE_POST_SIZE,
            ENABLED,
            EXECUTOR,
            MAX_CONNECTIONS,
            VIRTUAL_SERVER

    };
    static final WebConnectorDefinition INSTANCE = new WebConnectorDefinition();

    private static final List<AccessConstraintDefinition> ACCESS_CONSTRAINTS;
    static {
        List<AccessConstraintDefinition> constraints = new ArrayList<AccessConstraintDefinition>();
        constraints.add(WebExtension.WEB_CONNECTOR_CONSTRAINT);
        ACCESS_CONSTRAINTS = Collections.unmodifiableList(constraints);
    }

    private WebConnectorDefinition() {
        super(WebExtension.CONNECTOR_PATH,
                WebExtension.getResourceDescriptionResolver(Constants.CONNECTOR),
                new AddressToNameAddAdaptor(CONNECTOR_ATTRIBUTES),
                CONNECTOR_ATTRIBUTES);
        this.setDeprecated(WebExtension.DEPRECATED_SINCE);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration connectors) {
        super.registerAttributes(connectors);
        connectors.registerReadOnlyAttribute(NAME, null);
    }

    @Override
    public List<AccessConstraintDefinition> getAccessConstraints() {
        return ACCESS_CONSTRAINTS;
    }

    /**
     * The following is a slight hack specifically for supporting mod_cluster with current DC and legacy EAP 6.x slaves.
     * With introduction of capability support for mod_cluster connector attribute, a fake capability corresponding to
     * Undertow listener is registered for JBoss Web connectors so that the requirement can be satisfied for legacy profiles.
     */
    private final String UNDERTOW_LISTENER_CAPABILITY_NAME = "org.wildfly.undertow.listener";
    private final RuntimeCapability<Void> FAKE_UNDERTOW_LISTENER_CAPABILITY = RuntimeCapability.Builder.of(UNDERTOW_LISTENER_CAPABILITY_NAME, true)
            .setAllowMultipleRegistrations(true)
            .build();

    @Override
    public void registerCapabilities(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerCapability(FAKE_UNDERTOW_LISTENER_CAPABILITY);
    }
}
