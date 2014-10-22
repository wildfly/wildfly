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
package org.jboss.as.webservices.service;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.wsf.spi.metadata.j2ee.serviceref.UnifiedHandlerMetaData;

/**
 * A service for creating handler metadata.
 *
 * @author <a href="mailto:alessio.soldano@jboss.com">Alessio Soldano</a>
 */
public final class HandlerService implements Service<UnifiedHandlerMetaData> {

    private final String handlerName;
    private final String handlerClass;
    private final int counter;
    private volatile UnifiedHandlerMetaData handler;

    public HandlerService(String handlerName, String handlerClass, int counter) {
        this.handlerName = handlerName;
        this.handlerClass = handlerClass;
        this.counter = counter;
    }

    @Override
    public UnifiedHandlerMetaData getValue() {
        return handler;
    }

    @Override
    public void start(final StartContext context) throws StartException {
        handler = new UnifiedHandlerMetaData(handlerClass, handlerName, null, null, null, null, String.valueOf(counter));
    }

    @Override
    public void stop(final StopContext context) {
        handler = null;
    }
}
