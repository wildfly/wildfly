package org.wildfly.mod_cluster.undertow;

import java.util.List;

import org.wildfly.extension.undertow.Host;
import org.wildfly.extension.undertow.Server;
import org.wildfly.extension.undertow.UndertowService;

public class TestHost extends Host {
    public TestHost(String name, List<String> aliases, UndertowService service, Server server) {
        super(name, aliases);
        this.getUndertowService().inject(service);
        this.getServerInjection().inject(server);
    }
}
