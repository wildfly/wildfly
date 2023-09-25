/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.logging.config;

import jakarta.enterprise.context.ApplicationScoped;

import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@ApplicationScoped
public class LoggerResource {

    static final String LOGGER_NAME = LoggerResource.class.getName();
    private static final Logger LOGGER = Logger.getLogger(LOGGER_NAME);

    public void logStatic(final String msg) {
        LOGGER.info(formatStaticLogMsg(msg));
    }

    public void log(final String msg) {
        Logger.getLogger(LOGGER_NAME).info(formatLogMsg(msg));
    }

    static String formatStaticLogMsg(final String msg) {
        return String.format("%s - static resource", msg);
    }

    static String formatLogMsg(final String msg) {
        return String.format("%s - resource", msg);
    }
}
