/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.mod_cluster.undertow;

import java.util.function.Consumer;

import org.wildfly.extension.undertow.Server;
import org.wildfly.extension.undertow.UndertowService;

public class TestUndertowService extends UndertowService {

    public TestUndertowService(final Consumer<UndertowService> serviceConsumer, String defaultContainer, String defaultServer, String defaultVirtualHost, String instanceId, boolean obfuscateRoute, Server server) {
        super(serviceConsumer, defaultContainer, defaultServer, defaultVirtualHost, instanceId, obfuscateRoute, true);
        this.registerServer(server);
    }
}
