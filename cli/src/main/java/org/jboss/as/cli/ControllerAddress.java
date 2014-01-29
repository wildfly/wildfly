/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.as.cli;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * A class used both by the {@link ControllerAddressResolver} and by the configuration to represent the address of a controller.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public final class ControllerAddress {

    private final String protocol;
    private final String host;
    private final int port;

    private final int hashCode;

    public ControllerAddress(final String protocol, final String host, final int port) {
        this.protocol = protocol;
        this.host = host;
        this.port = port;

        hashCode = (protocol == null ? 3 : protocol.hashCode()) * (host == null ? 5 : host.hashCode()) * port;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    @Override
    public String toString() {
        try {
            return new URI(protocol, null, host, port, null, null, null).toString();
        } catch (URISyntaxException e) {
            return protocol + "://" + host + ":" + port;
        }
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ControllerAddress ? equals((ControllerAddress) other) : false;
    }

    public boolean equals(ControllerAddress other) {
        return (protocol == null ? other.protocol == null : protocol.equals(other.protocol))
                && (host == null ? other.host == null : host.equals(other.host)) && port == other.port;
    }

}