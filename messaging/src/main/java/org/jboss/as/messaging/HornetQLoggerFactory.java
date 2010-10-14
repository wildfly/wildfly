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

package org.jboss.as.messaging;

import org.hornetq.spi.core.logging.LogDelegate;
import org.hornetq.spi.core.logging.LogDelegateFactory;
import org.jboss.logging.Logger;
import org.jboss.logging.Logger.Level;

/**
 * @author Emanuel Muckenhuber
 */
public class HornetQLoggerFactory implements LogDelegateFactory {

    /** {@inheritDoc} */
    public LogDelegate createDelegate(Class<?> clazz) {
        return new HornetQLogDelegate(clazz);
    }

    static class HornetQLogDelegate implements LogDelegate {
        private final Logger delegate;

        HornetQLogDelegate(Class<?> clazz) {
            delegate = Logger.getLogger(clazz);
        }

        public boolean isEnabled(Level level) {
            return delegate.isEnabled(level);
        }

        public int hashCode() {
            return delegate.hashCode();
        }

        public boolean isTraceEnabled() {
            return delegate.isTraceEnabled();
        }

        public boolean equals(Object obj) {
            return delegate.equals(obj);
        }

        public void trace(Object message) {
            delegate.trace(message);
        }

        public void trace(Object message, Throwable t) {
            delegate.trace(message, t);
        }

        public void trace(String loggerFqcn, Object message, Throwable t) {
            delegate.trace(loggerFqcn, message, t);
        }

        public void trace(String loggerFqcn, Object message, Object[] params, Throwable t) {
            delegate.trace(loggerFqcn, message, params, t);
        }

        public void tracev(String format, Object... params) {
            delegate.tracev(format, params);
        }

        public void tracev(String format, Object param1) {
            delegate.tracev(format, param1);
        }

        public void tracev(String format, Object param1, Object param2) {
            delegate.tracev(format, param1, param2);
        }

        public void tracev(String format, Object param1, Object param2, Object param3) {
            delegate.tracev(format, param1, param2, param3);
        }

        public void tracev(Throwable t, String format, Object... params) {
            delegate.tracev(t, format, params);
        }

        public void tracev(Throwable t, String format, Object param1) {
            delegate.tracev(t, format, param1);
        }

        public void tracev(Throwable t, String format, Object param1, Object param2) {
            delegate.tracev(t, format, param1, param2);
        }

        public void tracev(Throwable t, String format, Object param1, Object param2, Object param3) {
            delegate.tracev(t, format, param1, param2, param3);
        }

        public String toString() {
            return delegate.toString();
        }

        public void tracef(String format, Object... params) {
            delegate.tracef(format, params);
        }

        public void tracef(String format, Object param1) {
            delegate.tracef(format, param1);
        }

        public void tracef(String format, Object param1, Object param2) {
            delegate.tracef(format, param1, param2);
        }

        public void tracef(String format, Object param1, Object param2, Object param3) {
            delegate.tracef(format, param1, param2, param3);
        }

        public void tracef(Throwable t, String format, Object... params) {
            delegate.tracef(t, format, params);
        }

        public void tracef(Throwable t, String format, Object param1) {
            delegate.tracef(t, format, param1);
        }

        public void tracef(Throwable t, String format, Object param1, Object param2) {
            delegate.tracef(t, format, param1, param2);
        }

        public void tracef(Throwable t, String format, Object param1, Object param2, Object param3) {
            delegate.tracef(t, format, param1, param2, param3);
        }

        public boolean isDebugEnabled() {
            return delegate.isDebugEnabled();
        }

        public void debug(Object message) {
            delegate.debug(message);
        }

        public void debug(Object message, Throwable t) {
            delegate.debug(message, t);
        }

        public void debug(String loggerFqcn, Object message, Throwable t) {
            delegate.debug(loggerFqcn, message, t);
        }

        public void debug(String loggerFqcn, Object message, Object[] params, Throwable t) {
            delegate.debug(loggerFqcn, message, params, t);
        }

        public void debugv(String format, Object... params) {
            delegate.debugv(format, params);
        }

        public void debugv(String format, Object param1) {
            delegate.debugv(format, param1);
        }

        public void debugv(String format, Object param1, Object param2) {
            delegate.debugv(format, param1, param2);
        }

        public void debugv(String format, Object param1, Object param2, Object param3) {
            delegate.debugv(format, param1, param2, param3);
        }

        public void debugv(Throwable t, String format, Object... params) {
            delegate.debugv(t, format, params);
        }

        public void debugv(Throwable t, String format, Object param1) {
            delegate.debugv(t, format, param1);
        }

        public void debugv(Throwable t, String format, Object param1, Object param2) {
            delegate.debugv(t, format, param1, param2);
        }

        public void debugv(Throwable t, String format, Object param1, Object param2, Object param3) {
            delegate.debugv(t, format, param1, param2, param3);
        }

        public void debugf(String format, Object... params) {
            delegate.debugf(format, params);
        }

        public void debugf(String format, Object param1) {
            delegate.debugf(format, param1);
        }

        public void debugf(String format, Object param1, Object param2) {
            delegate.debugf(format, param1, param2);
        }

        public void debugf(String format, Object param1, Object param2, Object param3) {
            delegate.debugf(format, param1, param2, param3);
        }

        public void debugf(Throwable t, String format, Object... params) {
            delegate.debugf(t, format, params);
        }

        public void debugf(Throwable t, String format, Object param1) {
            delegate.debugf(t, format, param1);
        }

        public void debugf(Throwable t, String format, Object param1, Object param2) {
            delegate.debugf(t, format, param1, param2);
        }

        public void debugf(Throwable t, String format, Object param1, Object param2, Object param3) {
            delegate.debugf(t, format, param1, param2, param3);
        }

        public boolean isInfoEnabled() {
            return delegate.isInfoEnabled();
        }

        public void info(Object message) {
            delegate.info(message);
        }

        public void info(Object message, Throwable t) {
            delegate.info(message, t);
        }

        public void info(String loggerFqcn, Object message, Throwable t) {
            delegate.info(loggerFqcn, message, t);
        }

        public void info(String loggerFqcn, Object message, Object[] params, Throwable t) {
            delegate.info(loggerFqcn, message, params, t);
        }

        public void infov(String format, Object... params) {
            delegate.infov(format, params);
        }

        public void infov(String format, Object param1) {
            delegate.infov(format, param1);
        }

        public void infov(String format, Object param1, Object param2) {
            delegate.infov(format, param1, param2);
        }

        public void infov(String format, Object param1, Object param2, Object param3) {
            delegate.infov(format, param1, param2, param3);
        }

        public void infov(Throwable t, String format, Object... params) {
            delegate.infov(t, format, params);
        }

        public void infov(Throwable t, String format, Object param1) {
            delegate.infov(t, format, param1);
        }

        public void infov(Throwable t, String format, Object param1, Object param2) {
            delegate.infov(t, format, param1, param2);
        }

        public void infov(Throwable t, String format, Object param1, Object param2, Object param3) {
            delegate.infov(t, format, param1, param2, param3);
        }

        public void infof(String format, Object... params) {
            delegate.infof(format, params);
        }

        public void infof(String format, Object param1) {
            delegate.infof(format, param1);
        }

        public void infof(String format, Object param1, Object param2) {
            delegate.infof(format, param1, param2);
        }

        public void infof(String format, Object param1, Object param2, Object param3) {
            delegate.infof(format, param1, param2, param3);
        }

        public void infof(Throwable t, String format, Object... params) {
            delegate.infof(t, format, params);
        }

        public void infof(Throwable t, String format, Object param1) {
            delegate.infof(t, format, param1);
        }

        public void infof(Throwable t, String format, Object param1, Object param2) {
            delegate.infof(t, format, param1, param2);
        }

        public void infof(Throwable t, String format, Object param1, Object param2, Object param3) {
            delegate.infof(t, format, param1, param2, param3);
        }

        public void warn(Object message) {
            delegate.warn(message);
        }

        public void warn(Object message, Throwable t) {
            delegate.warn(message, t);
        }

        public void warn(String loggerFqcn, Object message, Throwable t) {
            delegate.warn(loggerFqcn, message, t);
        }

        public void warn(String loggerFqcn, Object message, Object[] params, Throwable t) {
            delegate.warn(loggerFqcn, message, params, t);
        }

        public void warnv(String format, Object... params) {
            delegate.warnv(format, params);
        }

        public void warnv(String format, Object param1) {
            delegate.warnv(format, param1);
        }

        public void warnv(String format, Object param1, Object param2) {
            delegate.warnv(format, param1, param2);
        }

        public void warnv(String format, Object param1, Object param2, Object param3) {
            delegate.warnv(format, param1, param2, param3);
        }

        public void warnv(Throwable t, String format, Object... params) {
            delegate.warnv(t, format, params);
        }

        public void warnv(Throwable t, String format, Object param1) {
            delegate.warnv(t, format, param1);
        }

        public void warnv(Throwable t, String format, Object param1, Object param2) {
            delegate.warnv(t, format, param1, param2);
        }

        public void warnv(Throwable t, String format, Object param1, Object param2, Object param3) {
            delegate.warnv(t, format, param1, param2, param3);
        }

        public void warnf(String format, Object... params) {
            delegate.warnf(format, params);
        }

        public void warnf(String format, Object param1) {
            delegate.warnf(format, param1);
        }

        public void warnf(String format, Object param1, Object param2) {
            delegate.warnf(format, param1, param2);
        }

        public void warnf(String format, Object param1, Object param2, Object param3) {
            delegate.warnf(format, param1, param2, param3);
        }

        public void warnf(Throwable t, String format, Object... params) {
            delegate.warnf(t, format, params);
        }

        public void warnf(Throwable t, String format, Object param1) {
            delegate.warnf(t, format, param1);
        }

        public void warnf(Throwable t, String format, Object param1, Object param2) {
            delegate.warnf(t, format, param1, param2);
        }

        public void warnf(Throwable t, String format, Object param1, Object param2, Object param3) {
            delegate.warnf(t, format, param1, param2, param3);
        }

        public void error(Object message) {
            delegate.error(message);
        }

        public void error(Object message, Throwable t) {
            delegate.error(message, t);
        }

        public void error(String loggerFqcn, Object message, Throwable t) {
            delegate.error(loggerFqcn, message, t);
        }

        public void error(String loggerFqcn, Object message, Object[] params, Throwable t) {
            delegate.error(loggerFqcn, message, params, t);
        }

        public void errorv(String format, Object... params) {
            delegate.errorv(format, params);
        }

        public void errorv(String format, Object param1) {
            delegate.errorv(format, param1);
        }

        public void errorv(String format, Object param1, Object param2) {
            delegate.errorv(format, param1, param2);
        }

        public void errorv(String format, Object param1, Object param2, Object param3) {
            delegate.errorv(format, param1, param2, param3);
        }

        public void errorv(Throwable t, String format, Object... params) {
            delegate.errorv(t, format, params);
        }

        public void errorv(Throwable t, String format, Object param1) {
            delegate.errorv(t, format, param1);
        }

        public void errorv(Throwable t, String format, Object param1, Object param2) {
            delegate.errorv(t, format, param1, param2);
        }

        public void errorv(Throwable t, String format, Object param1, Object param2, Object param3) {
            delegate.errorv(t, format, param1, param2, param3);
        }

        public void errorf(String format, Object... params) {
            delegate.errorf(format, params);
        }

        public void errorf(String format, Object param1) {
            delegate.errorf(format, param1);
        }

        public void errorf(String format, Object param1, Object param2) {
            delegate.errorf(format, param1, param2);
        }

        public void errorf(String format, Object param1, Object param2, Object param3) {
            delegate.errorf(format, param1, param2, param3);
        }

        public void errorf(Throwable t, String format, Object... params) {
            delegate.errorf(t, format, params);
        }

        public void errorf(Throwable t, String format, Object param1) {
            delegate.errorf(t, format, param1);
        }

        public void errorf(Throwable t, String format, Object param1, Object param2) {
            delegate.errorf(t, format, param1, param2);
        }

        public void errorf(Throwable t, String format, Object param1, Object param2, Object param3) {
            delegate.errorf(t, format, param1, param2, param3);
        }

        public void fatal(Object message) {
            delegate.fatal(message);
        }

        public void fatal(Object message, Throwable t) {
            delegate.fatal(message, t);
        }

        public void fatal(String loggerFqcn, Object message, Throwable t) {
            delegate.fatal(loggerFqcn, message, t);
        }

        public void fatal(String loggerFqcn, Object message, Object[] params, Throwable t) {
            delegate.fatal(loggerFqcn, message, params, t);
        }

        public void fatalv(String format, Object... params) {
            delegate.fatalv(format, params);
        }

        public void fatalv(String format, Object param1) {
            delegate.fatalv(format, param1);
        }

        public void fatalv(String format, Object param1, Object param2) {
            delegate.fatalv(format, param1, param2);
        }

        public void fatalv(String format, Object param1, Object param2, Object param3) {
            delegate.fatalv(format, param1, param2, param3);
        }

        public void fatalv(Throwable t, String format, Object... params) {
            delegate.fatalv(t, format, params);
        }

        public void fatalv(Throwable t, String format, Object param1) {
            delegate.fatalv(t, format, param1);
        }

        public void fatalv(Throwable t, String format, Object param1, Object param2) {
            delegate.fatalv(t, format, param1, param2);
        }

        public void fatalv(Throwable t, String format, Object param1, Object param2, Object param3) {
            delegate.fatalv(t, format, param1, param2, param3);
        }

        public void fatalf(String format, Object... params) {
            delegate.fatalf(format, params);
        }

        public void fatalf(String format, Object param1) {
            delegate.fatalf(format, param1);
        }

        public void fatalf(String format, Object param1, Object param2) {
            delegate.fatalf(format, param1, param2);
        }

        public void fatalf(String format, Object param1, Object param2, Object param3) {
            delegate.fatalf(format, param1, param2, param3);
        }

        public void fatalf(Throwable t, String format, Object... params) {
            delegate.fatalf(t, format, params);
        }

        public void fatalf(Throwable t, String format, Object param1) {
            delegate.fatalf(t, format, param1);
        }

        public void fatalf(Throwable t, String format, Object param1, Object param2) {
            delegate.fatalf(t, format, param1, param2);
        }

        public void fatalf(Throwable t, String format, Object param1, Object param2, Object param3) {
            delegate.fatalf(t, format, param1, param2, param3);
        }

        public void log(Level level, Object message) {
            delegate.log(level, message);
        }

        public void log(Level level, Object message, Throwable t) {
            delegate.log(level, message, t);
        }

        public void log(Level level, String loggerFqcn, Object message, Throwable t) {
            delegate.log(level, loggerFqcn, message, t);
        }

        public void log(String loggerFqcn, Level level, Object message, Object[] params, Throwable t) {
            delegate.log(loggerFqcn, level, message, params, t);
        }

        public void logv(Level level, String format, Object... params) {
            delegate.logv(level, format, params);
        }

        public void logv(Level level, String format, Object param1) {
            delegate.logv(level, format, param1);
        }

        public void logv(Level level, String format, Object param1, Object param2) {
            delegate.logv(level, format, param1, param2);
        }

        public void logv(Level level, String format, Object param1, Object param2, Object param3) {
            delegate.logv(level, format, param1, param2, param3);
        }

        public void logv(Level level, Throwable t, String format, Object... params) {
            delegate.logv(level, t, format, params);
        }

        public void logv(Level level, Throwable t, String format, Object param1) {
            delegate.logv(level, t, format, param1);
        }

        public void logv(Level level, Throwable t, String format, Object param1, Object param2) {
            delegate.logv(level, t, format, param1, param2);
        }

        public void logv(Level level, Throwable t, String format, Object param1, Object param2, Object param3) {
            delegate.logv(level, t, format, param1, param2, param3);
        }

        public void logv(String loggerFqcn, Level level, Throwable t, String format, Object... params) {
            delegate.logv(loggerFqcn, level, t, format, params);
        }

        public void logv(String loggerFqcn, Level level, Throwable t, String format, Object param1) {
            delegate.logv(loggerFqcn, level, t, format, param1);
        }

        public void logv(String loggerFqcn, Level level, Throwable t, String format, Object param1, Object param2) {
            delegate.logv(loggerFqcn, level, t, format, param1, param2);
        }

        public void logv(String loggerFqcn, Level level, Throwable t, String format, Object param1, Object param2, Object param3) {
            delegate.logv(loggerFqcn, level, t, format, param1, param2, param3);
        }

        public void logf(Level level, String format, Object... params) {
            delegate.logf(level, format, params);
        }

        public void logf(Level level, String format, Object param1) {
            delegate.logf(level, format, param1);
        }

        public void logf(Level level, String format, Object param1, Object param2) {
            delegate.logf(level, format, param1, param2);
        }

        public void logf(Level level, String format, Object param1, Object param2, Object param3) {
            delegate.logf(level, format, param1, param2, param3);
        }

        public void logf(Level level, Throwable t, String format, Object... params) {
            delegate.logf(level, t, format, params);
        }

        public void logf(Level level, Throwable t, String format, Object param1) {
            delegate.logf(level, t, format, param1);
        }

        public void logf(Level level, Throwable t, String format, Object param1, Object param2) {
            delegate.logf(level, t, format, param1, param2);
        }

        public void logf(Level level, Throwable t, String format, Object param1, Object param2, Object param3) {
            delegate.logf(level, t, format, param1, param2, param3);
        }

        public void logf(String loggerFqcn, Level level, Throwable t, String format, Object param1) {
            delegate.logf(loggerFqcn, level, t, format, param1);
        }

        public void logf(String loggerFqcn, Level level, Throwable t, String format, Object param1, Object param2) {
            delegate.logf(loggerFqcn, level, t, format, param1, param2);
        }

        public void logf(String loggerFqcn, Level level, Throwable t, String format, Object param1, Object param2, Object param3) {
            delegate.logf(loggerFqcn, level, t, format, param1, param2, param3);
        }

        public void logf(String loggerFqcn, Level level, Throwable t, String format, Object... params) {
            delegate.logf(loggerFqcn, level, t, format, params);
        }
    }

}
