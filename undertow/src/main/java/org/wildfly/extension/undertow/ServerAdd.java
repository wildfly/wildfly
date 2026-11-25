/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow;

import static org.wildfly.extension.undertow.ServerDefinition.SERVER_CAPABILITY;

import java.util.ServiceLoader;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.CapabilityServiceBuilder;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.capability.CapabilityServiceInstaller;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2013 Red Hat Inc.
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class ServerAdd extends AbstractAddStepHandler {

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {
        final String name = context.getCurrentAddressValue();
        final String defaultHost = ServerDefinition.DEFAULT_HOST.resolveModelAttribute(context, resource.getModel()).asString();
        final String servletContainer = ServerDefinition.SERVLET_CONTAINER.resolveModelAttribute(context, resource.getModel()).asString();

        final CapabilityServiceBuilder<?> sb = context.getCapabilityServiceTarget().addCapability(SERVER_CAPABILITY);
        final Consumer<Server> sConsumer = sb.provides(SERVER_CAPABILITY);
        final Supplier<ServletContainerService> scsSupplier = sb.requiresCapability(Capabilities.CAPABILITY_SERVLET_CONTAINER, ServletContainerService.class, servletContainer);
        final Supplier<UndertowService> usSupplier = sb.requiresCapability(Capabilities.CAPABILITY_UNDERTOW, UndertowService.class);
        sb.setInstance(new Server(sConsumer, scsSupplier, usSupplier, name, defaultHost));
        sb.install();

        CapabilityServiceInstaller.BlockingBuilder.of(ServerDefinition.DEFAULT_HOST_CAPABILITY, ServiceDependency.on(Host.SERVICE_DESCRIPTOR, name, defaultHost)).build().install(context);

        for (ServerServiceInstallerProvider factory : ServiceLoader.load(ServerServiceInstallerProvider.class, ServerServiceInstallerProvider.class.getClassLoader())) {
            factory.getServiceInstaller(name).install(context);
        }
    }
}
