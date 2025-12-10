/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.mod_cluster.undertow;

import org.wildfly.common.function.Functions;
import org.wildfly.extension.undertow.Server;
import org.wildfly.extension.undertow.UndertowService;

public class TestUndertowService extends UndertowService {

    public TestUndertowService(String defaultContainer, String defaultServer, String defaultVirtualHost, String instanceId, boolean obfuscateRoute) {
        super(Functions.discardingConsumer(), defaultContainer, defaultServer, defaultVirtualHost, instanceId, obfuscateRoute, true);
    }

    @Override
    protected void registerServer(Server server) {
        super.registerServer(server);
    }
}
