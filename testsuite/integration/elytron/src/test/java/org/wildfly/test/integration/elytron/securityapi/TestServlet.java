/*
 * Copyright 2018 Red Hat, Inc.
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

import java.io.IOException;
import java.io.PrintWriter;
import java.security.Principal;

import jakarta.ejb.EJB;
import jakarta.inject.Inject;
import jakarta.security.enterprise.SecurityContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.wildfly.security.auth.server.SecurityDomain;

/**
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
@WebServlet(urlPatterns="/test")
public class TestServlet extends HttpServlet {

    @Inject
    private SecurityContext securityContext;

    @EJB
    private WhoAmI whoami;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        System.out.println("doGet");
        final PrintWriter writer = resp.getWriter();

        final String sourceParam = req.getParameter("source");
        final boolean ejb = Boolean.valueOf(req.getParameter("ejb"));
        final String source = sourceParam != null ? sourceParam : "";
        // Default Action
        final Principal principal;
        switch (source) {
            case "SecurityContext":
                principal = ejb ? whoami.getCallerPrincipalSecurityContext() : securityContext.getCallerPrincipal();
                break;
            case "SecurityDomain":
                principal = ejb ? whoami.getCallerPrincipalSecurityDomain() : SecurityDomain.getCurrent().getCurrentSecurityIdentity().getPrincipal();
                break;
            default:
                principal = ejb ? whoami.getCallerPrincipalSessionContext() : req.getUserPrincipal();
        }

        writer.print(principal == null ? "null" : principal.getName());
    }

}
