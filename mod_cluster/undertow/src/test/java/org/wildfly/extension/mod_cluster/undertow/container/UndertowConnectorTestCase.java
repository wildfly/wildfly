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

package org.wildfly.extension.mod_cluster.undertow.container;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.Collections;

import org.jboss.as.network.ClientMapping;
import org.jboss.as.network.NetworkInterfaceBinding;
import org.jboss.as.network.SocketBinding;
import org.jboss.as.network.SocketBindingManager;
import org.jboss.modcluster.container.Connector;
import org.jboss.msc.value.InjectedValue;
import org.junit.Test;
import org.wildfly.extension.mod_cluster.undertow.container.UndertowConnector;
import org.wildfly.extension.undertow.ListenerService;
import org.wildfly.extension.undertow.AjpListenerService;
import org.wildfly.extension.undertow.HttpListenerService;
import org.wildfly.extension.undertow.HttpsListenerService;
import org.xnio.OptionMap;

public class UndertowConnectorTestCase {
    private final ListenerService<?> listener = mock(ListenerService.class);
    private final Connector connector = new UndertowConnector(this.listener);

    @Test
    public void getType() {
        OptionMap options = OptionMap.builder().getMap();
        assertSame(Connector.Type.AJP, new UndertowConnector(new AjpListenerService("", "", options)).getType());
        assertSame(Connector.Type.HTTP, new UndertowConnector(new HttpListenerService("", "", options, false)).getType());
        assertSame(Connector.Type.HTTPS, new UndertowConnector(new HttpsListenerService("", "", options)).getType());
    }

    @Test
    public void getAddress() throws UnknownHostException {
        InetAddress expected = InetAddress.getLocalHost();
        NetworkInterfaceBinding interfaceBinding = new NetworkInterfaceBinding(Collections.<NetworkInterface>emptySet(), expected);
        SocketBindingManager bindingManager = mock(SocketBindingManager.class);
        SocketBinding binding = new SocketBinding("socket", 1, true, null, 0, interfaceBinding, bindingManager, Collections.<ClientMapping>emptyList());
        InjectedValue<SocketBinding> bindingValue = new InjectedValue<SocketBinding>();

        bindingValue.inject(binding);
        when(this.listener.getBinding()).thenReturn(bindingValue);

        InetAddress result = this.connector.getAddress();

        assertSame(expected, result);
    }

    @Test
    public void getPort() throws UnknownHostException {
        int expected = 10;
        NetworkInterfaceBinding interfaceBinding = new NetworkInterfaceBinding(Collections.<NetworkInterface>emptySet(), InetAddress.getLocalHost());
        SocketBindingManager bindingManager = mock(SocketBindingManager.class);
        SocketBinding binding = new SocketBinding("socket", expected, true, null, 0, interfaceBinding, bindingManager, Collections.<ClientMapping>emptyList());
        InjectedValue<SocketBinding> bindingValue = new InjectedValue<SocketBinding>();

        bindingValue.inject(binding);
        when(this.listener.getBinding()).thenReturn(bindingValue);

        int result = this.connector.getPort();

        assertSame(expected, result);
    }

    @Test
    public void setAddress() throws UnknownHostException {
        connector.setAddress(InetAddress.getLocalHost());

        verifyZeroInteractions(this.listener);
    }
}
