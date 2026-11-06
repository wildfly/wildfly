/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.vertx;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.impl.VertxBuilder;
import io.vertx.core.spi.resolver.ResolverProvider;
import org.jboss.as.controller.CapabilityServiceBuilder;
import org.jboss.as.controller.OperationContext;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.wildfly.extension.vertx.logging.VertxLogger;
import org.wildfly.security.manager.WildFlySecurityManager;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.wildfly.extension.vertx.AbstractVertxOptionsResourceDefinition.VERTX_OPTIONS_CAPABILITY;
import static org.wildfly.extension.vertx.VertxResourceDefinition.VERTX_RUNTIME_CAPABILITY;

/**
 * The msc service to initialize a Vertx instance.
 *
 * @author <a href="aoingl@gmail.com">Lin Gao</a>
 */
public class VertxProxyService implements Service, VertxConstants {

    static {
        if (WildFlySecurityManager.getPropertyPrivileged(ResolverProvider.DISABLE_DNS_RESOLVER_PROP_NAME, null) == null) {
            WildFlySecurityManager.setPropertyPrivileged(ResolverProvider.DISABLE_DNS_RESOLVER_PROP_NAME, "true");
        }
    }
    private VertxProxy vertxProxy;
    private final String optionName;
    private final Supplier<NamedVertxOptions> optionsSupplier;
    final Consumer<VertxProxy> vertxProxytConsumer;

    static void installService(OperationContext context, String optionName) {
        CapabilityServiceBuilder<?> vertxServiceBuilder = context.getCapabilityServiceTarget().addService();
        Consumer<VertxProxy> vertxProxytConsumer = vertxServiceBuilder.provides(VERTX_RUNTIME_CAPABILITY);
        final Supplier<NamedVertxOptions> optionsSupplier;
        if (optionName == null) {
            optionsSupplier = () -> NamedVertxOptions.DEFAULT;
        } else {
            optionsSupplier = vertxServiceBuilder.requiresCapability(VERTX_OPTIONS_CAPABILITY.getName(), NamedVertxOptions.class, optionName);
        }
        VertxProxyService vertxProxyService = new VertxProxyService(optionName, optionsSupplier, vertxProxytConsumer);
        vertxServiceBuilder.setInstance(vertxProxyService);
        vertxServiceBuilder.setInitialMode(ServiceController.Mode.ACTIVE);
        vertxServiceBuilder.install();
    }

    public VertxProxyService(String optionName, Supplier<NamedVertxOptions> optionsSupplier,
                             Consumer<VertxProxy> vertxProxytConsumer) {
        this.optionName = optionName;
        this.optionsSupplier = optionsSupplier;
        this.vertxProxytConsumer = vertxProxytConsumer;
    }
    @Override
    public void start(StartContext context) throws StartException {
        try {
            this.vertxProxy = new VertxProxy(optionName, createVertx());
            VertxProxyHolder.instance().instrument(this.vertxProxy);
        } catch (Exception e) {
            throw VertxLogger.VERTX_LOGGER.failedToStartVertxService(e);
        }
        vertxProxytConsumer.accept(vertxProxy);
    }

    private Vertx createVertx() {
        VertxOptions vertxOptions = optionsSupplier.get().getVertxOptions();
        if (vertxOptions == null) {
            vertxOptions = new VertxOptions();
        }
        return new VertxBuilder(vertxOptions).init().vertx();
    }

    @Override
    public void stop(StopContext context) {
        VertxProxyHolder.instance().release();
        if (this.vertxProxy != null) {
            CompletableFuture<Void> closeFuture = (CompletableFuture<Void>)this.vertxProxy.getVertx().close()
                    .toCompletionStage();
            try {
                closeFuture.join();
                this.vertxProxy = null;
            } catch (Exception e) {
                VertxLogger.VERTX_LOGGER.errorWhenClosingVertx(e);
            }
        }
    }

}
