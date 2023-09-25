/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.web.security.authentication.deployment;

import jakarta.ejb.EJB;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.HttpConstraint;
import jakarta.servlet.annotation.ServletSecurity;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * A simple secured Servlet which calls a secured EJB. Upon successful authentication and authorization the EJB will return the
 * principal's name. Servlet security is implemented using annotations.
 *
 * @author Sherif Makary
 *
 */
@SuppressWarnings("serial")
@WebServlet(name = "SecuredEJBServlet", urlPatterns = {"/SecuredEJBServlet/"}, loadOnStartup = 1)
@ServletSecurity(@HttpConstraint(rolesAllowed = {"guest"}))
public class SecuredEJBServlet extends HttpServlet {

    private static String PAGE_HEADER = "<html><head><title>ejb-security</title></head><body>";
    private static String PAGE_FOOTER = "</body></html>";

    @EJB
    private SecuredEJB securedEJB;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        PrintWriter writer = resp.getWriter();

        String principal = securedEJB.getSecurityInfo();
        String remoteUser = req.getRemoteUser();
        String authType = req.getAuthType();

        writer.println(PAGE_HEADER);
        writer.println("<h1>" + "Successfully called Secured EJB " + "</h1>");
        writer.println("<p>" + "Principal: " + principal + "</p>");
        writer.println("<p>" + "Remote User: " + remoteUser + "</p>");
        writer.println("<p>" + "Authentication Type: " + authType + "</p>");
        writer.println(PAGE_FOOTER);
        writer.close();
    }

}
