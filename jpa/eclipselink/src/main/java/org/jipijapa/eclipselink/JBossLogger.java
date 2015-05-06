/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
