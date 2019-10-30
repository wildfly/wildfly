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
package org.jboss.as.naming.service;

import org.jboss.as.naming.logging.NamingLogger;
import org.jboss.as.naming.context.external.ExternalContexts;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * A service containing the subsystem's {@link ExternalContexts}.
 * @author Eduardo Martins
 */
public class ExternalContextsService implements Service<ExternalContexts> {

    public static final ServiceName SERVICE_NAME = ContextNames.NAMING.append("externalcontexts");

    private final ExternalContexts externalContexts;
    private volatile boolean started = false;

    public ExternalContextsService(ExternalContexts externalContexts) {
        this.externalContexts = externalContexts;
    }

    @Override
    public void start(StartContext context) throws StartException {
        started = true;
    }

    @Override
    public void stop(StopContext context) {
        started = false;
    }

    @Override
    public ExternalContexts getValue() throws IllegalStateException, IllegalArgumentException {
        if(!started) {
            throw NamingLogger.ROOT_LOGGER.serviceNotStarted(SERVICE_NAME);
        }
        return externalContexts;
    }
}
