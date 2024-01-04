/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.webservices.service;

import java.util.function.Consumer;

import org.jboss.msc.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.jboss.wsf.spi.metadata.j2ee.serviceref.UnifiedHandlerMetaData;

/**
 * A service for creating handler metadata.
 *
 * @author <a href="mailto:alessio.soldano@jboss.com">Alessio Soldano</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class HandlerService implements Service {

    private final String handlerName;
    private final String handlerClass;
    private final int counter;
    private final Consumer<UnifiedHandlerMetaData> handlerConsumer;

    public HandlerService(final String handlerName, final String handlerClass, final int counter, final Consumer<UnifiedHandlerMetaData> handlerConsumer) {
        this.handlerName = handlerName;
        this.handlerClass = handlerClass;
        this.counter = counter;
        this.handlerConsumer = handlerConsumer;
    }

    @Override
    public void start(final StartContext context) {
        handlerConsumer.accept(new UnifiedHandlerMetaData(handlerClass, handlerName, null, null, null, null, String.valueOf(counter)));
    }

    @Override
    public void stop(final StopContext context) {
        handlerConsumer.accept(null);
    }
}
