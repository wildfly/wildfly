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
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jboss.as.webservices.logging.WSLogger;
import org.jboss.msc.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.jboss.wsf.spi.metadata.j2ee.serviceref.UnifiedHandlerChainMetaData;
import org.jboss.wsf.spi.metadata.j2ee.serviceref.UnifiedHandlerMetaData;

/**
 * A service for creating handler chain metadata.
 *
 * @author <a href="mailto:alessio.soldano@jboss.com">Alessio Soldano</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class HandlerChainService implements Service {

    private final String handlerChainId;
    private final String protocolBindings;
    private final List<Supplier<UnifiedHandlerMetaData>> handlerSuppliers;
    private final Consumer<UnifiedHandlerChainMetaData> handlerChainConsumer;

    public HandlerChainService(final String handlerChainType, final String handlerChainId, final String protocolBindings,
                               final Consumer<UnifiedHandlerChainMetaData> handlerChainConsumer,
                               final List<Supplier<UnifiedHandlerMetaData>> handlerSuppliers) {
        if (!handlerChainType.equalsIgnoreCase("pre-handler-chain") && !handlerChainType.equals("post-handler-chain")) {
            throw new RuntimeException(
                    WSLogger.ROOT_LOGGER.wrongHandlerChainType(handlerChainType, "pre-handler-chain", "post-handler-chain"));
        }
        this.handlerChainId = handlerChainId;
        this.protocolBindings = protocolBindings;
        this.handlerChainConsumer = handlerChainConsumer;
        this.handlerSuppliers = handlerSuppliers;
    }

    @Override
    public void start(final StartContext context) {
        final List<UnifiedHandlerMetaData> handlers = new ArrayList<>();
        for (final Supplier<UnifiedHandlerMetaData> handlerSupplier : handlerSuppliers) {
            handlers.add(handlerSupplier.get());
        }
        Collections.sort(handlers, HandlersComparator.INSTANCE);
        handlerChainConsumer.accept(new UnifiedHandlerChainMetaData(null, null, protocolBindings, handlers, false, handlerChainId));
    }

    @Override
    public void stop(final StopContext context) {
        handlerChainConsumer.accept(null);
    }

    private static final class HandlersComparator implements Comparator<UnifiedHandlerMetaData> {

        private static final Comparator<UnifiedHandlerMetaData> INSTANCE = new HandlersComparator();

        @Override
        public int compare(final UnifiedHandlerMetaData o1, final UnifiedHandlerMetaData o2) {
            return o1.getId().compareTo(o2.getId());
        }
    }

}
