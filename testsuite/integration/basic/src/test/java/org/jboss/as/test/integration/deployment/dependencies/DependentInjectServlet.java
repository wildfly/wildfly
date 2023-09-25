/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.deployment.dependencies;


import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet(urlPatterns = "/test", loadOnStartup = 1)
@ApplicationScoped
public class DependentInjectServlet extends HttpServlet {

    @EJB(lookup = "java:global/dependee/DependeeEjb")
    StringView depdendent;

    @Inject
    BeanManager beanManager;

    @PostConstruct
    public void doStuff() {
        if (!"hello".equals(depdendent.getString())) {
            throw new RuntimeException("wrong string");
        }
        beanManager.createInstance();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.getWriter().write(depdendent.getString());
    }
}
