/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.elytron.oidc.client.logout;


import java.io.IOException;
import java.io.PrintWriter;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = { SimplePostLogoutServlet.POST_LOGOUT_PATH })
public class SimplePostLogoutServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    public static final String POST_LOGOUT_PATH = "/SimplePostLogoutServlet";
    public static final String RESPONSE_BODY = "Post logout success.";

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

        writer.write(RESPONSE_BODY);
        writer.close();
    }
}
