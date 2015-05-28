/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.jipijapa.eclipselink;

import java.util.HashMap;
import java.util.Map;


import org.eclipse.persistence.logging.AbstractSessionLog;
import org.eclipse.persistence.logging.SessionLogEntry;
import org.jboss.logging.Logger;
import org.jboss.logging.Logger.Level;

/**
 * JBossLogger integrates Eclipse's logging with JBoss logger, so you can
 * configure log levels via the server and get the same log formatting
 * as everything else.
 * <p/>
 * See https://community.jboss.org/wiki/HowToUseEclipseLinkWithAS7
 */
public class JBossLogger extends AbstractSessionLog {

    private static final String ORG_ECLIPSE_PERSISTENCE = "org.eclipse.persistence";

    private Map<String, Logger> loggers = new HashMap<String, Logger>();

    @Override
    public void log(SessionLogEntry sessionLogEntry) {
        Logger logger = getLoggerForCategory(sessionLogEntry.getNameSpace());
        Level level = convertLevelIntToEnum(sessionLogEntry.getLevel());
        String message = formatMessage(sessionLogEntry);

        logger.log(level, message);
    }

    @Override
    public boolean shouldLog(int level, String category) {
        return getLoggerForCategory(category).isEnabled(convertLevelIntToEnum(level));
    }

    private Logger getLoggerForCategory(String category) {
        Logger logger = loggers.get(category);
        if (logger == null) {
            logger = Logger.getLogger(ORG_ECLIPSE_PERSISTENCE, (category == null ? "" : category));
            loggers.put(category, logger);
        }
        return logger;
    }


    private Level convertLevelIntToEnum(int level) {
        switch (level) {
            case SEVERE:
                return Level.FATAL;
            case WARNING:
                return Level.WARN;
            case CONFIG:
            case INFO:
                return Level.INFO;
            case FINE:
                return Level.DEBUG;
            case FINER:
            case FINEST:
                return Level.TRACE;
            default:
                getLoggerForCategory("logging").warnv("Received message for log level {0}, but no such level is defined in SessionLog! Logging at INFO level...", level);
                return Level.INFO;
        }
    }

    /**
     * Show bound parameters in EclipseLink logging (JIPI-5)
     *
     * @return true to show bind parameters
     */
    @Override
    public boolean shouldDisplayData() {
        if (shouldDisplayData == null) {
            return getLoggerForCategory("sql").isDebugEnabled();
        } else {
            return shouldDisplayData.booleanValue();
        }
    }
}
