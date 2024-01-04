/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.mod_cluster.undertow;

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.network.NetworkInterfaceBinding;
import org.jboss.as.network.SocketBinding;
import org.jboss.as.network.SocketBindingManager;
import org.jboss.modcluster.container.Connector;
import org.junit.Test;
import org.wildfly.extension.undertow.AjpListenerService;
import org.wildfly.extension.undertow.Constants;
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
        assertSame(Connector.Type.AJP, new UndertowConnector(new AjpListenerService(null, PathAddress.pathAddress(Constants.AJP_LISTENER, "dummy"), "", options, OptionMap.EMPTY)).getType());
        assertSame(Connector.Type.HTTP, new UndertowConnector(new HttpListenerService(null, PathAddress.pathAddress(Constants.HTTP_LISTENER, "dummy"), "", options, OptionMap.EMPTY, false, false, false)).getType());
        assertSame(Connector.Type.HTTPS, new UndertowConnector(new HttpsListenerService(null, PathAddress.pathAddress(Constants.HTTPS_LISTENER, "dummy"), "", options, null, OptionMap.EMPTY, false)).getType());
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

        verifyNoMoreInteractions(this.listener);
    }
}
