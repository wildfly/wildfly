/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.mod_cluster.undertow;

import org.wildfly.common.function.Functions;
import org.wildfly.extension.undertow.Host;
import org.wildfly.extension.undertow.ListenerService;
import org.wildfly.extension.undertow.Server;
import org.wildfly.extension.undertow.UndertowService;

final class TestServer extends Server {
    TestServer(final String name, final String defaultHost) {
        super(null, null, null, name, defaultHost);
    }

    TestServer(final String name, final String defaultHost, final UndertowService service, final Host host, final ListenerService listener) {
        super(Functions.discardingConsumer(), null, () -> service, name, defaultHost);
        this.registerHost(host);
        this.registerListener(listener);
    }
}
