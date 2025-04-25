/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.mod_cluster.undertow;

import org.wildfly.common.function.Functions;
import org.wildfly.extension.undertow.Host;
import org.wildfly.extension.undertow.ListenerService;
import org.wildfly.extension.undertow.Server;

final class TestServer extends Server {
    TestServer(final String name, final String defaultHost, TestUndertowService service) {
        super(Functions.discardingConsumer(), null, Functions.constantSupplier(service), name, defaultHost);
        service.registerServer(this);
    }

    @Override
    protected void registerListener(ListenerService listener) {
        super.registerListener(listener);
    }

    @Override
    protected void registerHost(Host host) {
        super.registerHost(host);
    }
}
