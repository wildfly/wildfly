/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow;

import org.jboss.as.network.SocketBinding;

/**
 * Represents the externally accessible interface provided by Undertow's listeners
 *
 * @author Stuart Douglas
 */
public interface UndertowListener {

    /**
     * Returns the listeners socket binding.
     *
     * @return The listeners socket binding
     */
    SocketBinding getSocketBinding();

    /**
     * Returns true if the listener is secure. In general this will be true for HTTPS listeners, however other listener
     * types may have been explicitly marked as secure.
     *
     * @return <code>true</code> if the listener is considered security
     */
    boolean isSecure();

    /**
     * Returns the transport protocol. This will generally either be http, https or ajp.
     *
     * @return The transport protocol
     */
    String getProtocol();

    /**
     * Returns the listener name
     *
     * @return The listener name
     */
    String getName();

    /**
     * Returns the server this listener is registered with.
     *
     * @return the server this listener is registered with
     */
    Server getServer();

    /**
     * Returns true if the listener has shut down.
     *
     * @return <code>true</code> if the listener has been shutdown
     */
    boolean isShutdown();
}
