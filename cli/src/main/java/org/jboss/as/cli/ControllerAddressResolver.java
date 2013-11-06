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

/**
 * Utility to resolve the address to use to connect to a controller based on a user supplied address, a default address from the
 * command line, a default address from the configuration and a hard coded default address.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class ControllerAddressResolver {

    private final CliConfig config;
    private final String defaultController;

    private ControllerAddressResolver(final CliConfig config, final String defaultController) {
        this.config = config;
        this.defaultController = defaultController;
    }

    public static ControllerAddressResolver newInstance(final CliConfig config, final String defaultController) {
        return new ControllerAddressResolver(config, defaultController);
    }

    public ControllerAddress resolveAddress(final String controller) throws CommandLineException {
        ControllerAddress intermediate;
        if (controller != null) {
            intermediate = convert(controller);
        } else if (defaultController != null) {
            intermediate = convert(defaultController);
        } else {
            intermediate = config.getDefaultControllerAddress();
        }

        return finish(intermediate);
    }

    private ControllerAddress finish(final ControllerAddress toFinish) throws CommandLineException {
        String protocol = toFinish.getProtocol();
        String host = toFinish.getHost();
        int port = toFinish.getPort();

        if (protocol == null) {
            if (config.isUseLegacyOverride() && port == 9999) {
                protocol = "remoting";
            } else {
                protocol = config.getDefaultControllerProtocol();
            }
        }

        if (host == null) {
            throw new CommandLineException("null host encountered");
        }

        if (port < 0) {
            if ("remote".equals(protocol) || "remoting".equals(protocol)) {
                port = 9999;
            } else if ("http-remoting".equals(protocol)) {
                port = 9990;
            } else if ("https-remoting".equals(protocol)) {
                port = 9993;
            } else {
                throw new CommandLineException("Unexpected protocol '" + protocol + "'");
            }
        }

        return new ControllerAddress(protocol, host, port);
    }

    private ControllerAddress convert(final String controller) throws CommandLineException {
        String protocol = null;
        String host = null;
        int port = -1;

        String parsing;
        final int protocolEnd = controller.lastIndexOf("://");
        if (protocolEnd == -1) {
            parsing = controller;
        } else {
            parsing = controller.substring(protocolEnd + 3);
            protocol = controller.substring(0, protocolEnd);
        }

        String portStr = null;
        int colonIndex = parsing.lastIndexOf(':');
        if (colonIndex < 0) {
            // default port
            host = parsing;
        } else if (colonIndex == 0) {
            // default host
            portStr = parsing.substring(1).trim();
        } else {
            final boolean hasPort;
            int closeBracket = parsing.lastIndexOf(']');
            if (closeBracket != -1) {
                // possible ip v6
                if (closeBracket > colonIndex) {
                    hasPort = false;
                } else {
                    hasPort = true;
                }
            } else {
                // probably ip v4
                hasPort = true;
            }
            if (hasPort) {
                host = parsing.substring(0, colonIndex).trim();
                portStr = parsing.substring(colonIndex + 1).trim();
            } else {
                host = parsing;
            }
        }

        if (portStr != null) {
            try {
                port = Integer.parseInt(portStr);
            } catch (NumberFormatException e) {
                throw new CommandFormatException("The port must be a valid non-negative integer: '" + parsing + "'");
            }
            if (port < 0) {
                throw new CommandFormatException("The port must be a valid non-negative integer: '" + parsing + "'");
            }
        }

        return new ControllerAddress(protocol, host, port);
    }

}
