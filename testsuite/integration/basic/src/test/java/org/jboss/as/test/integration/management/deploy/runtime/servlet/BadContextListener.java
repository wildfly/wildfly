/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.management.deploy.runtime.servlet;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

/**
 * A ServletContextListener that fails contextInitialized. Intended
 * use is as a way to fail a deployment.
 */
@WebListener
public class BadContextListener implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        throw new UnsupportedOperationException();
    }
}
