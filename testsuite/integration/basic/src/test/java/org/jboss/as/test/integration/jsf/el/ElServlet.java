/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jsf.el;

import java.io.IOException;

import jakarta.el.ExpressionFactory;
import jakarta.faces.FactoryFinder;
import jakarta.faces.application.Application;
import jakarta.faces.context.FacesContext;
import jakarta.faces.context.FacesContextFactory;
import jakarta.faces.lifecycle.LifecycleFactory;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.jsp.JspFactory;

@WebServlet(urlPatterns = {"/ElServlet"})
public class ElServlet extends HttpServlet {

    private ServletContext servletContext;
    private volatile Application application;

    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        servletContext = config.getServletContext();
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse response) throws ServletException, IOException {
        initApplication(req, response);
        ExpressionFactory jspFactory = JspFactory.getDefaultFactory()
                .getJspApplicationContext(servletContext).getExpressionFactory();

        ExpressionFactory applicationFactory = application.getExpressionFactory();

        if (!jspFactory.equals(applicationFactory)) {
            throw new IllegalStateException(String.format("Application factory %s@%d/%s does not match JSP factory %s%d/%s",
                    applicationFactory.getClass(), System.identityHashCode(applicationFactory), applicationFactory,
                    jspFactory.getClass(), System.identityHashCode(jspFactory), jspFactory));
        }

    }

    private void initApplication(ServletRequest request,
                           ServletResponse response) {

        if (application == null) {
            FacesContextFactory facesContextFactory = (FacesContextFactory) FactoryFinder
                    .getFactory(FactoryFinder.FACES_CONTEXT_FACTORY);

            LifecycleFactory lifecycleFactory = (LifecycleFactory) FactoryFinder
                    .getFactory(FactoryFinder.LIFECYCLE_FACTORY);
            FacesContext facesContext = facesContextFactory.getFacesContext(servletContext, request,
                    response,
                    lifecycleFactory.getLifecycle(LifecycleFactory.DEFAULT_LIFECYCLE));
            application = facesContext.getApplication();
        }
    }
}