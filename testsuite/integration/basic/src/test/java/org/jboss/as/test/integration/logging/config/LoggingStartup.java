/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.logging.config;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;

import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@Singleton
@Startup
public class LoggingStartup {
    static final String LOGGER_NAME = LoggingStartup.class.getName();
    static final String STARTUP_MESSAGE = "Test startup from EJB";
    private final Logger LOGGER = Logger.getLogger(LOGGER_NAME);

    @PostConstruct
    public void logEjbMessage() {
        LOGGER.info(STARTUP_MESSAGE);
    }
}
