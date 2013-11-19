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

package org.jboss.as.protocol;

import org.xnio.IoFuture;

/**
 * An implementation of this interface can be provided by calling clients where they wish to supply their own implementation to
 * handle timeouts whilst establishing a connection.
 *
 * The general purpose of this is for clients that wish to take into account additional factors during connection such as user
 * think time / value entry.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public interface ProtocolTimeoutHandler {

    /**
     * Wait for the specified time on the supplied {@link IoFuture}, taking into account that some of this time could actually
     * not be related to the establishment of the connection but instead some local task such as user think time.
     *
     * @param future - The {@link IoFuture} to wait on.
     * @param timeoutMillis - The configures timeout in milliseconds.
     * @return The {@link IoFuture.Status} when available or at the time the timeout is reached - whichever is soonest.
     */
    IoFuture.Status await(IoFuture<?> future, long timeoutMillis);

}
