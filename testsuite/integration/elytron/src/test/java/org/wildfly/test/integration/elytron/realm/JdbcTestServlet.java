/*
 * Copyright 2019 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.test.integration.elytron.realm;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Map.Entry;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.wildfly.security.auth.server.SecurityDomain;
import org.wildfly.security.auth.server.SecurityIdentity;
import org.wildfly.security.authz.Attributes;

/**
 * A simple servlet that just writes back a string.
 *
 */
@WebServlet(urlPatterns = { JdbcTestServlet.SERVLET_PATH })
public class JdbcTestServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    public static final String SERVLET_PATH = "/test";

    /** The String returned in the HTTP response body. */
    public static final String RESPONSE_BODY = "GOOD";



    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/plain");
        final PrintWriter writer = resp.getWriter();

        Map<String, String[]> parameters = req.getParameterMap();

        SecurityDomain securityDomain = SecurityDomain.getCurrent();
        SecurityIdentity securityIdentity = securityDomain.getCurrentSecurityIdentity();
        Attributes attributes = securityIdentity.getAttributes();

        for (Entry<String, String[]> entry : parameters.entrySet()) {
            for (String value : entry.getValue()) {
                if (attributes.containsValue(entry.getKey(), value) == false) {
                    writer.write(String.format("Attribute %s with value %s missing from the Attributes associated with the current SecurityIdentity.", entry.getKey(), value));
                    writer.close();
                    return;
                }
            }
        }

        writer.write(RESPONSE_BODY);
        writer.close();
    }
}
