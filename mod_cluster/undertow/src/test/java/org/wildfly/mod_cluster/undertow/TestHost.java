/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.mod_cluster.undertow;

import java.util.List;

import org.wildfly.extension.undertow.Host;
import org.wildfly.extension.undertow.Server;
import org.wildfly.extension.undertow.UndertowService;

class TestHost extends Host {
    TestHost(String name, List<String> aliases, UndertowService service, Server server) {
        super(null, () -> server, () -> service, null, null, name, aliases, "ROOT.war", 404, true);
    }
}
