/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.jsf.deployment;

import jakarta.faces.context.FacesContext;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

import com.sun.faces.config.InitFacesContext;
import com.sun.faces.config.WebConfiguration;

/**
 * Workaround for counter-productive "distributable" logic in Mojarra.
 * This setting is used to trigger redundant calls to HttpSession.setAttribute(...) for mutated attributes.
 */
public class NonDistributableServletContextListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent event) {
        ServletContext context = event.getServletContext();
        FacesContext facesContext = new InitFacesContext(context);
        try {
            WebConfiguration.getInstance(context).setOptionEnabled(WebConfiguration.BooleanWebContextInitParameter.EnableDistributable, false);
            context.setAttribute(WebConfiguration.BooleanWebContextInitParameter.EnableDistributable.getQualifiedName(), Boolean.FALSE);
        } finally {
            facesContext.release();
        }
    }
}
