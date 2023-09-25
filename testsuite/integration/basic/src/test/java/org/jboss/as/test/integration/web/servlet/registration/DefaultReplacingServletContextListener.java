/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.web.servlet.registration;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.ServletRegistration;
import jakarta.servlet.annotation.WebListener;

@WebListener
public class DefaultReplacingServletContextListener implements ServletContextListener {
    public void contextInitialized(ServletContextEvent sce) {
        ServletRegistration registration = sce.getServletContext().addServlet("ReplacementServlet", new ReplacementServlet());
        registration.addMapping("/");
    }

    public void contextDestroyed(ServletContextEvent sce) {
    }
}
