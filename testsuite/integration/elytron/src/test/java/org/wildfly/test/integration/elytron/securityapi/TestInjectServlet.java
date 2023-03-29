/*
 * Copyright 2023 Red Hat, Inc.
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
package org.wildfly.test.integration.elytron.securityapi;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.Principal;
import java.util.Objects;

import jakarta.inject.Inject;
import jakarta.security.enterprise.SecurityContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.HttpMethodConstraint;
import jakarta.servlet.annotation.ServletSecurity;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * A simple servlet to test using the custom method of {@link TestCustomPrincipal}, for the Jakarta annotation
 * {@link Inject}.
 *
 * @author <a href="mailto:carodrig@redhat.com">Cameron Rodriguez</a>
 */
@WebServlet(urlPatterns = "/inject")
public class TestInjectServlet extends HttpServlet {

    private static final long serialVersionUID = 1524739695027377372L;

    @Inject
    private SecurityContext securityContext = null;
    static final String LOGIN_HEADER = "Login-Time";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        try (PrintWriter pw = resp.getWriter()) {
            final Principal principal = Objects.requireNonNull(securityContext, "SecurityContext must be injected")
                    .getCallerPrincipal();
             pw.println("<html><head><title>doGet - TestCustomPrincipal</title></head>");
            pw.println("<body><h1>Custom principal test, via SecurityContext</h1>");

            if (principal instanceof TestCustomPrincipal) {
                TestCustomPrincipal customPrincipal = (TestCustomPrincipal) principal;

                pw.printf("<ul>Principal: %s</li>", customPrincipal.getName());
                pw.printf("<li>Class: %s</li>\n", customPrincipal.getClass().getCanonicalName());
                pw.printf("<li>Current login time: %s</li>\n", customPrincipal.getCurrentLoginTime().toString());
                pw.println("</ul></body></html>");

                resp.addHeader(LOGIN_HEADER, customPrincipal.getCurrentLoginTime().format(ISO_LOCAL_DATE_TIME));
            } else {
                pw.printf("<p>The principal returned was not of class TestCustomPrincipal.</p>");
                pw.printf("<ul>Principal: %s</li>", principal.getName());
                pw.printf("<li>Class: %s</li>\n", principal.getClass().getCanonicalName());
                pw.println("</ul></body></html>");

                resp.setStatus(500);
            }

        }
    }
}

/** Secured Servlets using Elytron authentication require an extra annotation */

@ServletSecurity(httpMethodConstraints = { @HttpMethodConstraint(value = "GET", rolesAllowed = { "Login" }) })
class TestInjectElytronServlet extends TestInjectServlet {
    private static final long serialVersionUID = 5806993562466870053L;
}