/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.elytron.oidc.client.multitenancy;

import static org.wildfly.test.integration.elytron.oidc.client.KeycloakConfiguration.TENANT1_ENDPOINT;

import java.io.IOException;
import java.io.PrintWriter;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * A simple secured HTTP servlet.
 *
 * @author <a href="mailto:fjuma@redhat.com">Farah Juma</a>
 */
@WebServlet(urlPatterns = { TENANT1_ENDPOINT })
public class Tenant1Servlet extends HttpServlet {

    /** The serialVersionUID */
    private static final long serialVersionUID = 1L;

    /** Name of a request parameter (parsed as a boolean), which says if a session should be created. */
    public static final String CREATE_SESSION_PARAM = "createSession";

    /**
     * Writes simple text response.
     *
     * @param req
     * @param resp
     * @throws ServletException
     * @throws IOException
     * @see jakarta.servlet.http.HttpServlet#doGet(jakarta.servlet.http.HttpServletRequest, jakarta.servlet.http.HttpServletResponse)
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/plain");
        final PrintWriter writer = resp.getWriter();
        if (Boolean.parseBoolean(req.getParameter(CREATE_SESSION_PARAM))) {
            req.getSession();
        }
        writer.write(TENANT1_ENDPOINT + ":" + OidcWithMultiTenancyTest.RESPONSE_BODY);
        writer.close();
    }
}
