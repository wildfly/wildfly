/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.webservices.config;
/**
 * Class to parse the virtualHost name with server instance info like "host@server"
 * @author <a herf="mailto:ema@redhat.com>Jim Ma</a>
 *
 */
public class ServerHostInfo {
    private String host = null;
    private String serverInstanceName = null;

    public ServerHostInfo(final String hostAtServer) {
        host = hostAtServer;
        serverInstanceName = null;
        if (hostAtServer != null) {
            int tokenLoc = hostAtServer.lastIndexOf("@");
            if (tokenLoc > 0 && tokenLoc != hostAtServer.length() - 1) {
                host = hostAtServer.substring(0, tokenLoc);
                serverInstanceName = hostAtServer.substring(tokenLoc + 1);
            }
        }
    }
    /**
     * @return host parsed part "host" from passed in virtualHost string like "host@server"
     */
    public String getHost() {
        return host;
    }
    /**
     * @return return parsed server instance part "sever" from passed in virtualHost string like "host@server"
     */
    public String getServerInstanceName() {
        return serverInstanceName;
    }
}
