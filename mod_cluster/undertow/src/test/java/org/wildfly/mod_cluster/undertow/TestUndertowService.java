package org.wildfly.mod_cluster.undertow;

import org.wildfly.extension.undertow.Server;
import org.wildfly.extension.undertow.UndertowService;

public class TestUndertowService extends UndertowService {

    public TestUndertowService(String defaultContainer, String defaultServer, String defaultVirtualHost, String instanceId, Server server) {
        super(defaultContainer, defaultServer, defaultVirtualHost, instanceId);
        this.registerServer(server);
    }
}
