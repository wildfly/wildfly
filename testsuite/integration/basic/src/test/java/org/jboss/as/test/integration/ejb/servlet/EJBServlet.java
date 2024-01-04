/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.Callable;

import jakarta.ejb.EJB;
import javax.naming.InitialContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.jboss.as.test.shared.integration.ejb.security.Util;
import org.jboss.logging.Logger;

/**
 * A servlet that accesses an EJB and tests whether the call argument is serialized.
 *
 * @author Scott.Stark@jboss.org
 */
public class EJBServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger log = Logger.getLogger(EJBServlet.class);

    @EJB(lookup = "java:global/ejb3-servlet-ejbs/Session30!org.jboss.as.test.integration.ejb.servlet.Session30BusinessRemote")
    Session30BusinessRemote injectedSession;

    @EJB(lookup = "java:global/ejb3-servlet-ejbs/StatelessBean!org.jboss.as.test.integration.ejb.servlet.StatelessLocal")
    StatelessLocal injectedStateless;

    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        final Callable<Void> callable = () -> {
            InitialContext ctx = new InitialContext();

            injectedSession.hello();
            injectedSession.goodbye();

            injectedStateless.hello();
            injectedStateless.goodbye();

            String lookupString = "java:global/ejb3-servlet-ejbs/Session30!";
            EJBServletHelper test = new EJBServletHelper();
            test.processRequest(lookupString, ctx);
            return null;
        };
        try {
            Util.switchIdentitySCF("user1", "password1", callable);
        } catch (Exception e) {
            log.error(e);
            throw new ServletException("Failed to call EJBs/Session30 through remote and local interfaces", e);
        }
        response.setContentType("text/plain");
        PrintWriter out = response.getWriter();
        out.print("EJBServlet OK");
        out.close();
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }
}
