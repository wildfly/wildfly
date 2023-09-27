/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jca.deployment;


import java.io.IOException;
import java.io.PrintWriter;
import jakarta.ejb.EJB;
import javax.naming.InitialContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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
