/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ejb.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.Callable;

import javax.ejb.EJB;
import javax.naming.InitialContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.as.test.shared.integration.ejb.security.Util;
import org.jboss.logging.Logger;

/**
 * A servlet that accesses an EJB and tests whether the call argument is serialized.
 *
 * @author Scott.Stark@jboss.org
 */
public class EJBServletEar extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger log = Logger.getLogger(EJBServletEar.class);

    @EJB(lookup = "java:app/ejb3-ear-servlet-ejbs/Session30!org.jboss.as.test.integration.ejb.servlet.Session30BusinessRemote")
    Session30BusinessRemote injectedSession;

    @EJB(lookup = "java:app/ejb3-ear-servlet-ejbs/StatelessBean!org.jboss.as.test.integration.ejb.servlet.StatelessLocal")
    StatelessLocal injectedStateless;

    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        final Callable<Void> callable = () -> {
            InitialContext ctx = new InitialContext();

            injectedSession.hello();
            injectedSession.goodbye();

            injectedStateless.hello();
            injectedStateless.goodbye();

            String lookupString = "java:app/ejb3-ear-servlet-ejbs/Session30!";
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
