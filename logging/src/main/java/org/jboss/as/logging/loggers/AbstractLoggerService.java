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

package org.jboss.as.logging.loggers;

import static org.jboss.as.logging.LoggingMessages.MESSAGES;

import java.util.logging.Filter;

import org.jboss.as.logging.CommonAttributes;
import org.jboss.logmanager.Logger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
abstract class AbstractLoggerService implements Service<Logger> {

    private final String name;

    private Logger logger;
    private Filter filter;

    protected AbstractLoggerService(final String name) {
        if (CommonAttributes.ROOT_LOGGER_NAME.equals(name)) {
            this.name = "";
        } else {
            this.name = name;
        }
    }

    protected final String getName() {
        return name;
    }

    public final synchronized void start(final StartContext context) throws StartException {
        logger = Logger.getLogger(getName());
        if (filter != null) {
            logger.setFilter(filter);
        }
        start(context, logger);
    }

    protected abstract void start(final StartContext context, final Logger logger) throws StartException;

    public final synchronized void stop(final StopContext context) {
        stop(context, getLogger());
    }

    protected abstract void stop(final StopContext context, final Logger logger);

    protected synchronized Logger getLogger() {
        return logger;
    }

    public synchronized Logger getValue() throws IllegalStateException {
        return notNull(getLogger());
    }

    public synchronized void setFilter(final Filter filter) {
        this.filter = filter;
        final Logger logger = this.logger;
        if (logger != null) {
            logger.setFilter(filter);
        }
    }

    private static <T> T notNull(T value) {
        if (value == null) throw MESSAGES.serviceNotStarted();
        return value;
    }
}
