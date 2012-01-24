package org.jboss.as.messaging.deployment;

import org.jboss.dmr.ModelNode;

/**
 * @author Stuart Douglas
 */
class JmsDestination {

    private final String server;

    private final String name;

    private final ModelNode destination;

    JmsDestination(final ModelNode destination, final String server, final String name) {
        this.destination = destination;
        this.server = server;
        this.name = name;
    }

    public String getServer() {
        return server;
    }

    public ModelNode getDestination() {
        return destination;
    }

    public String getName() {
        return name;
    }
}
