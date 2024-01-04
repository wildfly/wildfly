/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.web.security.servlet.methods;

import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.HttpMethodConstraint;
import jakarta.servlet.annotation.ServletSecurity;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Simple servlet that secures GET, HEAD and TRACE methods.
 *
 * @author Jan Tymel
 */
@WebServlet(name = "SecuredServlet", urlPatterns = {"/secured/"}, loadOnStartup = 1)
@ServletSecurity(
        httpMethodConstraints = {
            @HttpMethodConstraint(value = "GET", rolesAllowed = "role"),
            @HttpMethodConstraint(value = "HEAD", rolesAllowed = "role"),
            @HttpMethodConstraint(value = "TRACE", rolesAllowed = "role")
        }
)
public class SecuredServlet extends HttpServlet {

    // See the comment in DenyUncoveredHttpMethodsTestCase class above testCustomMethod()
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setStatus(HttpServletResponse.SC_OK);
    }
}
