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

package org.jboss.as.logging;

import org.jboss.as.model.UpdateContext;
import org.jboss.as.model.UpdateFailedException;
import org.jboss.as.model.UpdateResultHandler;
import org.jboss.logmanager.Logger;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.BatchServiceBuilder;

import java.util.logging.Handler;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class LoggerHandlerAdd extends AbstractLoggingSubsystemUpdate<Void> {

    private static final long serialVersionUID = -7863090205710498113L;

    private final String loggerName;

    private final String handlerName;

    public LoggerHandlerAdd(final String loggerName, final String handlerName) {
        this.loggerName = loggerName;
        this.handlerName = handlerName;
    }

    public String getLoggerName() {
        return loggerName;
    }

    public String getHandlerName() {
        return handlerName;
    }

    protected <P> void applyUpdate(final UpdateContext updateContext, final UpdateResultHandler<? super Void, P> handler, final P param) {
        try {
            final LoggerHandlerService service = new LoggerHandlerService(loggerName);
            final BatchServiceBuilder<Logger> serviceBuilder = updateContext.getBatchBuilder().addService(LogServices.loggerHandlerName(loggerName, handlerName), service);
            serviceBuilder.addDependency(LogServices.loggerName(loggerName));
            final Injector<Handler> injector = service.getHandlerInjector();
            serviceBuilder.addDependency(LogServices.handlerName(handlerName), Handler.class, injector);
            serviceBuilder.addListener(new UpdateResultHandler.ServiceStartListener<P>(handler, param));
        } catch (Throwable t) {
            handler.handleFailure(t, param);
            return;
        }
    }

    public AbstractLoggingSubsystemUpdate<Void> getCompensatingUpdate(final LoggingSubsystemElement original) {
        return new LoggerHandlerRemove(loggerName, handlerName);
    }

    protected void applyUpdate(final LoggingSubsystemElement element) throws UpdateFailedException {
        final AbstractLoggerElement<?> logger = loggerName.length() == 0 ? element.getRootLogger() : element.getLogger(loggerName);
        if (logger == null) {
            throw new UpdateFailedException("No logger element with a name of '" + loggerName + "' exists");
        }
        logger.getHandlers().add(handlerName);
    }
}
