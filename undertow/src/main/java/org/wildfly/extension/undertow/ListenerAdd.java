/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow;

import static org.wildfly.extension.undertow.AbstractHttpListenerResourceDefinition.REQUIRE_HOST_HTTP11;
import static org.wildfly.extension.undertow.Capabilities.REF_IO_WORKER;
import static org.wildfly.extension.undertow.ListenerResourceDefinition.LISTENER_CAPABILITY;
import static org.wildfly.extension.undertow.ServerDefinition.SERVER_CAPABILITY;
import static org.wildfly.extension.undertow.logging.UndertowLogger.ROOT_LOGGER;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import io.undertow.connector.ByteBufferPool;
import io.undertow.server.handlers.DisallowedMethodsHandler;
import io.undertow.server.handlers.PeerNameResolvingHandler;
import io.undertow.servlet.handlers.MarkSecureHandler;
import io.undertow.util.HttpString;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.CapabilityServiceBuilder;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.network.SocketBinding;
import org.jboss.dmr.ModelNode;
import org.wildfly.extension.io.OptionList;
import org.xnio.OptionMap;
import org.xnio.XnioWorker;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2012 Red Hat Inc.
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
abstract class ListenerAdd<S extends ListenerService> extends AbstractAddStepHandler {

    @Override
    protected void recordCapabilitiesAndRequirements(OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {
        super.recordCapabilitiesAndRequirements(context, operation, resource);

        String ourCap = LISTENER_CAPABILITY.getDynamicName(context.getCurrentAddress());
        String serverCap = SERVER_CAPABILITY.getDynamicName(context.getCurrentAddress().getParent());
        context.registerAdditionalCapabilityRequirement(serverCap, ourCap, null);
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        final PathAddress address = context.getCurrentAddress();
        final PathAddress parent = address.getParent();
        final String name = context.getCurrentAddressValue();
        final String bindingRef = ListenerResourceDefinition.SOCKET_BINDING.resolveModelAttribute(context, model).asString();
        final String workerName = ListenerResourceDefinition.WORKER.resolveModelAttribute(context, model).asString();
        final String bufferPoolName = ListenerResourceDefinition.BUFFER_POOL.resolveModelAttribute(context, model).asString();
        final boolean enabled = ListenerResourceDefinition.ENABLED.resolveModelAttribute(context, model).asBoolean();
        final boolean peerHostLookup = ListenerResourceDefinition.RESOLVE_PEER_ADDRESS.resolveModelAttribute(context, model).asBoolean();
        final boolean secure = ListenerResourceDefinition.SECURE.resolveModelAttribute(context, model).asBoolean();

        OptionMap listenerOptions = OptionList.resolveOptions(context, model, ListenerResourceDefinition.LISTENER_OPTIONS);
        OptionMap socketOptions = OptionList.resolveOptions(context, model, ListenerResourceDefinition.SOCKET_OPTIONS);
        String serverName = parent.getLastElement().getValue();
        final CapabilityServiceBuilder<?> sb = context.getCapabilityServiceTarget().addCapability(ListenerResourceDefinition.LISTENER_CAPABILITY);
        final Consumer<ListenerService> serviceConsumer = sb.provides(ListenerResourceDefinition.LISTENER_CAPABILITY, ListenerResourceDefinition.SERVER_LISTENER_CAPABILITY);
        final S service = createService(serviceConsumer, name, serverName, context, model, listenerOptions,socketOptions);
        if (peerHostLookup) {
            service.addWrapperHandler(PeerNameResolvingHandler::new);
        }
        service.setEnabled(enabled);
        if(secure) {
            service.addWrapperHandler(MarkSecureHandler.WRAPPER);
        }
        List<String> disallowedMethods = ListenerResourceDefinition.DISALLOWED_METHODS.unwrap(context, model);
        if(!disallowedMethods.isEmpty()) {
            final Set<HttpString> methodSet = new HashSet<>();
            for (String i : disallowedMethods) {
                HttpString httpString = new HttpString(i.trim());
                methodSet.add(httpString);
            }
            service.addWrapperHandler(handler -> new DisallowedMethodsHandler(handler, methodSet));
        }

        sb.setInstance(service);
        service.getWorker().set(sb.requiresCapability(REF_IO_WORKER, XnioWorker.class, workerName));
        service.getBinding().set(sb.requires(SocketBinding.SERVICE_DESCRIPTOR, bindingRef));
        service.getBufferPool().set(sb.requiresCapability(Capabilities.CAPABILITY_BYTE_BUFFER_POOL, ByteBufferPool.class, bufferPoolName));
        service.getServerService().set(sb.requires(Server.SERVICE_DESCRIPTOR, serverName));

        configureAdditionalDependencies(context, sb, model, service);
        sb.install();
    }

    abstract S createService(final Consumer<ListenerService> serviceConsumer, final String name, final String serverName, final OperationContext context, ModelNode model, OptionMap listenerOptions, OptionMap socketOptions) throws OperationFailedException;

    abstract void configureAdditionalDependencies(OperationContext context, CapabilityServiceBuilder<?> serviceBuilder, ModelNode model, S service) throws OperationFailedException;



    /**
     * Logs a message if {@link AbstractHttpListenerResourceDefinition#REQUIRE_HOST_HTTP11} has a 'false' value,
     * which no longer takes effect. The log level depends on whether the attribute was explicitly configured (WARN)
     * or just had the default value (INFO). This more forcefully complements any future deprecation of the attribute,
     * which would only result in an INFO log telling the user to check the read-resource-description output for details.
     *
     * @param context the context for an executing operation
     * @param model model node representing a listener resource
     * @throws OperationFailedException if resolving the attribute fails
     */
    static void logRequireHostHttp1Ineffective(OperationContext context, ModelNode model) throws OperationFailedException {
        if (!REQUIRE_HOST_HTTP11.resolveModelAttribute(context, model).asBoolean()) {
            String msg = ROOT_LOGGER.http11HostHeaderRequired(REQUIRE_HOST_HTTP11.getName(), context.getCurrentAddress().toCLIStyleString());
            if (model.hasDefined(REQUIRE_HOST_HTTP11.getName())) {
                ROOT_LOGGER.warn(msg);
            } else {
                ROOT_LOGGER.info(msg);
            }
        }
    }
}
