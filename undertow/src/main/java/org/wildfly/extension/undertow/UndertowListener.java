/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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
