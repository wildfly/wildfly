/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.domain.management.util;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.protocol.ProtocolChannelClient;
import org.jboss.remoting3.Endpoint;
import org.jboss.remoting3.Remoting;
import org.jboss.remoting3.remote.RemoteConnectionProviderFactory;
import org.xnio.OptionMap;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;

/**
 * @author Emanuel Muckenhuber
 */
public class SharedTestClientConfiguration implements Closeable {

    static final String ENDPOINT_NAME = "mgmt-test-client";
    private final Endpoint endpoint;

    SharedTestClientConfiguration(Endpoint endpoint) {
        this.endpoint = endpoint;
    }

    public ModelControllerClient createControllerClient(final URI connectionURI) throws IOException {

        final ProtocolChannelClient.Configuration configuration = new ProtocolChannelClient.Configuration();
        configuration.setEndpoint(endpoint);
        configuration.setUri(connectionURI);
        final ProtocolChannelClient client = ProtocolChannelClient.create(configuration);

        return null;
    }

    @Override
    public void close() throws IOException {
        if(endpoint != null) try {
            endpoint.close();
        } catch (IOException e) {

        }
    }

    public static SharedTestClientConfiguration create() throws IOException {
        final Endpoint endpoint = Remoting.createEndpoint(ENDPOINT_NAME, OptionMap.EMPTY);
        endpoint.addConnectionProvider("remote", new RemoteConnectionProviderFactory(), OptionMap.EMPTY);
        return new SharedTestClientConfiguration(endpoint);
    }

}
