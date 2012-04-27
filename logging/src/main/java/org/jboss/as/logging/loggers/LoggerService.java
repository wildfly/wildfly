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

import java.util.logging.Handler;
import java.util.logging.Level;

import org.jboss.logmanager.Logger;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class LoggerService extends AbstractLoggerService {

    private boolean useParentHandlers = true;

    private Level level;

    private Handler[] saved;

    protected LoggerService(final String name) {
        super(name);
    }

    protected synchronized void start(final StartContext context, final Logger logger) throws StartException {
        logger.setLevel(level);
        logger.setUseParentHandlers(useParentHandlers);
        saved = logger.clearHandlers();
    }

    protected synchronized void stop(final StopContext context, final Logger logger) {
        logger.setLevel(null);
        logger.setUseParentHandlers(true);
        logger.clearHandlers();
        for (Handler handler : saved) {
            logger.addHandler(handler);
        }
    }

    public synchronized boolean isUseParentHandlers() {
        return useParentHandlers;
    }

    public synchronized void setUseParentHandlers(final boolean useParentHandlers) {
        this.useParentHandlers = useParentHandlers;
        final Logger logger = getLogger();
        if (logger != null) {
            logger.setUseParentHandlers(useParentHandlers);
        }
    }

    public synchronized Level getLevel() {
        return level;
    }

    public synchronized void setLevel(final Level level) {
        this.level = level;
        final Logger logger = getLogger();
        if (logger != null) {
            logger.setLevel(level);
        }
    }
}
