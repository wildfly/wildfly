/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.mod_cluster.undertow;

import java.util.List;

import org.wildfly.common.function.Functions;
import org.wildfly.extension.undertow.Host;

class TestHost extends Host {
    TestHost(String name, List<String> aliases, TestServer server) {
        super(Functions.discardingConsumer(), Functions.constantSupplier(server), null, null, name, aliases, "ROOT.war", 404, true);
        server.registerHost(this);
    }
}
