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
import org.jboss.msc.service.BatchServiceBuilder;
import org.jboss.msc.service.ServiceController;

import java.util.logging.Level;

/**
 * @author Emanuel Muckenhuber
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public class LoggerAdd extends AbstractLoggingSubsystemUpdate<Void> {

    private static final long serialVersionUID = 4230922005791983261L;

    private final String name;

    private Level level;
    private boolean useParentHandlers = true;
    private FilterType filter;
    private String[] handlers;

    public LoggerAdd(final String name, final Level level, final boolean useParentHandlers) {
        this.name = name;
        this.level = level;
        this.useParentHandlers = useParentHandlers;
    }

    public LoggerAdd(final String name) {
        this.name = name;
    }

    /** {@inheritDoc} */
    protected <P> void applyUpdate(UpdateContext updateContext, UpdateResultHandler<? super Void, P> resultHandler, P param) {
        final LoggerService service = new LoggerService(name);
        service.setLevel(level);
        service.setUseParentHandlers(useParentHandlers);
        final BatchServiceBuilder<Logger> builder = updateContext.getBatchBuilder().addService(LogServices.getLoggerName(name), service);
        builder.setInitialMode(ServiceController.Mode.IMMEDIATE);
    }

    /** {@inheritDoc} */
    public LoggerRemove getCompensatingUpdate(LoggingSubsystemElement original) {
        return new LoggerRemove(name);
    }

    /** {@inheritDoc} */
    protected void applyUpdate(LoggingSubsystemElement element) throws UpdateFailedException {
        final LoggerElement logger = new LoggerElement(name);
        logger.setLevel(level);
        logger.setUseParentHandlers(useParentHandlers);
        if(! element.addLogger(logger)) {
            throw new UpdateFailedException("duplicate logger " + name);
        }
    }

    public Level getLevel() {
        return level;
    }

    public void setLevel(final Level level) {
        this.level = level;
    }

    public boolean isUseParentHandlers() {
        return useParentHandlers;
    }

    public void setUseParentHandlers(final boolean useParentHandlers) {
        this.useParentHandlers = useParentHandlers;
    }

    public FilterType getFilter() {
        return filter;
    }

    public void setFilter(final FilterType filter) {
        this.filter = filter;
    }

    public String[] getHandlers() {
        return handlers;
    }

    public void setHandlers(final String... handlers) {
        this.handlers = handlers;
    }
}
