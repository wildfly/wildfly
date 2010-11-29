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
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;

import java.util.logging.Level;

/**
 * @author Emanuel Muckenhuber
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public class LoggerAdd extends AbstractLoggerAdd {

    private static final long serialVersionUID = 4230922005791983261L;

    private final String name;

    private boolean useParentHandlers = true;

    public LoggerAdd(final String name, final boolean useParentHandlers) {
        this.name = name;
        this.useParentHandlers = useParentHandlers;
    }

    public LoggerAdd(final String name) {
        this.name = name;
    }

    protected AbstractLoggerElement<?> addNewElement(final LoggingSubsystemElement element) throws UpdateFailedException {
        final LoggerElement newElement = new LoggerElement(name);
        newElement.setUseParentHandlers(useParentHandlers);
        if (!element.addLogger(newElement)) {
            throw new UpdateFailedException("Logger " + name + " already exists");
        }
        return newElement;
    }

    /**
     * {@inheritDoc}
     */
    protected <P> void applyUpdate(UpdateContext updateContext, UpdateResultHandler<? super Void, P> resultHandler, P param) {
        try {
            final String loggerName = getLoggerName();
            final LoggerService service = new LoggerService(loggerName);
            service.setLevel(Level.parse(getLevelName()));
            final BatchBuilder batchBuilder = updateContext.getBatchBuilder();
            final ServiceBuilder<Logger> builder = batchBuilder.addService(LogServices.loggerName(loggerName), service);
            builder.setInitialMode(ServiceController.Mode.ACTIVE);
            builder.addListener(new UpdateResultHandler.ServiceStartListener<P>(resultHandler, param));
        } catch (Throwable t) {
            resultHandler.handleFailure(t, param);
            return;
        }
    }

    public boolean isUseParentHandlers() {
        return useParentHandlers;
    }

    public void setUseParentHandlers(final boolean useParentHandlers) {
        this.useParentHandlers = useParentHandlers;
    }

    public String getLoggerName() {
        return name;
    }
}
