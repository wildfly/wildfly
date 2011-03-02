/**
 *
 */
package org.jboss.as.domain.controller;

/**
 * Simple data object combining a server name with the name of the server's server group.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
class GroupedServer {

    private final String serverName;
    private final String serverGroupName;

    GroupedServer(final String serverName, final String serverGroupName) {
        if (serverName == null) {
            throw new IllegalArgumentException("serverName is null");
        }
        if (serverGroupName == null) {
            throw new IllegalArgumentException("serverGroupName is null");
        }
        this.serverGroupName = serverGroupName;
        this.serverName = serverName;
    }

    public String getServerName() {
        return serverName;
    }

    public String getServerGroupName() {
        return serverGroupName;
    }

    /**
     * Returns the hash code of the server name.
     */
    @Override
    public int hashCode() {
        return serverName.hashCode();
    }

    /**
     * Returns {@code true} if {@code obj} is a GroupedServer with the same server name.
     */
    @Override
    public boolean equals(Object obj) {
        return obj instanceof GroupedServer && serverName.equals(((GroupedServer) obj).serverName);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{serverName=" + serverName + ", serverGroupName=" + serverGroupName + "}";
    }
}
