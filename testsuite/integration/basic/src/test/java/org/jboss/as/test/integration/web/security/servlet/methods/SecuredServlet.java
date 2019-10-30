/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.web.security.servlet.methods;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.HttpMethodConstraint;
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
