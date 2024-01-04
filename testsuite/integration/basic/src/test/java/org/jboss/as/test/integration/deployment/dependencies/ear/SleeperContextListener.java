/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.deployment.dependencies.ear;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

import org.jboss.logging.Logger;

/**
 * A lifecycle hook for the test web-app.
 *
 * @author Josef Cacek
 */
public class SleeperContextListener implements ServletContextListener {

    private static Logger LOGGER = Logger.getLogger(SleeperContextListener.class);

    // Public methods --------------------------------------------------------

    /**
     * @param arg0
     * @see jakarta.servlet.ServletContextListener#contextDestroyed(jakarta.servlet.ServletContextEvent)
     */
    public void contextDestroyed(ServletContextEvent arg0) {
        LOGGER.trace("Context destroyed");
    }

    /**
     * @param arg0
     * @see jakarta.servlet.ServletContextListener#contextInitialized(jakarta.servlet.ServletContextEvent)
     */
    public void contextInitialized(ServletContextEvent arg0) {
        LOGGER.trace("Context initialized - going to sleep");
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            LOGGER.warn("Interrupted!");
        }
        LOGGER.trace("Woke up");
        Log.SB.append(getClass().getSimpleName());
    }

}
