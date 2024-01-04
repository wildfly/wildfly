/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.web.customerrors;

import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * A servlet that sends error responses based on the errorCode parameter.
 *
 * @author Scott.Stark@jboss.org
 */
public class ErrorGeneratorServlet extends HttpServlet {
    private static final long serialVersionUID = 694880881746275204L;

    /**
     * Issues a response.sendError() for the errorCode parameter passed in.
     * If there is no errorCode parameter an IllegalStateException is thrown.
     *
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String errorCode = request.getParameter("errorCode");
        if (errorCode == null)
            throw new IllegalStateException("No errorCode parameter seen");

        int code = Integer.parseInt(errorCode);
        response.sendError(code);
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

}
