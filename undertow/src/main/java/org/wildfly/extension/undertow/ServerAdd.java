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
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2013 Red Hat Inc.
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class ServerAdd extends AbstractAddStepHandler {

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {
        PathAddress address = context.getCurrentAddress();
        final ModelNode parentModel = context.readResourceFromRoot(address.getParent(), false).getModel();

        final String name = context.getCurrentAddressValue();
        final String defaultHost = ServerDefinition.DEFAULT_HOST.resolveModelAttribute(context, resource.getModel()).asString();
        final String servletContainer = ServerDefinition.SERVLET_CONTAINER.resolveModelAttribute(context, resource.getModel()).asString();
        final String defaultServerName = UndertowRootDefinition.DEFAULT_SERVER.resolveModelAttribute(context, parentModel).asString();

        boolean isDefaultServer = name.equals(defaultServerName);
        final CapabilityServiceBuilder<?> sb = context.getCapabilityServiceTarget().addCapability(SERVER_CAPABILITY);
        final Consumer<Server> sConsumer = isDefaultServer ? sb.provides(SERVER_CAPABILITY, UndertowService.DEFAULT_SERVER) : sb.provides(SERVER_CAPABILITY);
        final Supplier<ServletContainerService> scsSupplier = sb.requiresCapability(Capabilities.CAPABILITY_SERVLET_CONTAINER, ServletContainerService.class, servletContainer);
        final Supplier<UndertowService> usSupplier = sb.requiresCapability(Capabilities.CAPABILITY_UNDERTOW, UndertowService.class);
        sb.setInstance(new Server(sConsumer, scsSupplier, usSupplier, name, defaultHost));
        sb.install();

        for (ServerServiceInstallerProvider factory : ServiceLoader.load(ServerServiceInstallerProvider.class, ServerServiceInstallerProvider.class.getClassLoader())) {
            factory.getServiceInstaller(name).install(context);
        }
    }
}
