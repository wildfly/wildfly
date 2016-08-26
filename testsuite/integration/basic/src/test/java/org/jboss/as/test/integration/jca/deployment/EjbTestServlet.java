/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.jca.deployment;


import java.io.IOException;
import java.io.PrintWriter;
import javax.ejb.EJB;
import javax.naming.InitialContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.logging.Logger;



/*
 * In order for these tests to pass, we must have whitebox-tx.rar configured & deployed.
 * This is a connection resource which is typically done as part of config.vi, but since
 * it is a new anno, we want to do it here.  But like the connection resources, this also
 * will not work unless the corresponding RA for this resource is first deployed.
 * (note: whitebox-tx.rar should be deployed as part of initial config)
 *
 */


public class EjbTestServlet extends HttpServlet {
    @EJB
    private ITestStatelessEjb testStatelessEjb;
    @EJB
    private ITestStatelessEjbAO testStatelessEjbAO;

    private String servletAppContext = null;
    private String testMethod = null;
    private String RARJndiScope = null;

    private static final long serialVersionUID = 1L;
    private static final Logger log = Logger.getLogger(EjbTestServlet.class);

    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        //SecurityClient client = null;
        try {
            InitialContext ctx = new InitialContext();

            boolean testPassed;


            testPassed = testStatelessEjbAO.validateConnectorResource("java:app/rardeployment/AppAdmin");
            if (!testPassed) { throw new ServletException("Failed to access AppAdmin"); }

            testPassed = testStatelessEjbAO.validateConnectorResource("java:comp/rardeployment/CompAdmin");
            if (!testPassed) { throw new ServletException("Failed to access CompAdmin"); }

            testPassed = testStatelessEjbAO.validateConnectorResource("java:module/rardeployment/ModuleAdmin");
            if (!testPassed) { throw new ServletException("Failed to access ModuleAdmin"); }

            testPassed = testStatelessEjbAO.validateConnectorResource("java:global/rardeployment/GlobalAdmin");
            if (!testPassed) { throw new ServletException("Failed to access GlobalAdmin"); }

            testPassed = testStatelessEjb.validateConnectorResource("java:app/rardeployment/AppCF");
            if (!testPassed) { throw new ServletException("Failed to access AppCF"); }

            testPassed = testStatelessEjb.validateConnectorResource("java:comp/rardeployment/CompCF");
            if (!testPassed) { throw new ServletException("Failed to access CompCF"); }

            testPassed = testStatelessEjb.validateConnectorResource("java:module/rardeployment/ModuleCF");
            if (!testPassed) { throw new ServletException("Failed to access ModuleCF"); }

            testPassed = testStatelessEjb.validateConnectorResource("java:global/rardeployment/GlobalCF");
            if (!testPassed) { throw new ServletException("Failed to access GlobalCF"); }

        } catch (ServletException se) {
            log.error(se);
            throw se;
        } catch (Exception e) {
            log.error(e);
            throw new ServletException("Failed to access resource adapter", e);
        } finally {
            //client.logout();
        }
        response.setContentType("text/plain");
        PrintWriter out = response.getWriter();
        out.print("EjbTestServlet OK");
        out.close();
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

}
