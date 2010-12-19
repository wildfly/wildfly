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

import org.jboss.logging.Logger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * TODO add class javadoc for ShutdownHandlerImpl.
 *
 * @author Brian Stansberry
 */
public class ShutdownHandlerImpl implements ShutdownHandler, Service<ShutdownHandler> {

    private static Logger logger = Logger.getLogger("org.jboss.as.server");

    // ShutdownHandler

    @Override
    public void gracefulShutdownRequested(long gracefulShutdownTimeout, TimeUnit timeUnit) {
        logger.error("Graceful shutdown requested but not implemented", new UnsupportedOperationException("Graceful shutdown handling not implemented"));
    }

    @Override
    public void shutdownRequested() {
        logger.error("Shutdown handling requested but not implemented", new UnsupportedOperationException("Shutdown handling not implemented"));
    }

    // Service

    @Override
    public void start(StartContext context) throws StartException {
        // no-op
    }

    @Override
    public void stop(StopContext context) {
        // no-op
    }

    @Override
    public ShutdownHandler getValue() throws IllegalStateException {
        return this;
    }

}
