/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
package org.jboss.as.clustering.jgroups;

import org.jboss.logging.Logger;
import org.jboss.logging.Logger.Level;
import org.jgroups.logging.CustomLogFactory;
import org.jgroups.logging.Log;

/**
 * Temporary workaround for JGRP-1475.
 * @author Paul Ferraro
 */
public class LogFactory implements CustomLogFactory {

    @Override
    public Log getLog(Class<?> clazz) {
        return new LogAdapter(Logger.getLogger(clazz));
    }

    @Override
    public Log getLog(String category) {
        return new LogAdapter(Logger.getLogger(category));
    }

    private static class LogAdapter implements Log {
        private final Logger logger;

        LogAdapter(Logger logger) {
            this.logger = logger;
        }

        @Override
        public boolean isFatalEnabled() {
            return this.logger.isEnabled(Level.FATAL);
        }

        @Override
        public boolean isErrorEnabled() {
            return this.logger.isEnabled(Level.ERROR);
        }

        @Override
        public boolean isWarnEnabled() {
            return this.logger.isEnabled(Level.WARN);
        }

        @Override
        public boolean isInfoEnabled() {
            return this.logger.isInfoEnabled();
        }

        @Override
        public boolean isDebugEnabled() {
            return this.logger.isDebugEnabled();
        }

        @Override
        public boolean isTraceEnabled() {
            return this.logger.isTraceEnabled();
        }

        @Override
        public void fatal(String msg) {
            this.logger.fatal(msg);
        }

        @Override
        public void fatal(String msg, Object... args) {
            this.logger.fatalf(msg, args);
        }

        @Override
        public void fatal(String msg, Throwable throwable) {
            this.logger.fatal(msg, throwable);
        }

        @Override
        public void error(String msg) {
            this.logger.error(msg);
        }

        @Override
        public void error(String msg, Object... args) {
            this.logger.errorf(msg, args);
        }

        @Override
        public void error(String msg, Throwable throwable) {
            this.logger.error(msg, throwable);
        }

        @Override
        public void warn(String msg) {
            this.logger.warn(msg);
        }

        @Override
        public void warn(String msg, Object... args) {
            this.logger.warnf(msg, args);
        }

        @Override
        public void warn(String msg, Throwable throwable) {
            this.logger.warn(msg, throwable);
        }

        @Override
        public void info(String msg) {
            this.logger.info(msg);
        }

        @Override
        public void info(String msg, Object... args) {
            this.logger.infof(msg, args);
        }

        @Override
        public void debug(String msg) {
            this.logger.debug(msg);
        }

        @Override
        public void debug(String msg, Object... args) {
            this.logger.debugf(msg, args);
        }

        @Override
        public void debug(String msg, Throwable throwable) {
            this.logger.debug(msg, throwable);
        }

        @Override
        public void trace(Object msg) {
            this.logger.trace(msg);
        }

        @Override
        public void trace(String msg) {
            this.logger.trace(msg);
        }

        @Override
        public void trace(String msg, Object... args) {
            this.logger.tracef(msg, args);
        }

        @Override
        public void trace(String msg, Throwable throwable) {
            this.logger.trace(msg, throwable);
        }

        @Override
        public void setLevel(String level) {
            // Unsupported
        }

        @Override
        public String getLevel() {
            return null;
        }
    }
}
