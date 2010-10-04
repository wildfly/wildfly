/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.server.mgmt;

import java.util.concurrent.TimeUnit;

import org.jboss.msc.service.ServiceName;

/**
 * Service that handles requests to shut down a server.
 *
 * @author Brian Stansberry
 */
public interface ShutdownHandler {

    ServiceName SERVICE_NAME = ServiceName.JBOSS.append("shutdown-handler");

    /**
     * Requests that the server be shut down, without necessarily waiting for
     * potentially long-running in-process work to complete before shutting
     * down.
     */
    void shutdownRequested();

    /**
     * Requests that the server shut itself down gracefully, waiting for
     * potentially long-running in-process work to complete before shutting
     * down. See the full JBoss AS documentation for details on what "waiting for
     * in-process work to complete" means.
     *
     * @param gracefulShutdownTimeout maximum time to wait for long-running
     *             in-process work to complete before proceeding with the
     *             shutdown anyway
     * @param timeUnit time unit in which {@code gracefulShutdownTimeout} is
     *                 expressed
     */
    void gracefulShutdownRequested(long gracefulShutdownTimeout, TimeUnit timeUnit);
}
