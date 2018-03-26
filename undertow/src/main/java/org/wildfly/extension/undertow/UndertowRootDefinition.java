/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.undertow;

import static org.wildfly.extension.undertow.Capabilities.CAPABILITY_HTTP_INVOKER;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import io.undertow.server.handlers.PathHandler;
import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.ValueExpression;
import org.jboss.msc.service.ServiceController;
import org.jboss.security.SecurityConstants;
import org.wildfly.extension.undertow.filters.FilterDefinitions;
import org.wildfly.extension.undertow.handlers.HandlerDefinitions;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2012 Red Hat Inc.
 */
class UndertowRootDefinition extends PersistentResourceDefinition {

    static final RuntimeCapability<Void> UNDERTOW_CAPABILITY = RuntimeCapability.Builder.of(Capabilities.CAPABILITY_UNDERTOW, false, UndertowService.class)
                        .build();

    static final RuntimeCapability<Void> HTTP_INVOKER_RUNTIME_CAPABILITY =
            RuntimeCapability.Builder.of(CAPABILITY_HTTP_INVOKER, false, PathHandler.class)
                    .build();

    protected static final SimpleAttributeDefinition DEFAULT_SERVLET_CONTAINER =
            new SimpleAttributeDefinitionBuilder(Constants.DEFAULT_SERVLET_CONTAINER, ModelType.STRING, true)
                    .setRestartAllServices()
                    .setDefaultValue(new ModelNode("default"))
                    .setCapabilityReference(UNDERTOW_CAPABILITY, Capabilities.CAPABILITY_SERVLET_CONTAINER)
                    .build();
    protected static final SimpleAttributeDefinition DEFAULT_SERVER =
            new SimpleAttributeDefinitionBuilder(Constants.DEFAULT_SERVER, ModelType.STRING, true)
                    .setRestartAllServices()
                    .setDefaultValue(new ModelNode("default-server"))
                    .setCapabilityReference(UNDERTOW_CAPABILITY, Capabilities.CAPABILITY_SERVER)
                    .build();

    protected static final SimpleAttributeDefinition DEFAULT_VIRTUAL_HOST =
                new SimpleAttributeDefinitionBuilder(Constants.DEFAULT_VIRTUAL_HOST, ModelType.STRING, true)
                        .setRestartAllServices()
                        .setDefaultValue(new ModelNode("default-host"))
                        .setCapabilityReference(UNDERTOW_CAPABILITY, Capabilities.CAPABILITY_HOST, DEFAULT_SERVER)
                        .build();

    protected static final SimpleAttributeDefinition INSTANCE_ID =
            new SimpleAttributeDefinitionBuilder(Constants.INSTANCE_ID, ModelType.STRING, true)
                    .setRestartAllServices()
                    .setAllowExpression(true)
                    .setDefaultValue(new ModelNode().set(new ValueExpression("${jboss.node.name}")))
                    .build();
    protected static final SimpleAttributeDefinition STATISTICS_ENABLED =
            new SimpleAttributeDefinitionBuilder(Constants.STATISTICS_ENABLED, ModelType.BOOLEAN, true)
                    .setRestartAllServices()
                    .setAllowExpression(true)
                    .setDefaultValue(new ModelNode(false))
                    .build();
    protected static final SimpleAttributeDefinition DEFAULT_SECURITY_DOMAIN =
            new SimpleAttributeDefinitionBuilder(Constants.DEFAULT_SECURITY_DOMAIN, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setDefaultValue(new ModelNode(SecurityConstants.DEFAULT_APPLICATION_POLICY))
                    .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.SECURITY_DOMAIN_REF)
                    .setRestartAllServices()
                    .build();


    static final ApplicationSecurityDomainDefinition APPLICATION_SECURITY_DOMAIN = ApplicationSecurityDomainDefinition.INSTANCE;
    static final AttributeDefinition[] ATTRIBUTES = { DEFAULT_VIRTUAL_HOST, DEFAULT_SERVLET_CONTAINER, DEFAULT_SERVER, INSTANCE_ID, STATISTICS_ENABLED, DEFAULT_SECURITY_DOMAIN };
    static final PersistentResourceDefinition[] CHILDREN = {
            ByteBufferPoolDefinition.INSTANCE,
            BufferCacheDefinition.INSTANCE,
            ServerDefinition.INSTANCE,
            ServletContainerDefinition.INSTANCE,
            HandlerDefinitions.INSTANCE,
            FilterDefinitions.INSTANCE,
            APPLICATION_SECURITY_DOMAIN
    };

    public static final UndertowRootDefinition INSTANCE = new UndertowRootDefinition();

    private UndertowRootDefinition() {
        super(UndertowExtension.SUBSYSTEM_PATH,
                UndertowExtension.getResolver(),
                new UndertowSubsystemAdd(APPLICATION_SECURITY_DOMAIN.getKnownSecurityDomainPredicate()),
                ReloadRequiredRemoveStepHandler.INSTANCE);
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Arrays.asList(ATTRIBUTES);
    }

    @Override
    public List<? extends PersistentResourceDefinition> getChildren() {
        return Arrays.asList(CHILDREN);
    }

    @Override
    public void registerCapabilities(ManagementResourceRegistration resourceRegistration) {
        super.registerCapabilities(resourceRegistration);
        resourceRegistration.registerCapability(UNDERTOW_CAPABILITY);
        resourceRegistration.registerCapability(HTTP_INVOKER_RUNTIME_CAPABILITY);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        ReloadRequiredWriteAttributeHandler handler = new ReloadRequiredWriteAttributeHandler(getAttributes());
        for (AttributeDefinition attr : getAttributes()) {
            if (attr == STATISTICS_ENABLED) {
                resourceRegistration.registerReadWriteAttribute(attr, null, new AbstractWriteAttributeHandler<Void>(STATISTICS_ENABLED) {
                    @Override
                    protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode resolvedValue, ModelNode currentValue, HandbackHolder<Void> handbackHolder) throws OperationFailedException {
                        ServiceController<?> controller = context.getServiceRegistry(false).getService(UndertowService.UNDERTOW);
                        if (controller != null) {
                            UndertowService service = (UndertowService) controller.getService();
                            if (service != null) {
                                service.setStatisticsEnabled(resolvedValue.asBoolean());
                            }
                        }
                        return false;
                    }

                    @Override
                    protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode valueToRestore, ModelNode valueToRevert, Void handback) throws OperationFailedException {
                        ServiceController<?> controller = context.getServiceRegistry(false).getService(UndertowService.UNDERTOW);
                        if (controller != null) {
                            UndertowService service = (UndertowService) controller.getService();
                            if (service != null) {
                                service.setStatisticsEnabled(valueToRestore.asBoolean());
                            }
                        }
                    }
                });
            } else {
                resourceRegistration.registerReadWriteAttribute(attr, null, handler);
            }
        }
    }
}
