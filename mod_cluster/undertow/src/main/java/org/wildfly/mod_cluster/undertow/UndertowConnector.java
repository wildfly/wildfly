/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.mod_cluster.undertow;

import java.net.InetAddress;

import org.jboss.modcluster.container.Connector;
import org.wildfly.extension.undertow.AjpListenerService;
import org.wildfly.extension.undertow.HttpListenerService;
import org.wildfly.extension.undertow.UndertowListener;
import org.wildfly.mod_cluster.undertow.metric.BytesReceivedStreamSourceConduit;
import org.wildfly.mod_cluster.undertow.metric.BytesSentStreamSinkConduit;
import org.wildfly.mod_cluster.undertow.metric.RequestCountHttpHandler;
import org.wildfly.mod_cluster.undertow.metric.RunningRequestsHttpHandler;

/**
 * Adapts {@link UndertowListener} to a {@link Connector}.
 *
 * @author Radoslav Husar
 * @since 8.0
 */
public class UndertowConnector implements Connector {

    private final UndertowListener listener;
    private InetAddress address;

    public UndertowConnector(UndertowListener listener) {
        this.listener = listener;
    }

    @Override
    public boolean isReverse() {
        return false;
    }

    @Override
    public Type getType() {
        if (this.listener instanceof AjpListenerService) {
            return Type.AJP;
        } else if (this.listener instanceof HttpListenerService) {
            if (this.listener.isSecure()) {
                return Type.HTTPS;
            } else {
                return Type.HTTP;
            }
        }
        return null;
    }

    @Override
    public InetAddress getAddress() {
        return address == null ? this.listener.getSocketBinding().getAddress() : address;
    }

    @Override
    public void setAddress(InetAddress address) {
        this.address = address;
    }

    @Override
    public int getPort() {
        return this.listener.getSocketBinding().getAbsolutePort();
    }

    @Override
    public boolean isAvailable() {
        return !this.listener.isShutdown();
    }

    /**
     * @return int value "-1" to indicate this connector does not maintain corresponding maximum number of threads; capacity
     *         needs to be defined instead
     */
    @Override
    public int getMaxThreads() {
        return -1;
    }

    /**
     * @return int number of <em>running requests</em> on all connectors as opposed to busy threads
     */
    @Override
    public int getBusyThreads() {
        return RunningRequestsHttpHandler.getRunningRequestCount();
    }

    /**
     * @return long number of bytes sent on all connectors
     */
    @Override
    public long getBytesSent() {
        return BytesSentStreamSinkConduit.getBytesSent();
    }

    /**
     * @return long number of bytes received on all listeners without HTTP request size itself
     */
    @Override
    public long getBytesReceived() {
        return BytesReceivedStreamSourceConduit.getBytesReceived();
    }

    /**
     * @return long number of requests on all listeners as opposed to only this 'connector'
     */
    @Override
    public long getRequestCount() {
        return RequestCountHttpHandler.getRequestCount();
    }

    @Override
    public String toString() {
        return this.listener.getName();
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof UndertowConnector)) return false;

        UndertowConnector connector = (UndertowConnector) object;
        return this.listener.getName().equals(connector.listener.getName());
    }

    @Override
    public int hashCode() {
        return this.listener.getName().hashCode();
    }
}
