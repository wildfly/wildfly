/*
 * Copyright 2020 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.as.test.integration.logging.config;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;

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
