/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

import static org.jboss.as.controller.SimpleAttributeDefinitionBuilder.create;
import static org.wildfly.extension.messaging.activemq.Capabilities.HTTP_LISTENER_REGISTRY_CAPABILITY_NAME;
import static org.wildfly.extension.messaging.activemq.Capabilities.HTTP_UPGRADE_REGISTRY_CAPABILITY_NAME;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.HTTP_ACCEPTOR;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.PARAMS;

import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.extension.messaging.activemq.MessagingServices.ServerNameMapper;

/**
 * HTTP acceptor resource definition
 *
 * @author <a href="http://jmesnil.net">Jeff Mesnil</a> (c) 2013 Red Hat Inc.
 */
public class HTTPAcceptorDefinition extends PersistentResourceDefinition {

    static final RuntimeCapability<Void> CAPABILITY = RuntimeCapability.Builder.of("org.wildfly.messaging.activemq", true, HTTPUpgradeService.class)
            .setDynamicNameMapper(new ServerNameMapper("http-upgrade-service"))
            .addRequirements(HTTP_LISTENER_REGISTRY_CAPABILITY_NAME)
            .addAdditionalRequiredPackages("io.undertow.core", "org.jboss.as.remoting", "org.jboss.xnio", "org.jboss.xnio.netty.netty-xnio-transport")
            .build();

    static final SimpleAttributeDefinition HTTP_LISTENER = create(CommonAttributes.HTTP_LISTENER, ModelType.STRING)
            .setRequired(true)
            .setCapabilityReference(HTTP_UPGRADE_REGISTRY_CAPABILITY_NAME, CAPABILITY)
            .build();
    static final SimpleAttributeDefinition UPGRADE_LEGACY = create("upgrade-legacy", ModelType.BOOLEAN)
            .setDefaultValue(ModelNode.TRUE)
            .setRequired(false)
            .setAllowExpression(true)
            .build();
    static AttributeDefinition[] ATTRIBUTES = { HTTP_LISTENER, PARAMS, UPGRADE_LEGACY };

    static final HTTPAcceptorDefinition INSTANCE = new HTTPAcceptorDefinition();

    private HTTPAcceptorDefinition() {
        super(new SimpleResourceDefinition.Parameters(MessagingExtension.HTTP_ACCEPTOR_PATH,
                new StandardResourceDescriptionResolver(CommonAttributes.ACCEPTOR, MessagingExtension.RESOURCE_NAME, MessagingExtension.class.getClassLoader(), true, false) {
                    @Override
                    public String getResourceDescription(Locale locale, ResourceBundle bundle) {
                        return bundle.getString(HTTP_ACCEPTOR);
                    }
                })
                .addCapabilities(CAPABILITY)
                .setAddHandler(HTTPAcceptorAdd.INSTANCE)
                .setRemoveHandler(HTTPAcceptorRemove.INSTANCE));
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration registry) {
        OperationStepHandler attributeHandler = new ReloadRequiredWriteAttributeHandler(ATTRIBUTES);
        for (AttributeDefinition attr : ATTRIBUTES) {
            if (!attr.getFlags().contains(AttributeAccess.Flag.STORAGE_RUNTIME)) {
                registry.registerReadWriteAttribute(attr, null, attributeHandler);
            }
        }
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Arrays.asList(ATTRIBUTES);
    }
}
