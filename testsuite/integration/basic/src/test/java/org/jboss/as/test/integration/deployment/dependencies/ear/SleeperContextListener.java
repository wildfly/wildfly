/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
