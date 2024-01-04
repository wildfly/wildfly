/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
import org.jboss.as.controller.registry.RuntimePackageDependency;
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
    static AttributeDefinition[] ATTRIBUTES = {HTTP_LISTENER, PARAMS, UPGRADE_LEGACY, CommonAttributes.SSL_CONTEXT};

    HTTPAcceptorDefinition() {
        super(new SimpleResourceDefinition.Parameters(MessagingExtension.HTTP_ACCEPTOR_PATH,
                new StandardResourceDescriptionResolver(CommonAttributes.ACCEPTOR, MessagingExtension.RESOURCE_NAME, MessagingExtension.class.getClassLoader(), true, false) {
                    @Override
                    public String getResourceDescription(Locale locale, ResourceBundle bundle) {
                        return bundle.getString(HTTP_ACCEPTOR);
                    }
                })
                .addCapabilities(CAPABILITY)
                .setAdditionalPackages(
                        RuntimePackageDependency.required("io.undertow.core"),
                        RuntimePackageDependency.required("org.jboss.as.remoting"),
                        RuntimePackageDependency.required("org.jboss.xnio"),
                        RuntimePackageDependency.required("org.jboss.xnio.netty.netty-xnio-transport"))
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

    @Override
    public void registerAdditionalRuntimePackages(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerAdditionalRuntimePackages(
                RuntimePackageDependency.required("io.undertow.core"),
                RuntimePackageDependency.required("org.jboss.xnio"),
                RuntimePackageDependency.required("org.jboss.xnio.netty.netty-xnio-transport"));
    }
}
