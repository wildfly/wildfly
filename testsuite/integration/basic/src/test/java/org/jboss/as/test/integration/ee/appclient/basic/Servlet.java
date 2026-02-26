/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ee.appclient.basic;

import java.io.IOException;

import jakarta.ejb.EJB;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = "/servlet")
public class Servlet extends HttpServlet {

    @EJB
    private AppClientSingletonRemote injectedBean;

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        // Note: the EE 11 TCK issue that led to WFLY-21514 failed because having AppClientSingletonRemote in both
        // the appclient jar and the ejb jar led to injection failures in the servlet.  I couldn't get a deployment
        // setup that would lead to that failure in this test. Rather than continuing to bang my head I just
        // added a direct check that classes from the appclient jar are not visible to the servlet.
        try {
            Thread.currentThread().getContextClassLoader().loadClass("org.jboss.as.test.integration.ee.appclient.basic.AppClientMain");
            resp.getWriter().write(injectedBean.echo("FAIL"));
        } catch (ClassNotFoundException good) {
            // Confirm the injected EJB works
            resp.getWriter().write(injectedBean.echo("OK"));
        }
        resp.getWriter().close();
    }
}
