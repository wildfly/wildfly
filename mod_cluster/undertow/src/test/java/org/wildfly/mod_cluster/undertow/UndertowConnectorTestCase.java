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
package org.wildfly.mod_cluster.undertow;

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;

import org.jboss.as.network.NetworkInterfaceBinding;
import org.jboss.as.network.SocketBinding;
import org.jboss.as.network.SocketBindingManager;
import org.jboss.modcluster.container.Connector;
import org.junit.Test;
import org.wildfly.extension.undertow.AjpListenerService;
import org.wildfly.extension.undertow.HttpListenerService;
import org.wildfly.extension.undertow.HttpsListenerService;
import org.wildfly.extension.undertow.ListenerService;
import org.xnio.OptionMap;

public class UndertowConnectorTestCase {
    private final ListenerService listener = mock(ListenerService.class);
    private final Connector connector = new UndertowConnector(this.listener);

    @Test
    public void getType() {
        OptionMap options = OptionMap.builder().getMap();
        assertSame(Connector.Type.AJP, new UndertowConnector(new AjpListenerService("", "", options, OptionMap.EMPTY)).getType());
        assertSame(Connector.Type.HTTP, new UndertowConnector(new HttpListenerService("", "", options, OptionMap.EMPTY, false, false, false)).getType());
        assertSame(Connector.Type.HTTPS, new UndertowConnector(new HttpsListenerService("", "", options, null, OptionMap.EMPTY, false)).getType());
    }

    @Test
    public void getAddress() throws UnknownHostException {
        InetAddress expected = InetAddress.getLocalHost();
        NetworkInterfaceBinding interfaceBinding = new NetworkInterfaceBinding(Collections.emptySet(), expected);
        SocketBindingManager bindingManager = mock(SocketBindingManager.class);
        SocketBinding binding = new SocketBinding("socket", 1, true, null, 0, interfaceBinding, bindingManager, Collections.emptyList());

        when(this.listener.getSocketBinding()).thenReturn(binding);

        InetAddress result = this.connector.getAddress();

        assertSame(expected, result);
    }

    @Test
    public void getPort() throws UnknownHostException {
        int expected = 10;
        NetworkInterfaceBinding interfaceBinding = new NetworkInterfaceBinding(Collections.emptySet(), InetAddress.getLocalHost());
        SocketBindingManager bindingManager = mock(SocketBindingManager.class);
        SocketBinding binding = new SocketBinding("socket", expected, true, null, 0, interfaceBinding, bindingManager, Collections.emptyList());

        when(this.listener.getSocketBinding()).thenReturn(binding);

        int result = this.connector.getPort();

        assertSame(expected, result);
    }

    @Test
    public void setAddress() throws UnknownHostException {
        connector.setAddress(InetAddress.getLocalHost());

        verifyZeroInteractions(this.listener);
    }
}
