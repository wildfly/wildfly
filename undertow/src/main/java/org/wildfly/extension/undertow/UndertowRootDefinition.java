/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.wildfly.extension.undertow.Capabilities.CAPABILITY_HTTP_INVOKER;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import io.undertow.server.handlers.PathHandler;
import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.ValueExpression;
import org.wildfly.common.function.ExceptionFunction;
import org.wildfly.extension.undertow.filters.FilterDefinitions;
import org.wildfly.extension.undertow.handlers.HandlerDefinitions;
import org.wildfly.service.capture.FunctionExecutor;
import org.wildfly.subsystem.resource.capability.CapabilityReferenceRecorder;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.capture.FunctionExecutorRegistry;
import org.wildfly.subsystem.service.capture.ServiceValueExecutorRegistry;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2012 Red Hat Inc.
 */
class UndertowRootDefinition extends SimpleResourceDefinition {

    static final PathElement PATH_ELEMENT = PathElement.pathElement(SUBSYSTEM, UndertowExtension.SUBSYSTEM_NAME);
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
                    .setCapabilityReference(CapabilityReferenceRecorder.builder(UNDERTOW_CAPABILITY, Server.SERVICE_DESCRIPTOR).build())
                    .build();

    protected static final SimpleAttributeDefinition DEFAULT_VIRTUAL_HOST =
                new SimpleAttributeDefinitionBuilder(Constants.DEFAULT_VIRTUAL_HOST, ModelType.STRING, true)
                        .setRestartAllServices()
                        .setDefaultValue(new ModelNode("default-host"))
                        .setCapabilityReference(CapabilityReferenceRecorder.builder(UNDERTOW_CAPABILITY, Host.SERVICE_DESCRIPTOR).withParentAttribute(DEFAULT_SERVER).build())
                        .build();

    protected static final SimpleAttributeDefinition INSTANCE_ID =
            new SimpleAttributeDefinitionBuilder(Constants.INSTANCE_ID, ModelType.STRING, true)
                    .setRestartAllServices()
                    .setAllowExpression(true)
                    .setDefaultValue(new ModelNode().set(new ValueExpression("${jboss.node.name}")))
                    .build();
    protected static final SimpleAttributeDefinition OBFUSCATE_SESSION_ROUTE =
            new SimpleAttributeDefinitionBuilder(Constants.OBFUSCATE_SESSION_ROUTE, ModelType.BOOLEAN, true)
                    .setRestartAllServices()
                    .setAllowExpression(true)
                    .setDefaultValue(ModelNode.FALSE)
                    .build();
    protected static final SimpleAttributeDefinition STATISTICS_ENABLED =
            new SimpleAttributeDefinitionBuilder(Constants.STATISTICS_ENABLED, ModelType.BOOLEAN, true)
                    .setRestartAllServices()
                    .setAllowExpression(true)
                    .setDefaultValue(ModelNode.FALSE)
                    .build();
    protected static final SimpleAttributeDefinition DEFAULT_SECURITY_DOMAIN =
            new SimpleAttributeDefinitionBuilder(Constants.DEFAULT_SECURITY_DOMAIN, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setDefaultValue(new ModelNode("other"))
                    .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.SECURITY_DOMAIN_REF)
                    .setRestartAllServices()
                    .build();


    static final Collection<AttributeDefinition> ATTRIBUTES = List.of(DEFAULT_VIRTUAL_HOST, DEFAULT_SERVLET_CONTAINER, DEFAULT_SERVER, INSTANCE_ID,
            OBFUSCATE_SESSION_ROUTE, STATISTICS_ENABLED, DEFAULT_SECURITY_DOMAIN);

    private final FunctionExecutorRegistry<UndertowService> registry;
    private final Set<String> knownApplicationSecurityDomains;

    UndertowRootDefinition() {
        this(new CopyOnWriteArraySet<>(), ServiceValueExecutorRegistry.newInstance());
    }

    private UndertowRootDefinition(Set<String> knownApplicationSecurityDomains, ServiceValueExecutorRegistry<UndertowService> registry) {
        super(new SimpleResourceDefinition.Parameters(PATH_ELEMENT, UndertowExtension.getResolver())
                .setAddHandler(new UndertowSubsystemAdd(knownApplicationSecurityDomains::contains, registry))
                .setRemoveHandler(ReloadRequiredRemoveStepHandler.INSTANCE)
                .addCapabilities(UNDERTOW_CAPABILITY, HTTP_INVOKER_RUNTIME_CAPABILITY)
        );
        this.knownApplicationSecurityDomains = knownApplicationSecurityDomains;
        this.registry = registry;
    }

    @Override
    public void registerChildren(ManagementResourceRegistration registration) {
        registration.registerSubModel(new ByteBufferPoolDefinition());
        registration.registerSubModel(new BufferCacheDefinition());
        registration.registerSubModel(new ServerDefinition());
        registration.registerSubModel(new ServletContainerDefinition());
        registration.registerSubModel(new HandlerDefinitions());
        registration.registerSubModel(new FilterDefinitions());
        registration.registerSubModel(new ApplicationSecurityDomainDefinition(this.knownApplicationSecurityDomains));
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        for (AttributeDefinition attr : ATTRIBUTES) {
            if (attr == STATISTICS_ENABLED) {
                ExceptionFunction<UndertowService, UndertowService, RuntimeException> identity = service -> service;
                resourceRegistration.registerReadWriteAttribute(attr, null, new AbstractWriteAttributeHandler<Void>() {
                    @Override
                    protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode resolvedValue, ModelNode currentValue, HandbackHolder<Void> handbackHolder) throws OperationFailedException {
                        FunctionExecutor<UndertowService> executor = UndertowRootDefinition.this.registry.getExecutor(ServiceDependency.on(UndertowRootDefinition.UNDERTOW_CAPABILITY.getCapabilityServiceName()));
                        if (executor != null) {
                            executor.execute(identity).setStatisticsEnabled(resolvedValue.asBoolean());
                        }
                        return false;
                    }

                    @Override
                    protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode valueToRestore, ModelNode valueToRevert, Void handback) throws OperationFailedException {
                        FunctionExecutor<UndertowService> executor = UndertowRootDefinition.this.registry.getExecutor(ServiceDependency.on(UndertowRootDefinition.UNDERTOW_CAPABILITY.getCapabilityServiceName()));
                        if (executor != null) {
                            executor.execute(identity).setStatisticsEnabled(valueToRestore.asBoolean());
                        }
                    }
                });
            } else {
                resourceRegistration.registerReadWriteAttribute(attr, null, ReloadRequiredWriteAttributeHandler.INSTANCE);
            }
        }
    }
}
