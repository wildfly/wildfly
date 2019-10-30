/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
