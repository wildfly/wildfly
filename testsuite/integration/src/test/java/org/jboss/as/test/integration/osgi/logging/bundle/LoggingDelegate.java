/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.test.integration.osgi.logging.bundle;

/**
 * This class asserts that logging messages sent to the various logging systems will appear in the JBoss logging
 * system. This is checked by asserting that the logging implementation classes are the ones expected.
 *
 * @author thomas.diesler@jboss.com
 * @author David Bosschaert
 */
public class LoggingDelegate {
    public static void assertJBossLogging(String message) {
        org.jboss.logging.Logger log = org.jboss.logging.Logger.getLogger(LoggingDelegate.class);

        String loggerClass = log.getClass().getName();

        if ("org.jboss.logging.JBossLogManagerLogger".equals(loggerClass) == false)
            throw new IllegalStateException("Unexpected logger: " + loggerClass);

        log.info("*******************************************");
        log.info("* jboss: " + message);
        log.info("*******************************************");
    }

    public static void assertCommonsLogging(String message) {
        org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(LoggingDelegate.class);

        String loggerClass = log.getClass().getName();

        if ("org.apache.commons.logging.impl.SLF4JLocationAwareLog".equals(loggerClass) == false)
            throw new IllegalStateException("Unexpected logger: " + loggerClass);

        log.info("*******************************************");
        log.info("* jcl: " + message);
        log.info("*******************************************");
    }

    public static void assertSLF4J(String message) {
        org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LoggingDelegate.class);

        String loggerClass = log.getClass().getName();

        if ("org.slf4j.impl.Slf4jLogger".equals(loggerClass) == false)
            throw new IllegalStateException("Unexpected logger: " + loggerClass);

        log.info("*******************************************");
        log.info("* slf4j: " + message);
        log.info("*******************************************");
    }

    public static void assertJULLogging(String message) {
        java.util.logging.Logger log = java.util.logging.Logger.getLogger(LoggingDelegate.class.getName());

        String loggerClass = log.getClass().getName();

        if ("org.jboss.logmanager.Logger".equals(loggerClass) == false)
            throw new IllegalStateException("Unexpected logger: " + loggerClass);

        log.info("*******************************************");
        log.info("* JUL: " + message);
        log.info("*******************************************");
    }

    public static void assertLog4JLogging(String message) {
        org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(LoggingDelegate.class);

        String loggerClass = log.getClass().getName();

        if ("org.jboss.logmanager.log4j.BridgeLogger".equals(loggerClass) == false)
            throw new IllegalStateException("Unexpected logger: " + loggerClass);

        log.info("*******************************************");
        log.info("* Log4J: " + message);
        log.info("*******************************************");

    }
}