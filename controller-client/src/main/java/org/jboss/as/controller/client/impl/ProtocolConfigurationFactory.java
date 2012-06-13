/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.controller.client.impl;

import java.net.URI;
import java.net.URISyntaxException;

import org.jboss.as.controller.client.ModelControllerClientConfiguration;
import org.jboss.as.protocol.ProtocolChannelClient;
import org.jboss.remoting3.RemotingOptions;
import org.xnio.OptionMap;

/**
 * Transformation class of the model controller client configuration to the
 * underlying protocol configs.
 *
 * @author Emanuel Muckenhuber
 */
class ProtocolConfigurationFactory {

    private static final OptionMap DEFAULT_OPTIONS = OptionMap.create(RemotingOptions.TRANSMIT_WINDOW_SIZE, ProtocolChannelClient.Configuration.DEFAULT_WINDOW_SIZE,
            RemotingOptions.RECEIVE_WINDOW_SIZE, ProtocolChannelClient.Configuration.DEFAULT_WINDOW_SIZE);

    static ProtocolChannelClient.Configuration create(final ModelControllerClientConfiguration client) throws URISyntaxException {
        final ProtocolChannelClient.Configuration configuration = new ProtocolChannelClient.Configuration();

        configuration.setUri(new URI("remote://" + formatPossibleIpv6Address(client.getHost()) +  ":" + client.getPort()));
        configuration.setOptionMap(DEFAULT_OPTIONS);
        final long timeout = client.getConnectionTimeout();
        if(timeout > 0) {
            configuration.setConnectionTimeout(timeout);
        }
        return configuration;
    }

    private static String formatPossibleIpv6Address(String address) {
        if (address == null) {
            return address;
        }
        if (!address.contains(":")) {
            return address;
        }
        if (address.startsWith("[") && address.endsWith("]")) {
            return address;
        }
        return "[" + address + "]";
    }

}
