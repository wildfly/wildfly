/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.security.jacc.context;

import java.io.IOException;

import org.jboss.logging.Logger;
import org.wildfly.common.function.ExceptionSupplier;

import jakarta.ejb.EJB;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = { PolicyContextTestServlet.SERVLET_PATH })
public class PolicyContextTestServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(PolicyContextTestServlet.class);

    private static final long serialVersionUID = 1L;

    public static final String SERVLET_PATH = "/policy-context-test";

    @EJB
    private PolicyContextTestBean policyContextTestBean;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // This test is just using a static method on the bean implementation so it running in the servlet container.
        boolean availableForServlet = isAvailable(PolicyContextTestBean::getHttpServletRequest);
        // This test makes a call to the EJB so is running in the EJB container.
        boolean availableForEjb = isAvailable(() -> policyContextTestBean.getHttpServletRequestFromPolicyContext());

        if (!availableForServlet || !availableForEjb) {
            throw new ServletException(String.format(
                    "HttpServletRequest not available in all containers availableForServlet=%b, availableForEjb=%b.",
                    availableForServlet, availableForEjb));
        }

        String responseString = "HttpServletRequest successfully obtained from both containers.";
        LOGGER.debug(responseString);
        response.getWriter().write(responseString);
    }

    private boolean isAvailable(ExceptionSupplier<HttpServletRequest, Exception> testSupplier) {
        try {
            HttpServletRequest request = testSupplier.get();

            if (request == null) {
                LOGGER.debug("Instance from test Supplier<T> was null.");
                return false;
            }

            return true;
        } catch (Exception e) {
            LOGGER.warn("Unable to get instance from test Supplier<T>", e);
            return false;
        }
    }
}
