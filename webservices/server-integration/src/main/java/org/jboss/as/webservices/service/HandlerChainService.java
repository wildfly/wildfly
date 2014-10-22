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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.jboss.as.webservices.dmr.ListInjector;
import org.jboss.as.webservices.logging.WSLogger;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.wsf.spi.metadata.j2ee.serviceref.UnifiedHandlerChainMetaData;
import org.jboss.wsf.spi.metadata.j2ee.serviceref.UnifiedHandlerMetaData;

/**
 * A service for creating handler chain metadata.
 *
 * @author <a href="mailto:alessio.soldano@jboss.com">Alessio Soldano</a>
 */
public final class HandlerChainService implements Service<UnifiedHandlerChainMetaData> {

    private final List<UnifiedHandlerMetaData> handlers = new ArrayList<UnifiedHandlerMetaData>(2);
    private final String handlerChainId;
    private final String protocolBindings;
    private volatile UnifiedHandlerChainMetaData handlerChain;

    public HandlerChainService(String handlerChainType, String handlerChainId, String protocolBindings) {
        if (!handlerChainType.equalsIgnoreCase("pre-handler-chain") && !handlerChainType.equals("post-handler-chain")) {
            throw new RuntimeException(
                    WSLogger.ROOT_LOGGER.wrongHandlerChainType(handlerChainType, "pre-handler-chain", "post-handler-chain"));
        }
        this.handlerChainId = handlerChainId;
        this.protocolBindings = protocolBindings;
    }

    @Override
    public UnifiedHandlerChainMetaData getValue() {
        return handlerChain;
    }

    @Override
    public void start(final StartContext context) throws StartException {
        Comparator<UnifiedHandlerMetaData> c = new Comparator<UnifiedHandlerMetaData>() {
            @Override
            public int compare(UnifiedHandlerMetaData o1, UnifiedHandlerMetaData o2) {
                return o1.getId().compareTo(o2.getId());
            }
        };
        synchronized (handlers) {
            Collections.sort(handlers, c);
        }
        handlerChain = new UnifiedHandlerChainMetaData(null, null, protocolBindings, handlers, false, handlerChainId);
    }

    @Override
    public void stop(final StopContext context) {
        handlerChain = null;
    }

    public Injector<UnifiedHandlerMetaData> getHandlersInjector() {
        return new ListInjector<UnifiedHandlerMetaData>(handlers);
    }
}
