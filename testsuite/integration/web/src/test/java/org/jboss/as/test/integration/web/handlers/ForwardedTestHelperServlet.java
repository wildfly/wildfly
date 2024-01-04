/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
