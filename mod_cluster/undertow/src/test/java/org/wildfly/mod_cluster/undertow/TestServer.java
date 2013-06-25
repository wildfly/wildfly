package org.wildfly.mod_cluster.undertow;

import org.wildfly.extension.undertow.AbstractListenerService;
import org.wildfly.extension.undertow.Host;
import org.wildfly.extension.undertow.Server;

public class TestServer extends Server {
    public TestServer(String name, String defaultHost) {
        super(name, defaultHost);
    }

    public TestServer(String name, String defaultHost, Host host, AbstractListenerService<?> listener) {
        this(name, defaultHost);
        this.registerHost(host);
        this.registerListener(listener);
    }
}
