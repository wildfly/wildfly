/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.domain.suites;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.jboss.dmr.ModelNode;


@WebServlet(urlPatterns= {"/env"})
public class EnvironmentTestServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        ModelNode node = new ModelNode();
        Map<String, String> env = System.getenv();
        for (Map.Entry<String, String> entry : env.entrySet()) {
            node.get(entry.getKey(), entry.getValue());
        }
        resp.setContentType("application/json");
        final PrintWriter out = resp.getWriter();
        try {
            node.writeJSONString(out, true);
        } finally {
            out.close();
        }
    }
}
