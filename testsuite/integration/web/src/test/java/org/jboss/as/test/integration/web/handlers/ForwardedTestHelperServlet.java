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
package org.jboss.as.test.integration.web.handlers;

import java.io.IOException;
import java.io.PrintWriter;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jboss.as.test.shared.TestSuiteEnvironment;


/**
 * @author Jan Stourac
 */
@WebServlet(name = "ForwardedHelperServlet", urlPatterns = {"/forwarded"})
public class ForwardedTestHelperServlet extends HttpServlet {

    private String message;

    @Override
    public void init(ServletConfig config) throws ServletException {
        message = config.getInitParameter("message");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        PrintWriter out = resp.getWriter();
        String localAddr = req.getLocalAddr();
        if(localAddr.startsWith("/")) {
            localAddr = "/" + TestSuiteEnvironment.formatPossibleIpv6Address(localAddr.substring(1));
        } else {
            localAddr = TestSuiteEnvironment.formatPossibleIpv6Address(localAddr);
        }

        out.print(req.getRemoteAddr() + "|" + req.getRemoteHost() + ":" + req.getRemotePort() + "|" + req.getScheme()
                + "|" + req.getLocalName() + "|" + localAddr + ":" + req.getLocalPort());
        out.close();
    }
}
