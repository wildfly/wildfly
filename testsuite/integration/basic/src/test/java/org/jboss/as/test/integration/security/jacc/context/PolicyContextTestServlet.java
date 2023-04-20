/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
